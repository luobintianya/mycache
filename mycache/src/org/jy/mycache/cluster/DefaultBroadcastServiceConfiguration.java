package org.jy.mycache.cluster;

import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jy.mycache.util.ConfigIntf;
import org.jy.mycache.util.NumberPK;

public class DefaultBroadcastServiceConfiguration implements
		BroadcastServiceConfiguration {
	private static final Logger LOG = Logger
			.getLogger(DefaultBroadcastServiceConfiguration.class);

	public static String CFG_METHODS = "cluster.broadcast.methods";
	public static String CFG_METHOD_PREFIX = "cluster.broadcast.method.";
	public static String CFG_SENDER_THREADS_MIN = "cluster.broadcast.senderthreads.min";
	public static String CFG_SENDER_THREADS_MAX = "cluster.broadcast.senderthreads.max";
	public static String CFG_SENDER_THREADS_KEEPALIVE = "cluster.broadcast.senderthreads.keepalive";
	public static String CFG_SENDER_THREADS_QUEUESIZE = "cluster.broadcast.senderthreads.queuesize";
	public static String CFG_SENDER_THREADS_QUEUE_FAIRNESS = "cluster.broadcast.senderthreads.queue.fair";
	public static String CFG_SENDER_THREADS_MAXWAIT = "cluster.broadcast.senderthreads.maxwait";
	public static String CFG_SENDER_THREADS_WAITDELAY = "cluster.broadcast.senderthreads.waitdelay";
	public static String CFG_SENDER_THREADS_DISABLE = "cluster.broadcast.senderthreads.disable";

	public static String CFG_START_PING_ON_TENANT_STARTUP = "cluster.ping.load.on.startup";

	public static String CFG_MESSAGE_KEYCACHE_SIZE = "cluster.broadcast.keycache.size";

	public static String DEFAULT_METHODS = "udp";

	private long dynamicID;
	private int configuredID;
	private long clusterIslandID;
	private boolean clusterMode;

	private boolean asynchronousSending;
	private long sendingMaxWait;
	private int sendingWaitDelay;

	private int senderMinThreads;
	private int senderMaxThreads;
	private int senderKeepAlive;
	private int sendingQueueSize;
	private boolean sendingQueueFair;

	private int messageKeyCacheSize;

	private boolean startPingOnTenant;

	private Map<String, Class<? extends BroadcastMethod>>  methods;

	public DefaultBroadcastServiceConfiguration(long clusterIslandID,boolean clusterMode) {
		 
		this.dynamicID = NumberPK.createUUIDPK(0); 
		this.clusterIslandID = clusterIslandID; //cluster pk
		this.clusterMode = clusterMode; //is cluster model? true or false

		ConfigIntf cfg = org.jy.mycache.util.Registry.getConfig();
		this.asynchronousSending = (!(cfg.getBoolean(
				CFG_SENDER_THREADS_DISABLE, false)));
		this.sendingMaxWait = (1000L * cfg.getLong(CFG_SENDER_THREADS_MAXWAIT,
				60L));
		this.sendingWaitDelay = cfg.getInt(CFG_SENDER_THREADS_WAITDELAY, 100);

		this.senderMinThreads = cfg.getInt(CFG_SENDER_THREADS_MIN, 1);
		this.senderMaxThreads = cfg.getInt(CFG_SENDER_THREADS_MAX, 10);
		this.senderKeepAlive = (1000 * cfg.getInt(CFG_SENDER_THREADS_KEEPALIVE,
				10));
		this.sendingQueueSize = cfg.getInt(CFG_SENDER_THREADS_QUEUESIZE, 1000);
		this.sendingQueueFair = cfg.getBoolean(
				CFG_SENDER_THREADS_QUEUE_FAIRNESS, false);

		this.messageKeyCacheSize = cfg.getInt(CFG_MESSAGE_KEYCACHE_SIZE, 100);

		this.startPingOnTenant = cfg.getBoolean(
				CFG_START_PING_ON_TENANT_STARTUP, true);

		this.methods = Collections.unmodifiableMap(loadMethods(cfg));
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Class<? extends BroadcastMethod>> loadMethods(ConfigIntf cfg) {
		Map<String, Class<? extends BroadcastMethod>> ret = new LinkedHashMap<String, Class<? extends BroadcastMethod>>();

		for (String method : cfg.getString(CFG_METHODS, DEFAULT_METHODS).split(
				"[,; ]")) {
			String className = cfg.getParameter(CFG_METHOD_PREFIX + method);
			Class<? extends BroadcastMethod> clazz=null;
			try {
				clazz = (Class<? extends BroadcastMethod>) Class.forName(className);
			} catch (Exception e) {
				LOG.error("Invalid messaging mode definition " + method
						+ " -> " + className + " ( error: " + e.getMessage()
						+ ")");

			}
			if (clazz == null)
				continue;
			ret.put(method, clazz);
		}

		return ret;
	}

	public boolean enableAsynchonousSending() {
		return this.asynchronousSending;
	}

	public void setAsynchonousSending(boolean enabled) {
		this.asynchronousSending = enabled;
	}

	public boolean enableClusterMode() {
		return this.clusterMode;
	}

	public void setClusterMode(boolean enabled) {
		this.clusterMode = enabled;
	}

	public int getConfiguredNodeID() {
		return this.configuredID;
	}

	public void setConfiguredNodeID(int id) {
		this.configuredID = id;
	}

	public long getClusterIslandID() {
		return this.clusterIslandID;
	}

	public void setClusterIslandID(long id) {
		this.clusterIslandID = id;
	}

	public long getDynamicNodeID() {
		return this.dynamicID;
	}

	public void setDynamicNodeID(long id) {
		this.dynamicID = id;
	}

	public int getMessageKeyCacheSize() {
		return this.messageKeyCacheSize;
	}

	public void setMessageKeyCacheSize(int size) {
		this.messageKeyCacheSize = size;
	}

	public long getMessageSendingMaxWait() {
		return this.sendingMaxWait;
	}

	public void setMessageSendingMaxWait(long milliseconds) {
		this.sendingMaxWait = milliseconds;
	}

	public int getMessageSendingQueueSize() {
		return this.sendingQueueSize;
	}

	public void setMessageSendingQueueSize(int size) {
		this.sendingQueueSize = size;
	}

	public boolean getMessageSendingQueueFairness() {
		return this.sendingQueueFair;
	}

	public void setMessageSendingQueueFairness(boolean fair) {
		this.sendingQueueFair = fair;
	}

	public int getMessageSendingWaitDelay() {
		return this.sendingWaitDelay;
	}

	public void setMessageSendingWaitDelay(int milliseconds) {
		this.sendingWaitDelay = milliseconds;
	}

	public Map<String, Class<? extends BroadcastMethod>> getMethods() {
		return this.methods;
	}

	public void setMethods(Map<String, Class<? extends BroadcastMethod>> methodClassMap) {
		this.methods = Collections.unmodifiableMap(methodClassMap);
	}

	public int getSenderMaxThreads() {
		return this.senderMaxThreads;
	}

	public void setSenderMaxThreads(int maxThreads) {
		this.senderMaxThreads = maxThreads;
	}

	public int getSenderMinThreads() {
		return this.senderMinThreads;
	}

	public void setSenderMinThreads(int minThreads) {
		this.senderMinThreads = minThreads;
	}

	public int getSenderThreadsKeepAlive() {
		return this.senderKeepAlive;
	}

	public void setSenderThreadsKeepAlive(int milliseconds) {
		this.senderKeepAlive = milliseconds;
	}

	public boolean startPingOnTenantStartup() {
		return this.startPingOnTenant;
	}
}
