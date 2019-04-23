package node;


import node.ringstore.VirtualNode;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressTranslator {

    private final Map<Integer, InetSocketAddress> idMap;

    public AddressTranslator(List<VirtualNode> nodes) {
        this.idMap = new HashMap<>();
        for (VirtualNode node : nodes) {
            idMap.put(node.getNodeId(), new InetSocketAddress(node.getAddress(), node.getPort()));
        }
    }

    public InetSocketAddress getSocketAddress(Integer nodeId) {
        return idMap.get(nodeId);
    }
}
