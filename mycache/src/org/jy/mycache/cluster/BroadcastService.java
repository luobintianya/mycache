package org.jy.mycache.cluster;

import java.util.Set;

public  abstract interface BroadcastService
{
	  public abstract void send(RawMessage paramRawMessage);

	  public abstract void registerBroadcastListener(BroadcastMessageListener paramBroadcastMessageListener, boolean paramBoolean);

	  public abstract void unregisterBroadcastListener(BroadcastMessageListener paramBroadcastMessageListener);

	  public abstract long getDynamicClusterNodeID();

	  public abstract BroadcastMethod getBroadcastMethod(String paramString);

	  public abstract Set<String> getBroadcastMethodNames();

	  public abstract boolean accept(RawMessage paramRawMessage);
	}
