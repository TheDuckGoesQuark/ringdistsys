package messages;

import java.io.Serializable;

public class SuccessorMessage implements Serializable {
    private int successorId;

    public SuccessorMessage(int successorId) {
        this.successorId = successorId;
    }

    public int getSuccessorId() {
        return successorId;
    }
}
