package node;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class AddressTranslator {

    private Map<Integer, InetSocketAddress> idMap;
    private Map<SocketAddress, Integer> addressMap;

    public AddressTranslator(Map<Integer, InetSocketAddress> idMap) {
        this.idMap = idMap;
        this.addressMap = new HashMap<>(idMap.size());

        for (Map.Entry<Integer, InetSocketAddress> entry : idMap.entrySet()) {
            addressMap.put(entry.getValue(), entry.getKey());
        }
    }

    public Integer getNodeId(InetSocketAddress socketAddress) {
        return addressMap.get(socketAddress);
    }

    public InetSocketAddress getSocketAddress(Integer nodeId) {
        return idMap.get(nodeId);
    }

    public void add(Integer nodeId, InetSocketAddress socketAddress) {
        idMap.put(nodeId, socketAddress);
        addressMap.put(socketAddress, nodeId);
    }

    public void remove(Integer nodeId) {
        InetSocketAddress addr = getSocketAddress(nodeId);
        idMap.remove(nodeId);
        addressMap.remove(addr);
    }

    public void remove(InetSocketAddress socketAddress) {
        Integer Id = getNodeId(socketAddress);
        addressMap.remove(socketAddress);
        idMap.remove(Id);
    }
}
