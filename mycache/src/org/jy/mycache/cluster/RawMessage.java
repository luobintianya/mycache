package org.jy.mycache.cluster;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RawMessage implements Serializable {
	public static final int HEADER_SIZE = 36;
	private int version;
	private long clusterislandpk;
	private long dynamicNodeID;
	private int messageNumber;
	private int numberOfPackets;
	private int currentPacketNumber;
	private transient InetAddress remoteAddress;
	private transient String broadcastMethod;
	private transient MessageKey key;
	private final int kind;
	private final byte[] data;

	protected RawMessage(RawMessage original, int numberOfPackets,
			int currentPacketNumber, int offset, int length) {

		byte[] newData = new byte[length];
		if (original.data == null) {
			this.data = null;
		} else {
			System.arraycopy(original.data, offset, newData, 0, length);
			this.data = newData;
		}

		this.kind = original.kind;
		this.version = original.version;
		this.clusterislandpk = original.clusterislandpk;
		this.dynamicNodeID = original.dynamicNodeID;
		this.messageNumber = original.messageNumber;
		this.remoteAddress = original.remoteAddress;
		this.broadcastMethod = original.broadcastMethod;
		this.key = original.key;

		this.numberOfPackets = numberOfPackets;
		this.currentPacketNumber = currentPacketNumber;
	}

	public RawMessage(byte[] binaryMessageBytes, int offset, int length) {

		if ((length < 0) || (offset < 0)) {
			throw new IllegalArgumentException(
					"length and offset mustn't be negative! length:" + length
							+ " offset:" + offset);
		}
		if (binaryMessageBytes.length < offset + length) {
			throw new IllegalArgumentException(
					"byte array smaller than offset+size ("
							+ binaryMessageBytes.length + "<" + offset + "+"
							+ length + ")");
		}

		this.version = toInt(binaryMessageBytes, 0 + offset);
		this.clusterislandpk = toLong(binaryMessageBytes, 4 + offset);
		this.dynamicNodeID = toLong(binaryMessageBytes, 12 + offset);
		this.messageNumber = toInt(binaryMessageBytes, 20 + offset);
		this.kind = toInt(binaryMessageBytes, 24 + offset);
		this.currentPacketNumber = toInt(binaryMessageBytes, 28 + offset);
		this.numberOfPackets = toInt(binaryMessageBytes, 32 + offset);

		if (length > 36) {
			this.data = new byte[length - 36];
			System.arraycopy(binaryMessageBytes, 36 + offset, this.data, 0,
					this.data.length);
		} else {
			this.data = null;
		}
	}

	public RawMessage(int kind, byte[] data) {
		this.version = -1;
		this.clusterislandpk = -1L;
		this.dynamicNodeID = -1L;
		this.messageNumber = -1;

		this.numberOfPackets = 1;
		this.currentPacketNumber = 1;

		this.remoteAddress = null;
		this.broadcastMethod = null;

		this.kind = kind;
		this.data = data;
	}

	public RawMessage(int kind) {
		this(kind, null);
	}

	public RawMessage(byte[] binaryMessageBytes) {
		this(binaryMessageBytes, 0, binaryMessageBytes.length);
	}

	public RawMessage(byte[] binaryMessageBytes, int offset) {
		this(binaryMessageBytes, offset, binaryMessageBytes.length - offset);
	}

	public void setSenderTransportData(int version, long clusterIsland,
			long dynamicNodeID, int messageNumber) {
		this.version = version;
		this.clusterislandpk = clusterIsland;
		this.dynamicNodeID = dynamicNodeID;
		this.messageNumber = messageNumber;
	}

	public void setPacketSplitInfo(int currentPacketNumber, int numberOfPackets) {
		if ((currentPacketNumber > 0) && (numberOfPackets > 0)
				&& (currentPacketNumber <= numberOfPackets)) {
			this.currentPacketNumber = currentPacketNumber;
			this.numberOfPackets = numberOfPackets;
		} else {
			throw new IllegalArgumentException(
					"both parameters mustn't be negative and current packet number mustn't be greater than the number of packets");
		}
	}

	public void setReceiverTransportData(InetAddress remoteAdress) {
		this.remoteAddress = remoteAdress;
	}

	public int getVersion() {
		return this.version;
	}

	public long getClusterIslandPK() {
		return this.clusterislandpk;
	}

	public long getDynamicNodeID() {
		return this.dynamicNodeID;
	}

	public int getMessageNumber() {
		return this.messageNumber;
	}

	public int getNumberOfPackets() {
		return this.numberOfPackets;
	}

	public int getCurrentPacketNumber() {
		return this.currentPacketNumber;
	}

	public int getKind() {
		return this.kind;
	}

	public byte[] getData() {
		return this.data;
	}

	public boolean matches(int version, long clusterisland) {
		return ((this.version == version) && (this.clusterislandpk == clusterisland));
	}

	public boolean matches(RawMessage other) {
		return matches(other.getVersion(), other.getClusterIslandPK());
	}

	public String toString() {
		String location = ((this.broadcastMethod == null) ? "?"
				: this.broadcastMethod)
				+ "://"
				+ ((this.remoteAddress == null) ? "?" : this.remoteAddress
						.toString());

		return "[" + location + "|ver:" + Integer.toHexString(this.version)
				+ "|" + getClusterIslandPK() + "-" + getDynamicNodeID() + "-"
				+ getMessageNumber() + "|" + this.kind + "|"
				+ this.currentPacketNumber + "of" + this.numberOfPackets
				+ " (content: " + ((this.data == null) ? 0 : this.data.length)
				+ " bytes)]";
	}

	public InetAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	public void setRemoteAddress(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getBroadcastMethod() {
		return this.broadcastMethod;
	}

	public void setBroadcastMethod(String broadcastMethod) {
		this.broadcastMethod = broadcastMethod;
	}

	public Object getMessageKey() {
		if (this.key == null) {
			if (this.dynamicNodeID == -1L) {
				throw new IllegalStateException(
						"transport information not set yet");
			}
			this.key = new MessageKey(getDynamicNodeID(), getMessageNumber());
		}
		return this.key;
	}

	private static class MessageKey {
		private final long dynamicID;
		private final int messageNumber;
		private final int hashCodeCache;

		MessageKey(long dynamicID, int messageNumber) {
			this.dynamicID = dynamicID;
			this.messageNumber = messageNumber;
			this.hashCodeCache = ((int) dynamicID ^ messageNumber);
		}

		public int hashCode() {
			return this.hashCodeCache;
		}

		public boolean equals(Object obj) {
			try {
				return ((super.equals(obj)) || ((((MessageKey) obj).messageNumber == this.messageNumber) && (((MessageKey) obj).dynamicID == this.dynamicID)));
			} catch (ClassCastException localClassCastException) {
			}
			return false;
		}
	}

	public boolean mustSplit(int packetsize) {
		return (((getData() == null) ? 0 : getData().length) + 36 > packetsize);
	}

	public List<RawMessage> split(int packetSize) {
		if ((packetSize < 36) || (this.data == null))

		{
			return Collections.singletonList(this);

		}

		int normalContentLength = packetSize - 36;

		int lastPacketContentLength = this.data.length % normalContentLength;

		int packetCount = this.data.length / normalContentLength;
		if (lastPacketContentLength > 0) {
			++packetCount;

		}

		List<RawMessage> splittedMessages = new ArrayList<RawMessage>(
				packetCount);

		int offset = 0;
		int currentPacketNumber = 1;
		while (currentPacketNumber <= packetCount) {
			int copyLength = normalContentLength;
			if (currentPacketNumber == packetCount) {
				copyLength = (lastPacketContentLength == 0) ? normalContentLength
						: lastPacketContentLength;
			}

			splittedMessages.add(new RawMessage(this, packetCount,
					currentPacketNumber, offset, copyLength));
			++currentPacketNumber;
			offset += normalContentLength;
		}
		return splittedMessages;
	}

	public RawMessage join(Collection<RawMessage> messageparts) {
		if ((messageparts == null) || (messageparts.isEmpty())) {
			return this;

		}

		List<RawMessage> messages = new ArrayList<RawMessage>(messageparts);
		if (!(messages.contains(this))) {
			messages.add(this);
		}

		int overalDataLenght = 0;

		Collections.sort(messages, comp);
		for (RawMessage sortedElement : messages) {
			overalDataLenght += ((sortedElement.getData() == null) ? 0
					: sortedElement.getData().length);
		}

		byte[] joinedData = new byte[overalDataLenght];
		int i = 0;
		for (RawMessage sortedElement : messages) {
			int sortedDataLenght = (sortedElement.getData() == null) ? 0
					: sortedElement.getData().length;
			if (sortedDataLenght <= 0)
				continue;
			System.arraycopy(sortedElement.getData(), 0, joinedData, i,
					sortedDataLenght);
			i += sortedDataLenght;

		}

		RawMessage joinedMessage = new RawMessage(getKind(), joinedData);
		joinedMessage.setBroadcastMethod(getBroadcastMethod());
		joinedMessage.setReceiverTransportData(getRemoteAddress());

		joinedMessage.setSenderTransportData(getVersion(),
				getClusterIslandPK(), getDynamicNodeID(), getMessageNumber());
		return joinedMessage;
	}

	public byte[] toRawByteArray() {
		int version = getVersion();
		byte[] userData = getData();
		long clusterisland = getClusterIslandPK();
		long dynamicNodeID = getDynamicNodeID();
		int messageNumber = getMessageNumber();
		int currentPacketNumber = getCurrentPacketNumber();
		int numberOfPackets = getNumberOfPackets();
		int kind = getKind();

		int len = (userData == null) ? 36 : userData.length + 36;
		byte[] result = new byte[len];

		result[0] = (byte) (version >> 24 & 0xFF);
		result[1] = (byte) (version >> 16 & 0xFF);
		result[2] = (byte) (version >> 8 & 0xFF);
		result[3] = (byte) (version >> 0 & 0xFF);

		result[4] = (byte) (int) (clusterisland >>> 56 & 0xFF);
		result[5] = (byte) (int) (clusterisland >>> 48 & 0xFF);
		result[6] = (byte) (int) (clusterisland >>> 40 & 0xFF);
		result[7] = (byte) (int) (clusterisland >>> 32 & 0xFF);
		result[8] = (byte) (int) (clusterisland >>> 24 & 0xFF);
		result[9] = (byte) (int) (clusterisland >>> 16 & 0xFF);
		result[10] = (byte) (int) (clusterisland >>> 8 & 0xFF);
		result[11] = (byte) (int) (clusterisland >>> 0 & 0xFF);

		result[12] = (byte) (int) (dynamicNodeID >>> 56 & 0xFF);
		result[13] = (byte) (int) (dynamicNodeID >>> 48 & 0xFF);
		result[14] = (byte) (int) (dynamicNodeID >>> 40 & 0xFF);
		result[15] = (byte) (int) (dynamicNodeID >>> 32 & 0xFF);
		result[16] = (byte) (int) (dynamicNodeID >>> 24 & 0xFF);
		result[17] = (byte) (int) (dynamicNodeID >>> 16 & 0xFF);
		result[18] = (byte) (int) (dynamicNodeID >>> 8 & 0xFF);
		result[19] = (byte) (int) (dynamicNodeID >>> 0 & 0xFF);

		result[20] = (byte) (messageNumber >> 24 & 0xFF);
		result[21] = (byte) (messageNumber >> 16 & 0xFF);
		result[22] = (byte) (messageNumber >> 8 & 0xFF);
		result[23] = (byte) (messageNumber >> 0 & 0xFF);

		result[24] = (byte) (kind >> 24 & 0xFF);
		result[25] = (byte) (kind >> 16 & 0xFF);
		result[26] = (byte) (kind >> 8 & 0xFF);
		result[27] = (byte) (kind >> 0 & 0xFF);

		result[28] = (byte) (currentPacketNumber >> 24 & 0xFF);
		result[29] = (byte) (currentPacketNumber >> 16 & 0xFF);
		result[30] = (byte) (currentPacketNumber >> 8 & 0xFF);
		result[31] = (byte) (currentPacketNumber >> 0 & 0xFF);

		result[32] = (byte) (numberOfPackets >> 24 & 0xFF);
		result[33] = (byte) (numberOfPackets >> 16 & 0xFF);
		result[34] = (byte) (numberOfPackets >> 8 & 0xFF);
		result[35] = (byte) (numberOfPackets >> 0 & 0xFF);

		if (userData != null) {
			System.arraycopy(userData, 0, result, 36, userData.length);
		}
		return result;
	}

	private static long toLong(byte[] byt, int offset) {
		return ((byt[(7 + offset)] & 0xFF) + ((byt[(6 + offset)] & 0xFF) << 8)
				+ ((byt[(5 + offset)] & 0xFF) << 16)
				+ ((byt[(4 + offset)] & 0xFF) << 24)
				+ ((byt[(3 + offset)] & 0xFF) << 32)
				+ ((byt[(2 + offset)] & 0xFF) << 40)
				+ ((byt[(1 + offset)] & 0xFF) << 48) + ((byt[(0 + offset)] & 0xFF) << 56));
	}

	private static int toInt(byte[] byt, int offset) {
		return ((byt[(3 + offset)] & 0xFF) + ((byt[(2 + offset)] & 0xFF) << 8)
				+ ((byt[(1 + offset)] & 0xFF) << 16) + ((byt[(0 + offset)] & 0xFF) << 24));
	}

	private static Comparator<RawMessage> comp = new Comparator<RawMessage>()

	{
		public int compare(RawMessage obj1, RawMessage obj2) {
			return (obj1.getCurrentPacketNumber() - obj2
					.getCurrentPacketNumber());
		}

	};
}
