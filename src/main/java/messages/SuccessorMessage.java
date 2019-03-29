package messages;

import java.net.InetSocketAddress;

public class SuccessorMessage {
    private int successorId;

    public SuccessorMessage(int successorId) {
        this.successorId = successorId;
    }

    public int getSuccessorId() {
        return successorId;
    }
}
