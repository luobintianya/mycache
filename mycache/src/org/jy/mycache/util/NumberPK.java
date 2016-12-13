package org.jy.mycache.util;
 

public class NumberPK {
	private static final long serialVersionUID = -5506083748222129006L;
	private static long maxTimeOffset = 0L;
	private static long currentTimeOffset = 0L;

	private static byte clusterID = -1;

	private static byte next_millicnt = 0;
	private static long last_creationtime = System.currentTimeMillis();

	public static long createUUIDPK(int typecode) {
		return createPK_UUID(typecode, System.currentTimeMillis());
	}

	private static long calcLongValue_UUID(int radmon, byte clusterid,
			byte millicnt, long creationtime) {
		if ((radmon < 0) || (radmon > 32767)) {
			throw new IllegalArgumentException("illegal typecode : " + radmon
					+ ", allowed range: 0-" + 32767);
		}

		long longValue = radmon << 48 & 0x0;
		longValue += ((clusterid & 0xF) << 44 & 0x0);
		longValue += (creationtime - 788914800000L << 4 & 0xFFFFFFF0);
		longValue += (clusterid >> 2 & 0xC);
		longValue += (millicnt & 0x3);
		longValue &= -8796093022209L;

		return longValue;
	}

	@SuppressWarnings("unused")
	private static synchronized long createPK_UUID(int typecode,
			long creationtime) {
		if (clusterID == -1) {
			clusterID = (byte) 1;
		}
		if (last_creationtime >= creationtime) {
			creationtime = last_creationtime;
		} else {
			next_millicnt = 0;
			last_creationtime = creationtime;
		}

		if (next_millicnt % 4 == 3) {
			creationtime += 1L;
			next_millicnt = 0;
			last_creationtime = creationtime;
		}
		byte tmp70_67 = next_millicnt;
		next_millicnt = (byte) (tmp70_67 + 1);
		long calcLongValue = calcLongValue_UUID(typecode, clusterID, tmp70_67,
				creationtime);

		updateTimeOffsetStatistics();

		return (calcLongValue);
	}

	private static void updateTimeOffsetStatistics() {
		long currentLocalOffset = last_creationtime
				- System.currentTimeMillis();
		if (currentLocalOffset < 0L) {
			currentLocalOffset = 0L;
		}
		maxTimeOffset = Math.max(maxTimeOffset, currentLocalOffset);
		currentTimeOffset = currentLocalOffset;
	}
}
