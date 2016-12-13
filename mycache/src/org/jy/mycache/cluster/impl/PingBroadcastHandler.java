package org.jy.mycache.cluster.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger; 

public class PingBroadcastHandler   implements BroadcastMessageListener
 {
   private static final Logger LOG = Logger.getLogger(PingBroadcastHandler.class); 

   private volatile long lastPingRequest = 0L;

   private final Pattern pingRequestRegExpPattern = Pattern.compile("<PING>");
   private final Pattern pingAnswerRegExpPattern = Pattern.compile("<PING_RESULT>(\\d+)\\|(\\d+)");
   
   private static final SingletonCreator.Creator<PingBroadcastHandler> SINGLETON_CREATOR = new singletoncreator.Creator()   };
   {
     private final String SINGLETON_CREATOR_ID = PingBroadcastHandler.class.getName().intern();

 
     public String getID()
     {
       return this.SINGLETON_CREATOR_ID;
     }
 
     public PingBroadcastHandler create()
       throws Exception
     {
       return new PingBroadcastHandler(DefaultBroadcastService.getInstance());
     }
 
     public void destroy(PingBroadcastHandler pingBroadcastHandler)
       throws Exception
     {
       pingBroadcastHandler.destroy();



   public static PingBroadcastHandler getInstance()
   {
     return ((PingBroadcastHandler)Registry.getNonTenantSingleton(SINGLETON_CREATOR));
   }

   public PingBroadcastHandler(BroadcastService broadcastService)
   {
     this.broadcastService = broadcastService;
     this.nodes = new ConcurrentHashMap();
     this.broadcastService.registerBroadcastListener(this, false);
     startPingTask(Registry.getMasterTenant().getConfig().getInt("cluster.ping.interval", 30));
   }

   public void destroy()
   {
     synchronized (this)
     {
       this.broadcastService.unregisterBroadcastListener(this);
       if (this.pingTimer != null)
       {
         this.pingTimer.cancel();
         this.pingTimer = null;
       }
       if (this.timerTask != null)
       {
         this.timerTask.cancel();
         this.timerTask = null;
       }
       this.nodes.clear();
     }
   }


   public boolean processMessage(RawMessage message)
   {
     return ((message.getKind() == 99) && (processPingMessage(message)));
   }

   private void startPingTask(int intervalSec)
   {
     if (this.pingTimer != null)
     {
       throw new IllegalStateException("ping timer task already started");
     }
 
     if (intervalSec <= 0)
       return;
     this.pingTimer = new Timer("Ping Timer", true);
 
     TimerTask timerTask = new TimerTask()

     {
       public void run()
       {
         try
         {
           PingBroadcastHandler.this.pingNodes();
         }
         catch (Exception e)
         {
           PingBroadcastHandler.LOG.error("unknown error pinging cluster nodes: " + e.getMessage());
         }
       }
     };
     try
     {
       this.pingTimer.schedule(timerTask, 0L, intervalSec * 1000);
     }
     catch (Exception e)
     {
       LOG.error("unknown error starting ping timer task - cannot discover other cluster nodes! (see stacktrace)", e);
     }
   }


   public boolean isNodeAlive(int nodeID)
   {
     return this.nodes.containsKey(Integer.valueOf(nodeID));
   }


   public Collection<NodeInfo> getNodes()
   {
     long diff;
     if ((diff = System.currentTimeMillis() - this.lastPingRequest) < 5000L)

     {
       try
       {
         Thread.sleep(5000L - diff);

       }
       catch (InterruptedException localInterruptedException)
       {
         Thread.currentThread().interrupt();
       }
     }
     return ((this.nodes.isEmpty()) ? Collections.EMPTY_LIST : Collections.unmodifiableCollection(this.nodes.values()));
   }


   protected void addNode(long dynamicNodeID, int remoteNodeID, String ipAddress, String methodName)
   {
     synchronized (this.nodes)
     {
       NodeInfo newOne = new NodeInfo(dynamicNodeID, remoteNodeID, ipAddress, methodName);
       this.nodes.put(Long.valueOf(dynamicNodeID), newOne);
       for (Map.Entry e : this.nodes.entrySet())
       {
         NodeInfo other = (NodeInfo)e.getValue();
         if ((other.getNodeID() != remoteNodeID) || (other.getDynamicNodeID() == dynamicNodeID)) {
           continue;
         }
         other.markAsDuplicate();
 
         newOne.markAsDuplicate();
       }
     }
   }


   public void pingNodes()
   {
     if (RedeployUtilities.isShutdownInProgress()) {
       return;
     }
     try
     {
       this.nodes.clear();
       this.lastPingRequest = System.currentTimeMillis();
       sendPing();
     }
     catch (Exception e)
     {
       LOG.error("error sending cluster ping message (error:" + e.getMessage() + ")", e);
     }
   }


   protected void sendPing()
   {
     this.broadcastService.send(
       new RawMessage(
       99, 

       "<PING>".getBytes()));
   }


   protected void sendPingAnswer(long remoteDynamicNode)
   {
     StringBuilder stringBuilder = new StringBuilder();
     stringBuilder.append("<PING_RESULT>");
     stringBuilder.append(remoteDynamicNode).append("|");
     stringBuilder.append(getClusterNodeID());
 
     this.broadcastService.send(
       new RawMessage(
       99, 
       stringBuilder.toString().getBytes()));
   }



   protected boolean processPingMessage(RawMessage message)
   {
     String string = new String(message.getData());
     if (LOG.isDebugEnabled())
     {
       LOG.debug("process ping message:" + string);
     }
 
     try
     {
       Matcher pingMatcher = this.pingRequestRegExpPattern.matcher(string);
       if (pingMatcher.matches())
       {
         sendPingAnswer(message.getDynamicNodeID());
         return true;
       }
 
       Matcher pingAnswerMatcher = this.pingAnswerRegExpPattern.matcher(string);
       if (!(pingAnswerMatcher.matches()))
         break label201;
       long srcDynamicNodeID = Long.parseLong(pingAnswerMatcher.group(1));
 
       if (this.broadcastService.getDynamicClusterNodeID() == srcDynamicNodeID)
       {
         addNode(
           message.getDynamicNodeID(), 
           Integer.parseInt(pingAnswerMatcher.group(2)), 
           (message.getRemoteAddress() != null) ? message.getRemoteAddress().getHostAddress() : "n/a", 
           message.getBroadcastMethod());
       }
 
       return true;

     }
     catch (Exception e)
     {
       LOG.error("unexpected error parsing PING message " + message + " (error:" + e.getMessage() + ")", e);
     }
     label201: return false;
   }

   protected int getClusterNodeID()
   {
     return Registry.getMasterTenant().getClusterID();
   }

   protected long getClusterIslandPK()
   {
     return Registry.getMasterTenant().getClusterIslandPK();
   }

   public static final class NodeInfo
   {
     private final long dynamicNodeID;
     private final int nodeID;
     private final String ipAddress;
     private final String methodName;
     boolean duplicate = false;


     NodeInfo(long dynamicNodeID, int nodeID, String ipAddress, String methodName)
     {
       this.dynamicNodeID = dynamicNodeID;
       this.nodeID = nodeID;
       this.ipAddress = ipAddress;
       this.methodName = methodName;
     }

     public long getDynamicNodeID()
     {
       return this.dynamicNodeID;
     }

     public int getNodeID()
     {
       return this.nodeID;
     }

     public String getIP()
     {
       return this.ipAddress;
     }

     protected void markAsDuplicate()
     {
       this.duplicate = true;
     }

     public boolean isDuplicate()
     {
       return this.duplicate;
     }

     public String getMethodName()
     {
       return this.methodName;
     }
   }
 }