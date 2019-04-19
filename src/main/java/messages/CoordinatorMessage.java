package messages;

import java.io.Serializable;

public class CoordinatorMessage implements Serializable {

    private int coordinatorId;

    public CoordinatorMessage(int coordinatorId) {
        this.coordinatorId = coordinatorId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    @Override
    public String toString() {
        return "CoordinatorMessage{" +
                "coordinatorId=" + coordinatorId +
                '}';
    }
}
