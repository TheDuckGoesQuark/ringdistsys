package node.jdbc;

import logging.LoggerFactory;
import node.clientmessaging.messages.ChatMessage;
import node.clientmessaging.repositories.MessageRepository;
import node.clientmessaging.repositories.UserGroupRepository;

import java.io.IOException;
import java.sql.*;
import java.util.Set;
import java.util.logging.Logger;

public class MessagingDatabaseConnection implements UserGroupRepository, MessageRepository {

    private static final String CONNECTION_STRING = "jdbc:mariadb://jm354.host.cs.st-andrews.ac.uk/jm354_distsys";
    private static final String USERNAME = "jm354";
    private static final String PASSOWRD = "722Em!9LLknjhZ";

    private static final String NODE_TABLE_NAME = "nodes";
    private static final String COORDINATOR_TABLE_NAME = "coordinators";

    private static final String NODE_SCHEMA =
            "CREATE TABLE " + NODE_TABLE_NAME + " ( " +
                    "nodeId INT NOT NULL," +
                    "address VARCHAR(255) NOT NULL," +
                    "coordinationPort INT NOT NULL," +
                    "clientPort INT NOT NULL," +
                    "successorId INT," +
                    "PRIMARY KEY (nodeId)," +
                    "FOREIGN KEY (successorId) REFERENCES " + NODE_TABLE_NAME + "(nodeId)" +
                    ")";

    private static final String COORDINATOR_SCHEMA =
            "CREATE TABLE " + COORDINATOR_TABLE_NAME + " ( " +
                    "coordinatorId INT," +
                    "singleRowLock BOOLEAN," +
                    "PRIMARY KEY (singleRowLock)," +
                    "FOREIGN KEY (coordinatorId) REFERENCES " + NODE_TABLE_NAME + "(nodeId)" +
                    "ON DELETE SET NULL" +
                    ")";

    private static final String DROP_COORDINATOR = "DROP TABLE " + COORDINATOR_TABLE_NAME;
    private static final String DROP_NODE = "DROP TABLE " + NODE_TABLE_NAME;

    private static final String SELECT_ALL =
            "SELECT n.*, c.coordinatorId FROM " + NODE_TABLE_NAME + " n " +
                    "LEFT JOIN " + COORDINATOR_TABLE_NAME + " c ON c.coordinatorId ";

    private static final String SELECT_ALL_WITH_SUCCESSOR =
            "SELECT n.*, c.coordinatorId FROM " + NODE_TABLE_NAME + " n " +
                    "LEFT JOIN " + COORDINATOR_TABLE_NAME + " c ON c.coordinatorId " +
                    "WHERE n.successorId IS NOT NULL";

    private static final String SELECT_ALL_WITH_SUCCESSOR_AND_IDS_GREATER_THAN =
            "SELECT n.*, c.coordinatorId FROM " + NODE_TABLE_NAME + " n " +
                    "LEFT JOIN " + COORDINATOR_TABLE_NAME + " c ON c.coordinatorId " +
                    "WHERE n.successorId IS NOT NULL AND n.nodeId > ?";

    private static final String COUNT_NODES =
            "SELECT count(*) as nodeCount FROM " + NODE_TABLE_NAME;

    private static final String COUNT_NODES_IN_RING =
            "SELECT count(*) as nodeCount FROM " + NODE_TABLE_NAME + " " +
                    "WHERE successorId IS NOT NULL";

    private static final String INSERT_NODE =
            "INSERT INTO " + NODE_TABLE_NAME + " VALUES (?, ?, ?, ?, NULL)";

    private static final String INSERT_SUCCESSOR =
            "UPDATE " + NODE_TABLE_NAME + " SET successorId = ? where nodeId = ?";

    private static final String REMOVE_SUCCESSOR =
            "UPDATE " + NODE_TABLE_NAME + " SET successorId = NULL where nodeId = ?";

    private static final String INSERT_COORDINATOR =
            "INSERT INTO " + COORDINATOR_TABLE_NAME + " VALUES (?, True) ON DUPLICATE KEY UPDATE coordinatorId = ?";

    private final Logger logger = LoggerFactory.getLogger();

    private final boolean doFullRestart;

    public MessagingDatabaseConnection(boolean doFullRestart) {
        this.doFullRestart = doFullRestart;
    }

    /**
     * Checks for the existence of the schema
     *
     * @return true if table exists
     */
    private boolean schemaIsInitialized(Connection conn) throws SQLException {
        final DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getTables(null, null, NODE_TABLE_NAME, null)) {
            return rs.next();
        }
    }

    /**
     * Inserts database schema to DB
     *
     * @param conn connection to DB
     */
    private void initializeSchema(Connection conn) throws SQLException {
        try (
                final PreparedStatement nodeStatement = conn.prepareStatement(NODE_SCHEMA);
                final PreparedStatement coordinatorStatement = conn.prepareStatement(COORDINATOR_SCHEMA)
        ) {
            conn.setAutoCommit(false);
            nodeStatement.executeUpdate();
            coordinatorStatement.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Removes all tables from the database
     */
    private void dropEverything(Connection conn) throws SQLException {
        try (
                final PreparedStatement dropNode = conn.prepareStatement(DROP_NODE);
                final PreparedStatement dropCoord = conn.prepareStatement(DROP_COORDINATOR)
        ) {
            conn.setAutoCommit(false);
            dropCoord.executeUpdate();
            dropNode.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Initializes the database
     */
    public void initialize() {
        try (final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD)) {
            if (doFullRestart) {
                dropEverything(conn);
            }

            if (!schemaIsInitialized(conn)) {
                initializeSchema(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }
    }

    @Override
    public void sendMessage(ChatMessage userMessage) {

    }

    @Override
    public boolean checkForMessageForUsers(Set<String> usernames) {
        return false;
    }

    @Override
    public void addUser(String username) throws IOException {

    }

    @Override
    public void removeUser(String username) throws IOException {

    }

    @Override
    public void addUserToGroup(String username, String groupname) throws IOException {

    }

    @Override
    public void removeUserFromGroup(String username, String groupname) throws IOException {

    }

    @Override
    public Set<String> getAllUsersInGroup(String groupname) throws IOException {
        return null;
    }
}
