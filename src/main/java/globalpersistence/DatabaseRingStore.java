package globalpersistence;

import logging.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class DatabaseRingStore implements RingStore {

    private static final String CONNECTION_STRING = "jdbc:mariadb://jm354.host.cs.st-andrews.ac.uk/jm354_distsys";
    private static final String USERNAME = "jm354";
    private static final String PASSOWRD = "722Em!9LLknjhZ";

    private static final String NODE_TABLE_NAME = "nodes";
    private static final String SUCCESSOR_TABLE_NAME = "successors";
    private static final String COORDINATOR_TABLE_NAME = "coordinators";

    private static final String NODE_SCHEMA =
            "CREATE TABLE " + NODE_TABLE_NAME + "( \n" +
                    "    nodeId INT NOT NULL,\n" +
                    "    address VARCHAR(255) NOT NULL,\n" +
                    "    port INT NOT NULL,\n" +
                    "    PRIMARY KEY (nodeId)\n" +
                    ")";

    private static final String SUCCESSOR_SCHEMA =
            "CREATE TABLE " + SUCCESSOR_TABLE_NAME + "( \n" +
                    "    nodeId INT NOT NULL,\n" +
                    "    successorId INT NOT NULL,\n" +
                    "    PRIMARY KEY (nodeId),\n" +
                    "    FOREIGN KEY (successorId) REFERENCES " + NODE_TABLE_NAME + "(nodeId)\n" +
                    ")";

    private static final String COORDINATOR_SCHEMA =
            "CREATE TABLE " + COORDINATOR_TABLE_NAME + "(\n" +
                    "    coordinatorId INT,\n" +
                    "    singleRowLock BOOLEAN,\n" +
                    "    PRIMARY KEY (singleRowLock),\n" +
                    "    FOREIGN KEY (coordinatorId) REFERENCES " + NODE_TABLE_NAME + "(nodeId)\n" +
                    "    ON DELETE SET NULL\n" +
                    ")";

    private static final String COUNT_NODES =
            "SELECT count(*) as nodeCount FROM " + NODE_TABLE_NAME;

    private static final String INSERT_NODE =
            "INSERT INTO " + NODE_TABLE_NAME + " VALUES (?, ?, ?)";

    private static final String INSERT_SUCCESSOR =
            "INSERT INTO " + SUCCESSOR_TABLE_NAME + " VALUES (?, ?)";

    private static final String REMOVE_SUCCESSOR =
            "DELETE FROM " + SUCCESSOR_TABLE_NAME + " WHERE nodeId = ?";

    private static final String INSERT_COORDINATOR =
            "INSERT INTO " + COORDINATOR_TABLE_NAME + " VALUES (?, True) ON DUPLICATE KEY UPDATE coordinatorId = ?";

    private Logger logger = LoggerFactory.getLogger();
    private String nodelistpath;

    /**
     * Creates a database ring store instance with the path to the file it can use to initialize the database.
     *
     * @param nodelistpath path to file with node ids and socket addresses
     */
    public DatabaseRingStore(String nodelistpath) {
        this.nodelistpath = nodelistpath;
    }

    /**
     * Closes object and ignores any exceptions
     *
     * @param closeable object to close
     */
    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * Retrieves an integer from the result set that can be null
     *
     * @param name name of column to get
     * @param rs   result set to fetch from
     * @return value of the column
     * @throws SQLException :(
     */
    private static Optional<Integer> getNullableInt(String name, ResultSet rs) throws SQLException {
        final Integer value = rs.getInt(name);
        if (rs.wasNull()) return Optional.empty();
        else return Optional.of(value);
    }

    /**
     * Checks for the existence of the schema
     *
     * @return true if table exists
     */
    private boolean schemaIsInitialized(Connection conn) throws SQLException {
        ResultSet rs = null;

        try {
            DatabaseMetaData meta = conn.getMetaData();
            rs = meta.getTables(null, null, NODE_TABLE_NAME, null);

            return rs.next();
        } finally {
            closeQuietly(rs);
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
                final PreparedStatement successorStatement = conn.prepareStatement(SUCCESSOR_SCHEMA);
                final PreparedStatement coordinatorStatement = conn.prepareStatement(COORDINATOR_SCHEMA)
        ) {
            conn.setAutoCommit(false);
            nodeStatement.executeUpdate();
            successorStatement.executeUpdate();
            coordinatorStatement.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Counts the number of nodes in the databases
     *
     * @param conn connection to database
     * @return true if there are no rows in the node table in the database
     * @throws SQLException if something goes wrong with the DB
     */
    private boolean noNodesInDatabase(Connection conn) throws SQLException {
        try (
                final PreparedStatement nodeExistQuery = conn.prepareStatement(COUNT_NODES);
                final ResultSet rs = nodeExistQuery.executeQuery()
        ) {
            rs.next();
            return rs.getInt("nodeCount") == 0;
        }
    }

    /**
     * Inserts the nodes into the node table
     *
     * @param conn connection to the database
     * @throws IOException  if the file cannot be found/read
     * @throws SQLException if something goes wrong while inserting into the DB
     */
    private void insertNodesFromFile(Connection conn) throws IOException, SQLException {
        final Map<Integer, InetSocketAddress> idToAddress = NodeListFileParser.parseNodeFile(nodelistpath, logger);

        try (
                final PreparedStatement insertNodeQuery = conn.prepareStatement(INSERT_NODE);
        ) {
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, InetSocketAddress> node : idToAddress.entrySet()) {
                insertNodeQuery.setInt(1, node.getKey());
                insertNodeQuery.setString(2, node.getValue().getHostName());
                insertNodeQuery.setInt(3, node.getValue().getPort());
                insertNodeQuery.addBatch();
            }
            insertNodeQuery.executeBatch();
            conn.commit();
        }
    }

    @Override
    public void initialize() {
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);

            if (!schemaIsInitialized(conn)) {
                initializeSchema(conn);
            }

            if (noNodesInDatabase(conn)) {
                insertNodesFromFile(conn);
            }
        } catch (SQLException | IOException e) {
            logger.warning(e.getMessage());
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public List<NodeRow> getAllNodes() {
        final List<NodeRow> nodeRows = new ArrayList<>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);

            final String selectAll = "SELECT * FROM node LEFT JOIN coordinator ON coordinator.coordinatorId";
            ps = conn.prepareStatement(selectAll);
            rs = ps.executeQuery();

            while (rs.next()) {

                final Optional<Integer> coordinatorId = getNullableInt("coordinatorId", rs);
                final String address = rs.getString("address");
                final int port = rs.getInt("port");
                final int nodeId = rs.getInt("nodeId");
                final Integer successorId = getNullableInt("successorId", rs).orElse(null);

                final boolean isCoordinator = coordinatorId.isPresent() && coordinatorId.get() == nodeId;
                nodeRows.add(new NodeRow(address, port, nodeId, successorId, isCoordinator));

            }
        } catch (SQLException e) {
            logger.warning(e.getMessage());
        } finally {
            closeQuietly(conn);
            closeQuietly(ps);
            closeQuietly(rs);
        }

        return nodeRows;
    }

    @Override
    public void updateCoordinator(int newCoordinatorId) {

    }

    @Override
    public void setNodeSuccessor(int nodeId, int successorId) {

    }

    @Override
    public void removeNodeSuccessor(int nodeId) {

    }
}
