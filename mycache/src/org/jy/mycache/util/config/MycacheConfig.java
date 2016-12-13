package org.jy.mycache.util.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jy.mycache.util.ConfigIntf;

public class MycacheConfig extends AbstractConfig<String> {

	public static final String STANDALONE_PREFIX = "standalone";
	private final ConfigIntf config;

	public MycacheConfig(Properties map, boolean standaloneMode, int clusterNode) {
		if (clusterNode != -1) {
			map.setProperty(Config.Params.CLUSTER_ID,
					String.valueOf(clusterNode));
		}

		int clusterid;
		try {
			clusterid = Integer.parseInt(map
					.getProperty(Config.Params.CLUSTER_ID));
		} catch (NumberFormatException localNumberFormatException) {
			throw new RuntimeException("cluster id invalid or not set");
		}

		Map<String, String> map_Standalone_Cluster = new HashMap<String, String>();
		Map<String, String> map_Standalone = new HashMap<String, String>();
		Map<String, String> map_Cluster = new HashMap<String, String>();
		Map<String, String> map_Fallback = new HashMap<String, String>();

		Pattern pattern = Pattern
				.compile("^(standalone\\.)?(cluster\\.(\\d+)\\.)?(.*)$");

		String idStr = Integer.toString(clusterid);

		int count = 0;

		for (Iterator localIterator = map.keySet().iterator(); localIterator
				.hasNext();) {
			Object prop = localIterator.next(); 
			String key = (String) prop;
			String value = map.getProperty(key);
			Matcher matcher = pattern.matcher(key);
			if (matcher.matches())

			{
				if (matcher.group(1) != null)

				{
					if (!(standaloneMode)) {
						continue;
					}

					if (matcher.group(2) != null)

					{
						if (!(idStr.equals(matcher.group(3)))) {
							continue;
						}

						map_Standalone_Cluster.put(matcher.group(4), value);

					} else {
						map_Standalone.put(matcher.group(4), value);
					}
				}

				if (matcher.group(2) != null)

				{
					if (!(idStr.equals(matcher.group(3)))) {
						continue;
					}

					map_Cluster.put(matcher.group(4), value);

				} else {
					map_Fallback.put(matcher.group(4), value);
				}
				++count;
			} else {
				System.err.println("wrong config key '" + key + "'");
			}
		}
		Map<String,String> merged = new HashMap<String,String>(count);
		merged.putAll(map_Fallback);
		merged.putAll(map_Cluster);
		merged.putAll(map_Standalone);
		merged.putAll(map_Standalone_Cluster);

		this.config = new FastHashMapConfig(merged);
	}

	public Map<String, String> getAllParameters() {
		Map<String,String> newP = new HashMap<String,String>();
		for (Map.Entry<String,String> entry : this.config.getAllParameters().entrySet()) {
			newP.put(entry.getKey(), entry.getValue());
		}
		return Collections.unmodifiableMap(newP);
	}

	public String getParameter(String key) {
		return this.config.getParameter(key);
	}

	public Map<String, String> getParametersMatching(String keyRegExp,
			boolean stripMatchingKey) {
		return this.config.getParametersMatching(keyRegExp, stripMatchingKey);
	}

	public String setParameter(String key, String value) {
		String prev = this.config.setParameter(key, value);
		notifyListeners(key, prev, value);
		return prev;
	}

	public String removeParameter(String key) {
		String prev = this.config.removeParameter(key);
		notifyListeners(key, prev, null);
		return prev;
	}

}
