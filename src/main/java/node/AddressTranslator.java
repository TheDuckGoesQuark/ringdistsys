package node;


import globalpersistence.NodeRow;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressTranslator {

    private final Map<Integer, InetSocketAddress> idMap;

    public AddressTranslator(List<NodeRow> nodes) {
        this.idMap = new HashMap<>();
        for (NodeRow node : nodes) {
            idMap.put(node.getNodeId(), new InetSocketAddress(node.getAddress(), node.getPort()));
        }
    }

    public InetSocketAddress getSocketAddress(Integer nodeId) {
        return idMap.get(nodeId);
    }
}
