package org.jy.mycache.cluster;

public abstract interface BroadcastMessageListener
{
	  public abstract boolean processMessage(RawMessage paramRawMessage);
	}