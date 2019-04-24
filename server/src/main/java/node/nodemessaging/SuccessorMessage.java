package node.nodemessaging;

import java.io.Serializable;

public class SuccessorMessage implements Serializable {
    private int successorId;

    public SuccessorMessage(int successorId) {
        this.successorId = successorId;
    }

    public int getSuccessorId() {
        return successorId;
    }

    @Override
    public String toString() {
        return "SuccessorMessage{" +
                "successorId=" + successorId +
                '}';
    }
}
