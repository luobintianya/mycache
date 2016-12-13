package org.jy.mycache.util;

import java.io.Serializable; 
 

public final class Key<K, V> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private K element1;
	private V element2;
    private static final ThreadLocal<Key> KEY_HOLDER = new ThreadLocal<Key>() ;
 
	protected Key<K, V>  initialValue() {
		return new Key<K, V> (null, null);

	}

	private Key(K element1, V element2) {
		this.element1 = element1;
		this.element2 = element2;
	}

	private void set(K element1, V element2) {
		this.element1 = element1;
		this.element2 = element2;
	}

	public V getValue() {
		return this.element2;
	}

	public K getKey() {
		return this.element1;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (super.getClass() != o.getClass())) {
			return false;
		}

		Key<K, V>  configKey = (Key) o;

		return ((equalRef(this.element1, configKey.element1)) && (equalRef(
				this.element2, configKey.element2)));
	}

	private boolean equalRef(Object ref1, Object ref2) {
		return ((ref1 == ref2) || ((ref1 != null) && (ref1.equals(ref2))));
	}

	public String toString() {
		return String.format("<%s,%s>", new Object[] { this.element1,
				this.element2 });
	}

	public int hashCode() {
		int result = (this.element2 != null) ? this.element2.hashCode() : 0;
		result = 31 * result + this.element1.hashCode();
		return result;
	}

	public static <K, V> Key<K, V> get(K element1, V element2) {
		Key<K, V>  searchKey = (Key<K, V>) KEY_HOLDER.get();
		searchKey.set(element1, element2);
		return searchKey;
	}

	public static void clear() {
		((Key) KEY_HOLDER.get()).set(null, null);
	}

	public static <K, V> Key<K, V> create(K element1, V element2) {
		return new Key(element1, element2);
	}
}