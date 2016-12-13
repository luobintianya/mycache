package org.jy.mycache.cluster;

import java.util.Map;

public abstract interface BroadcastServiceConfiguration
{
	  public abstract boolean enableClusterMode();

	  public abstract Map<String, Class<? extends BroadcastMethod>> getMethods();

	  public abstract long getDynamicNodeID();

	  public abstract int getConfiguredNodeID();

	  public abstract long getClusterIslandID();

	  public abstract boolean startPingOnTenantStartup();

	  public abstract boolean enableAsynchonousSending();

	  public abstract int getSenderMinThreads();

	  public abstract int getSenderMaxThreads();

	  public abstract int getSenderThreadsKeepAlive();

	  public abstract int getMessageSendingQueueSize();

	  public abstract boolean getMessageSendingQueueFairness();

	  public abstract long getMessageSendingMaxWait();

	  public abstract int getMessageSendingWaitDelay();

	  public abstract int getMessageKeyCacheSize();
	}