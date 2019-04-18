package config;

import node.ElectionMethod;

public class Configuration {

    /**
     * The ID of this node, unique to the system
     */
    private final int nodeId;

    /**
     * Path to file containing information about group members
     */
    private final String listFilePath;

    /**
     * The election method to be used in this dist sys
     */
    private final ElectionMethod electionMethod;

    /**
     * Should this node trigger a full DB refresh
     */
    private final boolean dropEverything;

    public Configuration(int nodeId, String listFilePath, ElectionMethod electionMethod, boolean dropEverything) {
        this.nodeId = nodeId;
        this.listFilePath = listFilePath;
        this.electionMethod = electionMethod;
        this.dropEverything = dropEverything;
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getListFilePath() {
        return listFilePath;
    }

    public ElectionMethod getElectionMethod() {
        return electionMethod;
    }

    public boolean shouldDropEverything() {
        return dropEverything;
    }


}
