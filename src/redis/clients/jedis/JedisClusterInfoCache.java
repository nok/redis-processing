package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.ClusterNodeInformation;
import redis.clients.util.ClusterNodeInformationParser;
import redis.clients.util.SafeEncoder;

public class JedisClusterInfoCache {
    public static final ClusterNodeInformationParser nodeInfoParser = new ClusterNodeInformationParser();

    private Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
    private Map<Integer, JedisPool> slots = new HashMap<Integer, JedisPool>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();
    private final GenericObjectPoolConfig poolConfig;

    public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig) {
	this.poolConfig = poolConfig;
    }

    public void discoverClusterNodesAndSlots(Jedis jedis) {
	w.lock();

	try {
	    this.nodes.clear();
	    this.slots.clear();

	    String localNodes = jedis.clusterNodes();
	    for (String nodeInfo : localNodes.split("\n")) {
		ClusterNodeInformation clusterNodeInfo = nodeInfoParser.parse(
			nodeInfo, new HostAndPort(jedis.getClient().getHost(),
				jedis.getClient().getPort()));

		HostAndPort targetNode = clusterNodeInfo.getNode();
		setNodeIfNotExist(targetNode);
		assignSlotsToNode(clusterNodeInfo.getAvailableSlots(),
			targetNode);
	    }
	} finally {
	    w.unlock();
	}
    }

    public void discoverClusterSlots(Jedis jedis) {
	w.lock();

	try {
	    this.slots.clear();

	    List<Object> slots = jedis.clusterSlots();

	    for (Object slotInfoObj : slots) {
		List<Object> slotInfo = (List<Object>) slotInfoObj;

		if (slotInfo.size() <= 2) {
		    continue;
		}

		List<Integer> slotNums = getAssignedSlotArray(slotInfo);

		// hostInfos
		List<Object> hostInfos = (List<Object>) slotInfo.get(2);
		if (hostInfos.size() <= 0) {
		    continue;
		}

		// at this time, we just use master, discard slave information
		HostAndPort targetNode = generateHostAndPort(hostInfos);

		setNodeIfNotExist(targetNode);
		assignSlotsToNode(slotNums, targetNode);
	    }
	} finally {
	    w.unlock();
	}
    }

    private HostAndPort generateHostAndPort(List<Object> hostInfos) {
	return new HostAndPort(SafeEncoder.encode((byte[]) hostInfos.get(0)),
		((Long) hostInfos.get(1)).intValue());
    }

    public void setNodeIfNotExist(HostAndPort node) {
	w.lock();
	try {
	    String nodeKey = getNodeKey(node);
	    if (nodes.containsKey(nodeKey))
		return;

	    JedisPool nodePool = new JedisPool(poolConfig, node.getHost(),
		    node.getPort());
	    nodes.put(nodeKey, nodePool);
	} finally {
	    w.unlock();
	}
    }

    public void assignSlotToNode(int slot, HostAndPort targetNode) {
	w.lock();
	try {
	    JedisPool targetPool = nodes.get(getNodeKey(targetNode));

	    if (targetPool == null) {
		setNodeIfNotExist(targetNode);
		targetPool = nodes.get(getNodeKey(targetNode));
	    }
	    slots.put(slot, targetPool);
	} finally {
	    w.unlock();
	}
    }

    public void assignSlotsToNode(List<Integer> targetSlots,
	    HostAndPort targetNode) {
	w.lock();
	try {
	    JedisPool targetPool = nodes.get(getNodeKey(targetNode));

	    if (targetPool == null) {
		setNodeIfNotExist(targetNode);
		targetPool = nodes.get(getNodeKey(targetNode));
	    }

	    for (Integer slot : targetSlots) {
		slots.put(slot, targetPool);
	    }
	} finally {
	    w.unlock();
	}
    }

    public JedisPool getNode(String nodeKey) {
	r.lock();
	try {
	    return nodes.get(nodeKey);
	} finally {
	    r.unlock();
	}
    }

    public JedisPool getSlotPool(int slot) {
	r.lock();
	try {
	    return slots.get(slot);
	} finally {
	    r.unlock();
	}
    }

    public Map<String, JedisPool> getNodes() {
	r.lock();
	try {
	    return new HashMap<String, JedisPool>(nodes);
	} finally {
	    r.unlock();
	}
    }

    public static String getNodeKey(HostAndPort hnp) {
	return hnp.getHost() + ":" + hnp.getPort();
    }

    public static String getNodeKey(Client client) {
	return client.getHost() + ":" + client.getPort();
    }

    public static String getNodeKey(Jedis jedis) {
	return getNodeKey(jedis.getClient());
    }

    private List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
	List<Integer> slotNums = new ArrayList<Integer>();
	for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo
		.get(1)).intValue(); slot++) {
	    slotNums.add(slot);
	}
	return slotNums;
    }

}
