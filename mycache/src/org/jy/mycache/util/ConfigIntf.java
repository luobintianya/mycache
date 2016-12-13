package org.jy.mycache.util;

import java.util.Map;
 
public  abstract interface ConfigIntf
{
	  public abstract Map<String, String> getAllParameters();

	  public abstract String getParameter(String paramString);

	  public abstract Map<String, String> getParametersMatching(String paramString);

	  public abstract Map<String, String> getParametersMatching(String paramString, boolean paramBoolean);

	  public abstract String setParameter(String paramString1, String paramString2);

	  public abstract String removeParameter(String paramString);

	  public abstract int getInt(String paramString, int paramInt)
	    throws NumberFormatException;

	  public abstract long getLong(String paramString, long paramLong)
	    throws NumberFormatException;

	  public abstract double getDouble(String paramString, double paramDouble)
	    throws NumberFormatException;

	  public abstract boolean getBoolean(String paramString, boolean paramBoolean);

	  public abstract String getString(String paramString1, String paramString2);

	  public abstract char getChar(String paramString, char paramChar)
	    throws IndexOutOfBoundsException;

	  public abstract void registerConfigChangeListener(ConfigChangeListener paramConfigChangeListener);

	  public abstract void unregisterConfigChangeListener(ConfigChangeListener paramConfigChangeListener);

	  public abstract void clearCache();

	  public static abstract interface ConfigChangeListener
	  {
	    public abstract void configChanged(String paramString1, String paramString2);
	  }
	}