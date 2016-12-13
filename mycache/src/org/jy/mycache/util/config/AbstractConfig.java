package org.jy.mycache.util.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.jy.mycache.util.ConfigIntf;
import org.jy.mycache.util.Key;
import org.jy.mycache.util.StringUtils;

public abstract class AbstractConfig<T> implements ConfigIntf {
	private static final Logger LOG = Logger.getLogger(AbstractConfig.class);

	private final Set<ConfigIntf.ConfigChangeListener> listeners = new CopyOnWriteArraySet();

	private final Map<Key, Object> convertCache = new ConcurrentHashMap<Key, Object>();

	private final ConfigKeyGetter<Boolean> booleanGetter = new BooleanConfigKeyGetter();
	private final ConfigKeyGetter<Integer> integerGetter = new IntegerConfigKeyGetter();
	private final ConfigKeyGetter<Long> longGetter = new LongConfigKeyGetter();
	private final ConfigKeyGetter<Double> doubleGetter = new DoubleConfigKeyGetter();
	private final ConfigKeyGetter<Character> charGetter = new CharacterConfigKeyGetter();
	private final ConfigKeyGetter<String> stringGetter = new StringConfigKeyGetter();

	public void clearCache() {
		this.convertCache.clear();
	}

	public void registerConfigChangeListener(
			ConfigIntf.ConfigChangeListener listener) {
		this.listeners.add(listener);
	}

	public void unregisterConfigChangeListener(
			ConfigIntf.ConfigChangeListener listener) {
		this.listeners.remove(listener);
	}

	protected void notifyListeners(String key, String oldValue, String newValue) {
		if ((oldValue == newValue)
				|| ((oldValue != null) && (oldValue.equals(newValue))))
			return;
		this.convertCache.clear();
		for (ConfigIntf.ConfigChangeListener listener : this.listeners) {
			try {
				listener.configChanged(key, newValue);
			} catch (Exception e) {
				LOG.error("error notifying cfg listener " + listener, e);
			}
		}
	}

	public final Map<String, String> getParametersMatching(String keyRegExp) {
		return getParametersMatching(keyRegExp, false);
	}

	public boolean getBoolean(String key, boolean def) {
		Boolean value = Boolean.valueOf(def);
		return ((Boolean) this.booleanGetter.get(key, value)).booleanValue();
	}

	public int getInt(String key, int def) throws NumberFormatException {
		Integer value = Integer.valueOf(def);
		return ((Integer) this.integerGetter.get(key, value)).intValue();
	}

	public long getLong(String key, long def) throws NumberFormatException {
		Long value = Long.valueOf(def);
		return ((Long) this.longGetter.get(key, value)).longValue();
	}

	public double getDouble(String key, double def)
			throws NumberFormatException {
		Double value = Double.valueOf(def);
		return ((Double) this.doubleGetter.get(key, value)).doubleValue();
	}

	public String getString(String key, String def) {
		return ((String) this.stringGetter.get(key, def));
	}

	public char getChar(String key, char def) throws IndexOutOfBoundsException {
		Character value = Character.valueOf(def);
		return ((Character) this.charGetter.get(key, value)).charValue();
	}

	private abstract class ConfigKeyGetter<T> {
		Object get(String key, T value) {
			try {
				Key<String, T> searchKey = Key.get(key, value);
				Object cached = (Object) convertCache.get(searchKey);
				if (cached == null) {
					cached = convert(AbstractConfig.this.getParameter(key),
							value);
					if (cached != null) {
						convertCache.put(Key.create(key, value), cached);
					}
				}
				return cached;
			} catch (ClassCastException localClassCastException) {
			}
			return convert(AbstractConfig.this.getParameter(key), value);
		}

		abstract T convert(String paramString, T paramT)
				throws NumberFormatException;
	}

	private class BooleanConfigKeyGetter extends ConfigKeyGetter<Boolean> {
		private BooleanConfigKeyGetter() {
			super();
		}

		Boolean convert(String value, Boolean def) {
			return ((value == null) ? def : Boolean.valueOf((Boolean.TRUE
					.equals(Boolean.valueOf(value))) || ("1".equals(value))));
		}
	}

	private class IntegerConfigKeyGetter extends ConfigKeyGetter<Integer>

	{
		private IntegerConfigKeyGetter() {
			super();
		}

		Integer convert(String value, Integer def) throws NumberFormatException {
			return ((value == null) ? def : Integer.valueOf(value));
		}
	}

	private class LongConfigKeyGetter extends ConfigKeyGetter<Long> {
		private LongConfigKeyGetter() {
			super();
		}

		Long convert(String value, Long def) {
			return ((value == null) ? def : Long.valueOf(value));
		}

	}

	private class CharacterConfigKeyGetter extends ConfigKeyGetter<Character> {
		private CharacterConfigKeyGetter() {
			super();
		}

		Character convert(String value, Character def) {
			return (((value == null) || (value.length() == 0)) ? def
					: Character.valueOf(value.charAt(0)));
		}

	}

	private class DoubleConfigKeyGetter extends ConfigKeyGetter<Double> {
		private DoubleConfigKeyGetter() {
			super();
		}

		Double convert(String value, Double def) {
			return ((value == null) ? def : Double.valueOf(value));
		}
	}

	private class StringConfigKeyGetter extends ConfigKeyGetter<String> {

		private StringConfigKeyGetter() {
			super();
		}

		String convert(String value, String def) {
			return ((StringUtils.isEmpty(value)) ? def : value);
		}
	}

}
