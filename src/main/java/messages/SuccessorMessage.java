package messages;

import java.net.InetSocketAddress;

public class SuccessorMessage {

    private InetSocketAddress successorAddress;

    public SuccessorMessage(InetSocketAddress successorAddress) {
        this.successorAddress = successorAddress;
    }

    public InetSocketAddress getSuccessorAddress() {
        return successorAddress;
    }
}
