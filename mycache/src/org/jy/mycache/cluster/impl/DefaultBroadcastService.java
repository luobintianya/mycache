package org.jy.mycache.cluster.impl;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.jy.mycache.cluster.BroadcastMessageListener;
import org.jy.mycache.cluster.BroadcastMethod;
import org.jy.mycache.cluster.BroadcastService;
import org.jy.mycache.cluster.BroadcastServiceConfiguration;
import org.jy.mycache.cluster.DefaultBroadcastServiceConfiguration;
import org.jy.mycache.cluster.RawMessage;
import org.jy.mycache.util.Registry;

 


















 public class DefaultBroadcastService
   implements BroadcastService
 {
   private static final Logger LOG = Logger.getLogger(DefaultBroadcastService.class);

   private final String SINGLETON_CREATOR_ID = DefaultBroadcastService.class.getName().intern();

    private long dynamicID;
	private int clusterNodeID;
	private long clusterIslandID;
	private boolean clusteringEnabled; 
	private boolean  sendAsynchonouslyFlag;
	private long sendAsyncTimeout ;
	private int sendAsyncWaitTime; 
	private int senderMinThreads;
	private int senderMaxThreads;
	private int senderKeepAlive;
	private int sendingQueueSize;
	private boolean sendingQueueFair;
	private BroadcastServiceConfiguration cfg;
	private ExecutorService messageSendingExecutors;
	private final List<BroadcastMessageListener> listeners = new CopyOnWriteArrayList();
	private final List<BroadcastMessageListener> localMessageListeners = new CopyOnWriteArrayList();
	private final AtomicInteger messageCounter = new AtomicInteger();
	private final Map messageKeyMap; 
	private List<MethodWrapper>   methods=null;
	private boolean methodsInitialized;
     public String getID()
     {
       return this.SINGLETON_CREATOR_ID;


     }
 
     public DefaultBroadcastService create(long clusterIslandID,boolean clusterMode )
       throws Exception
     {
       BroadcastServiceConfiguration cfg = new DefaultBroadcastServiceConfiguration( clusterIslandID,  clusterMode);
       DefaultBroadcastService ret = new DefaultBroadcastService.ProtectedDefaultBroadcastService(cfg);
  
       return ret;
     }
 
     public void destroy(DefaultBroadcastService defaultBroadcastService)
       throws Exception
     {
       ((DefaultBroadcastService.ProtectedDefaultBroadcastService)defaultBroadcastService).doDestroy();


     }



   private static class ProtectedDefaultBroadcastService extends DefaultBroadcastService
   {
     public ProtectedDefaultBroadcastService(BroadcastServiceConfiguration cfg)
     {
       super(cfg);
     }


     public void destroy()
     {
       throw new IllegalStateException("DefaultBroadcastService cannot be destroyed manually!");
     }

     void doDestroy()
     {
       super.destroy();
     }
   }


   public static DefaultBroadcastService getInstance()
   {
      return null;
   }













   public DefaultBroadcastService(BroadcastServiceConfiguration cfg)
   {
     this.cfg = cfg; 
     this.clusterNodeID = cfg.getConfiguredNodeID();
     this.clusterIslandID = cfg.getClusterIslandID();
     this.clusteringEnabled = cfg.enableClusterMode();


 
     this.sendAsynchonouslyFlag = ((cfg.enableClusterMode()) && (cfg.enableAsynchonousSending()));
     if (this.sendAsynchonouslyFlag)
     {
       this.sendAsyncTimeout = cfg.getMessageSendingMaxWait();
       this.sendAsyncWaitTime = cfg.getMessageSendingWaitDelay();
       this.messageSendingExecutors = createMessageSendingExecutorService();
     }
     else
     {
       this.sendAsyncTimeout = -1L;
       this.sendAsyncWaitTime = -1;
       this.messageSendingExecutors = null;
     }
     this.messageKeyMap = new LinkedHashMap(cfg.getMessageKeyCacheSize());

 
     getMethods();
 
     if (!(LOG.isInfoEnabled()))
       return;
     LOG.info(getStartupInfo());
   }


   protected String getStartupInfo()
   {
     StringBuilder stringBuilder = new StringBuilder("Started message broadcast service:\n");
     stringBuilder.append("clustering (enabled:").append(this.clusteringEnabled);
     if (this.clusteringEnabled)
     {
       stringBuilder.append(",islandID:").append(
         (this.clusterIslandID == -1L) ? "<loaded from tenant>" : Long.toString(this.clusterIslandID));
       stringBuilder.append(",nodeID:").append(this.clusterNodeID);
       //stringBuilder.append(",dynamicNodeID:").append(this.dynamicNodeID);
     }
     stringBuilder.append(")\n");
 
     if (this.sendAsynchonouslyFlag)
     {
       stringBuilder.append("sending asynchronously (queueSize:").append(this.cfg.getMessageSendingQueueSize());
       stringBuilder.append(",fair:").append(this.cfg.getMessageSendingQueueFairness());
       stringBuilder.append(",workers:").append(this.cfg.getSenderMinThreads()).append('/').append(this.cfg.getSenderMaxThreads());
       stringBuilder.append(",keep-alive:").append(this.cfg.getSenderThreadsKeepAlive()).append("ms");
       stringBuilder.append(",sendWaitDelay:").append(this.sendAsyncWaitTime).append("ms");
       stringBuilder.append(",sendTimeout:").append(this.sendAsyncTimeout).append("ms");
       stringBuilder.append(")\n");
     }
     else
     {
       stringBuilder.append("sending synchronously - no queue\n");
     }
 
     stringBuilder.append("methods ").append(getMethods());
 
     return stringBuilder.toString();
   }




   protected void updateNodeIDsFromDatabase()
   { 
 
     if (LOG.isInfoEnabled())
     {
       if (this.clusterIslandID != Registry.getClusterIslandPK())
       {
         LOG.info("updating cluster island ID " + this.clusterIslandID + "->" + Registry.getClusterIslandPK());
       }
       if (this.clusterNodeID != Registry.getClusterID())
       {
         LOG.info("updating cluster node ID " + this.clusterNodeID + "->" + Registry.getClusterID());
       }
     }
     this.clusterIslandID = Registry.getClusterIslandPK();
     this.clusterNodeID = Registry.getClusterID();
   }

   protected ExecutorService createMessageSendingExecutorService()
   {
     return new ThreadPoolExecutor(
       this.cfg.getSenderMinThreads(), this.cfg.getSenderMaxThreads(), 
       this.cfg.getSenderThreadsKeepAlive(), TimeUnit.MILLISECONDS, 
       new ArrayBlockingQueue(
       this.cfg.getMessageSendingQueueSize(), 
       this.cfg.getMessageSendingQueueFairness()), 
       new MyThreadFactory("BroadcastSender"));
   }





   private static class MyThreadFactory
     implements ThreadFactory
   {
     final ThreadGroup group;
     final AtomicInteger threadNumber = new AtomicInteger(1);
     final String namePrefix;
    
 
     MyThreadFactory(String name)
     {
       SecurityManager securityManager = System.getSecurityManager();
       this.group = ((securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup());
       this.namePrefix = name + "-";
     }


     public Thread newThread(Runnable runnable)
     {
       Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
       if (thread.isDaemon())
       {
         thread.setDaemon(false);
       }
       if (thread.getPriority() != 5)
       {
         thread.setPriority(5);
       }
       return thread;
     }
   }

   protected ExecutorService getMessageSendingExecutorService()
   {
     return this.messageSendingExecutors;
   }


 

   public Set<String> getBroadcastMethodNames()
   {
     List<MethodWrapper> methods = getMethods();
     if (methods.isEmpty())
     {
       return Collections.emptySet(); 
     }
 
     Set<String> ret = new LinkedHashSet<String>(methods.size());
     for (MethodWrapper m : methods)
     {
       ret.add(m.getName());
     }
     return ret;
   }



   public BroadcastMethod getBroadcastMethod(String name)
   {
     for (MethodWrapper m : getMethods())
     {
       if (name.equalsIgnoreCase(m.getName()))
       {
         return m.getMethod();
       }
     }
     return null;
   }

   protected List<MethodWrapper> getMethods()
   {
     List ret = this.methods;
     if (ret == null)
     {
       synchronized (this)
       {
         ret = this.methods;
         if (ret == null)
         {
           ret = Collections.unmodifiableList(loadMethods());
           this.methods = ret;
         }
       }
     }
     return ret;
   }

   public void initMethods()
   {
     if (this.methodsInitialized)
       return;
     synchronized (this)
     {
       if (!(this.methodsInitialized))
       {
         this.methodsInitialized = true;
 
         if (LOG.isDebugEnabled())
         {
           LOG.debug("lazy initialization of broadcast methods");
         }
 
         List failed = new ArrayList();
         for (MethodWrapper m : getMethods())
         {
           try
           {
             m.initialize(this);
           }
           catch (Exception e)
           {
             LOG.error("error initializing broadcast method " + m + " (error:" + e.getMessage() + ") - removing method", 
               e);
             failed.add(m);
           }
         }
 
         if (!(failed.isEmpty()))
         {
           List<MethodWrapper> newOne = new ArrayList<MethodWrapper>(this.methods);
           newOne.removeAll(failed);
 
           if (newOne.isEmpty())
           {
             newOne.addAll(loadLoopbackMethods());
             for (Iterator<MethodWrapper> e = newOne.iterator(); e.hasNext(); ) { 
            	 MethodWrapper	 m = (MethodWrapper)e.next();

               try
               {
                 ((MethodWrapper)m).initialize(this);
               }
               catch (Exception ex)
               {
                 LOG.error("error initializing loopback broadcast method " + m + " (error:" + ex.getMessage() + 
                   ") - removing method", ex);
                 newOne.remove(m);
               }
             }
           }
           this.methods = Collections.unmodifiableList(newOne);
         }
 
         for (Object m = this.methods.iterator(); ((Iterator)m).hasNext(); ) { MethodWrapper wr = (MethodWrapper)((Iterator)m).next();
           wr.getMethod().registerProcessor(new MethodBroadcastListener(wr.getName()));
         }
       }
     }
   }


   protected void startPingHandler()
   {
     if (!(this.cfg.startPingOnTenantStartup()))
       return;
      PingBroadcastHandler.getInstance();
   }


   protected List<MethodWrapper> loadMethods()
   {
     List methods = null;
 
     if (this.clusteringEnabled)

     {
       methods = loadConfiguredMethods();
       if (methods.isEmpty())
       {
         methods = loadLoopbackMethods();
       }
 
     }
     else
     {
       methods = loadLoopbackMethods();
     }
     return methods;
   }

   protected List<MethodWrapper> loadLoopbackMethods()
   {
     MethodWrapper methodWrapper = new MethodWrapper("<loopback>", new LoopBackBroadcastMethod());
     return Collections.singletonList(methodWrapper);
   }

   protected List<MethodWrapper> loadConfiguredMethods()
   {
     List tmp = new ArrayList();
     for (Map.Entry cfgMethod : this.cfg.getMethods().entrySet())
     {
       MethodWrapper methodWrapper = createMethod((String)cfgMethod.getKey(), (Class)cfgMethod.getValue());
       if (methodWrapper == null)
         continue;
       tmp.add(methodWrapper);
     }
 
     return tmp;
   }

   protected MethodWrapper createMethod(String name, Class clazz)
   {
     try
     {
       return new MethodWrapper(name, (BroadcastMethod)clazz.newInstance());
     }
     catch (Exception e)
     {
       LOG.error("Invalid messaging mode definition " + name + " -> " + clazz + " ( error: " + e.getMessage() + ")"); }
     return null;
   }



   public void registerBroadcastListener(BroadcastMessageListener listener, boolean remoteMessagesOnly)
   {
     if (listener == null)
     {
       throw new NullPointerException("listener was null");
     }
 
     if ((!(remoteMessagesOnly)) && 

       (!(this.localMessageListeners.contains(listener))))
     {
       this.localMessageListeners.add(listener);

     }
 
     if (this.listeners.contains(listener))
       return;
     this.listeners.add(listener);
   }



   public void unregisterBroadcastListener(BroadcastMessageListener listener)
   {
     if (listener == null)
     {
       throw new NullPointerException("listener was null");
     }
     this.listeners.remove(listener);
     this.localMessageListeners.remove(listener);
   }

   public long getClusterIslandPK()
   {
     return this.clusterIslandID;
   }

   public int getClusterNodeID()
   {
     return this.clusterNodeID;
   }

   public boolean isClusteringEnabled()
   {
     return this.clusteringEnabled;
   }

   public void addTransportData(RawMessage message)
   {
     message.setSenderTransportData(67174656, getClusterIslandPK(), getDynamicClusterNodeID(), 
       this.messageCounter.getAndIncrement());
   }


   public void send(RawMessage message)
   {
     addTransportData(message);
 
     Tenant currentTenant = Registry.getCurrentTenantNoFallback();
     label150: for (MethodWrapper m : getMethods())
     {
       if ((!(m.isInitialized())) || 

         (this.sendAsynchonouslyFlag));

       try
       {
         sendAsnychronously(m, message, currentTenant, getMessageSendingExecutorService());
       }
       catch (RejectedExecutionException localRejectedExecutionException)
       {
         LOG.error("could not place message " + message + " into queue after " + this.sendAsyncTimeout + 
           " ms - this message is not being broadcasted!");
         return;



 
         sendSynchronously(m, message);
 
         break label150:

 
         LOG.warn("broadcast method " + m + " is not initialized yet - cannot send message " + message);
       }
     }
   }









   protected void sendSynchronously(MethodWrapper methodWrapper, RawMessage message)
   {
     try
     {
       methodWrapper.getMethod().send(message);
     }
     catch (Exception e)
     {
       LOG.error("error trying to send message " + message + " using " + methodWrapper + " (error: " + e.getMessage() + ")", e);
     }
   }















   protected void sendAsnychronously(MethodWrapper methodWrapper, RawMessage message, Tenant currentTenant, ExecutorService executorService)
     throws RejectedExecutionException
   {
     MethodMessageSender sender = new MethodMessageSender(methodWrapper, message, currentTenant);
     boolean success = false;
     long retryStartTime = -1L;
     do
     {
       try
       {
         executorService.execute(sender);
         success = true;

       }
       catch (RejectedExecutionException localInterruptedException)
       {
         if (retryStartTime == -1L)
         {
           retryStartTime = System.currentTimeMillis();

         }
         else if (System.currentTimeMillis() - retryStartTime >= this.sendAsyncTimeout)
         {
           throw e;
         }
 
         try
         {
           Thread.sleep(this.sendAsyncWaitTime);
         }
         catch (InterruptedException localInterruptedException)
         {
         }
       }
     }
 
     while (!(success));
   }
   private static class MethodMessageSender
     implements Runnable
   {
     final DefaultBroadcastService.MethodWrapper methodWrapper;
     final RawMessage message;
     final Tenant tenant;
 
     MethodMessageSender(DefaultBroadcastService.MethodWrapper methodWrapper, RawMessage message, Tenant tenant)
     {
       this.methodWrapper = methodWrapper;
       this.message = message;
       this.tenant = tenant;
     }


     public void run()
     {
       if (!(isAllowedToRunForTenant(this.tenant)))
         return;
       Tenant prev = Registry.getCurrentTenantNoFallback();

       try
       {
         Registry.setCurrentTenant(this.tenant);
         this.methodWrapper.getMethod().send(this.message);
       }
       catch (Exception e)
       {
         DefaultBroadcastService.LOG.error("error trying to send message " + this.message + " using " + this.methodWrapper + " (error: " + e.getMessage() + 
           ")", e);
       }
       finally
       {
         if (prev == null)
         {
           Registry.unsetCurrentTenant();
         }
         else
         {
           Registry.setCurrentTenant(prev);
         }
       }
     }


     private boolean isAllowedToRunForTenant(Tenant tenant)
     {
       return ((!(RedeployUtilities.isShutdownInProgress())) && (((tenant == null) || (
         (!(((AbstractTenant)tenant).isStarting())) && (!(((AbstractTenant)tenant).isStopping()))))));
     }
   }












   public boolean accept(RawMessage message)
   {
     if (message.getVersion() == -1)
     {
       LOG.error("Received RawMessage with undefined message version! " + message.toString());

     }
 
     if (getClusterIslandPK() == -1L)

     {
       if ((message.getClusterIslandPK() != -1L) || 
         (getDynamicClusterNodeID() != message.getDynamicNodeID()))
       {
         return false;
       }
 
     }
     else if (getClusterIslandPK() != message.getClusterIslandPK())
     {
       if (message.getClusterIslandPK() == -1L)
       {
         LOG.error("Received RawMessage with undefined message version! " + message.toString());
       }
       return false;



     }
 
     return (message.getVersion() == 67174656);
   }








   protected boolean isDuplicateMessage(RawMessage message)
   {
     if (this.messageKeyMap.put(message.getMessageKey(), Boolean.TRUE) == null)
     {
       return false;

     }
 
     if (LOG.isDebugEnabled())
     {
       LOG.debug("skipped duplicate message " + message);
     }
     return true;
   }


   protected boolean isLocal(RawMessage message)
   {
     return (getDynamicClusterNodeID() == message.getDynamicNodeID());
   }

   protected void processMessageFromMethod(RawMessage message, String methodName)
   {
     if ((!(accept(message))) || (isDuplicateMessage(message)))
       return;
     message.setBroadcastMethod(methodName);
     if (isLocal(message))
     {
       for (BroadcastMessageListener listener : this.localMessageListeners)
       {
         if (listener.processMessage(message)) {
           return;
         }
 
       }
 
     }
     else
       for (BroadcastMessageListener listener : this.listeners)
       {
         if (listener.processMessage(message))
           return;
       }
   }





   public void destroy()
   {
     this.listeners.clear();
     this.localMessageListeners.clear();
     if (getMessageSendingExecutorService() != null)
     {
       getMessageSendingExecutorService().shutdownNow();
     }
     if (this.methods == null)
       return;
     synchronized (this)
     {
       if (this.methods != null)
       {
         for (MethodWrapper wrapper : this.methods)
         {
           wrapper.shutdown();
         }
         this.methods = null;
       }
     }
   }




   protected class MethodBroadcastListener
     implements BroadcastMessageListener
   {
     private final String methodName;
 
     MethodBroadcastListener(String paramString)
     {
       this.methodName = paramString;
     }


     public boolean processMessage(RawMessage message)
     {
       DefaultBroadcastService.this.processMessageFromMethod(message, this.methodName);
       return true;
     }
   }

   protected static class MethodWrapper
   {
     private final BroadcastMethod _method;
     private final String _name;
     private volatile boolean _initialized = false;

     MethodWrapper(String name, BroadcastMethod method)
     {
       this._name = name;
       this._method = method;
     }

     public void shutdown()
     {
       if (!(this._initialized))
         return;
       synchronized (this)
       {
         if (this._initialized)
         {
           this._initialized = false;
           this._method.shutdown();
         }
       }
     }


     BroadcastMethod getMethod()
     {
       if (!(this._initialized))
       {
         throw new IllegalStateException("method " + getName() + "::" + this._method + " is not initialized");
       }
       return this._method;
     }

     void initialize(BroadcastService service)
     {
       if (this._initialized)
         return;
       synchronized (this)
       {
         if (!(this._initialized))
         {
           this._method.init(service);
           this._initialized = true;
         }
       }
     }


     boolean isInitialized()
     {
       return this._initialized;
     }

     void setInitialized(boolean initialized)
     {
       this._initialized = initialized;
     }

     String getName()
     {
       return this._name;
     }


     public String toString()
     {
       return getName() + "::" + this._method;
     }
   }


@Override
public long getDynamicClusterNodeID() {
	// TODO Auto-generated method stub
	return 0;
}
 }
 
