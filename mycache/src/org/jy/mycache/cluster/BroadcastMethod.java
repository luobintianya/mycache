package org.jy.mycache.cluster;

import java.util.Map;

public  abstract interface BroadcastMethod
{
	  public abstract void init(BroadcastService paramBroadcastService);

	  public abstract void shutdown();

	  public abstract void send(RawMessage paramRawMessage);

	  public abstract void registerProcessor(BroadcastMessageListener paramBroadcastMessageListener);

	  public abstract void unregisterProcessor(BroadcastMessageListener paramBroadcastMessageListener);

	  public abstract Map<String, String> getSettings();
	}