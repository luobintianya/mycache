package org.jy.mycache.counter;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Robin
 * 
 *         This class used for count cache hit or evict or miss
 * 
 */
public class CacheCounter implements CacheCounterEntry, Comparable<Object> {
	private final CountHolder global = new CountHolder();

	private final ConcurrentMap<Object, CountHolder> subTypeCounter = new ConcurrentHashMap<Object, CountHolder>();

	private CountHolder getCountHolderForObject(Object typeCode) {
		String typeStr = String.valueOf(typeCode);
		CountHolder holder = (CountHolder) this.subTypeCounter.get(typeStr);
		if (holder == null) {
			holder = new CountHolder();
			CountHolder tmp = (CountHolder) this.subTypeCounter.putIfAbsent(typeStr,
					holder);
			if (tmp != null) {
				holder = tmp;
			}
		}
		return holder;
	}

	@Override
	public String getKeyString() { 
		 return Arrays.deepToString(getTypes().toArray());
	}

	@Override
	public long getHitCount() { 
		  return this.global.hits.longValue();
	}

	@Override
	public long getMissCount() { 
		  return this.global.misses.longValue();
	}

	@Override
	public int getFactor() {
		long hitCount = getHitCount();
		if (hitCount == 0L) {
			return 0;

		}
		return (int) (100L * getHitCount() / (getHitCount() + getMissCount()));
	}

	public void evicted(Object typeCode) {
		this.global.evictions.incrementAndGet();
		getCountHolderForObject(typeCode).evictions.incrementAndGet();
	}

	public void invalidated(Object typeCode) {
		this.global.invalidations.incrementAndGet();
		getCountHolderForObject(typeCode).invalidations.incrementAndGet();
	}

	public void hit(Object typeCode) {
		this.global.hits.incrementAndGet();
		getCountHolderForObject(typeCode).hits.incrementAndGet();
	}

	public void missed(Object typeCode) {
		this.global.misses.incrementAndGet();
		getCountHolderForObject(typeCode).misses.incrementAndGet();
	}

	public void fetched(Object typeCode) {
		this.global.fetches.incrementAndGet();
		getCountHolderForObject(typeCode).fetches.incrementAndGet();
	}

	@Override
	public int compareTo(Object object) {
		if (!(object instanceof CacheCounterEntry)) {
			return -1;
		}
		CacheCounterEntry tmp = (CacheCounterEntry) object;
		return (int) (tmp.getHitCount() - getHitCount());
	}

	public long getMisses() {
		return this.global.misses.longValue();
	}

	public long getMisses(Object typeCode) {
		return getCountHolderForObject(typeCode).misses.longValue();
	}

	public long getEvictions() {
		return this.global.evictions.longValue();
	}

	public long getEvictions(Object typeCode) {
		return getCountHolderForObject(typeCode).evictions.longValue();
	}

	public long getInvalidations() {
		return this.global.invalidations.longValue();
	}

	public long getInvalidations(Object typeCode) {
		return getCountHolderForObject(typeCode).invalidations.longValue();
	}

	public long getInstanceCount() {
		return (getMisses() - getInvalidations() - getEvictions());
	}

	public long getInstanceCount(Object typeCode) {
		return (getMisses(typeCode) - getInvalidations(typeCode) - getEvictions(typeCode));
	}

	public Collection<Object> getTypes() {
		return this.subTypeCounter.keySet();
	}

	public class CountHolder {

		public final AtomicLong hits = new AtomicLong();
		public final AtomicLong fetches = new AtomicLong();
		public final AtomicLong misses = new AtomicLong();
		public final AtomicLong evictions = new AtomicLong();
		public final AtomicLong invalidations = new AtomicLong();

	}
}
