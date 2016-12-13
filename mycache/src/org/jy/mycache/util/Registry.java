package org.jy.mycache.util;

import org.jy.mycache.util.config.MycacheConfig;


public class Registry { 
	
	 private static ConfigIntf config =new MycacheConfig(null, true, 0);
	 

	/**
	 * @return the config
	 */
	public static ConfigIntf getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(ConfigIntf config) {
		this.config = config;
	}
	
	public static long  getClusterIslandPK(){
		return 0;
		
	}
	public static int  getClusterID(){
		return 0;
		
	}
	
	
}
