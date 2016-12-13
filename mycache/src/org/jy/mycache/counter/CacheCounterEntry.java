package org.jy.mycache.counter;

public abstract interface CacheCounterEntry {
  
	  /**
	   * get the cache type info 
	 * @return
	 */
	public abstract String getKeyString();

	  /**
	   * get global hit info count
	 * @return
	 */
	public abstract long getHitCount();

	  /**get miss count
	 * @return
	 */
	public abstract long getMissCount();

	  /**
	   * get the percent of hit in all
	 * @return
	 */
	public abstract int getFactor();
}
