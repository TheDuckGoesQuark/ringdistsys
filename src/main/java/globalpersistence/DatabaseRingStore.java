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
    private static final String COORDINATOR_TABLE_NAME = "coordinators";

    private static final String NODE_SCHEMA =
            "CREATE TABLE " + NODE_TABLE_NAME + " ( " +
                    "nodeId INT NOT NULL," +
                    "address VARCHAR(255) NOT NULL," +
                    "port INT NOT NULL," +
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

    private static final String COUNT_NODES =
            "SELECT count(*) as nodeCount FROM " + NODE_TABLE_NAME;

    private static final String INSERT_NODE =
            "INSERT INTO " + NODE_TABLE_NAME + " VALUES (?, ?, ?, NULL)";

    private static final String INSERT_SUCCESSOR =
            "UPDATE " + NODE_TABLE_NAME + " SET successorId = ? where nodeId = ?";

    private static final String REMOVE_SUCCESSOR =
            "UPDATE " + NODE_TABLE_NAME + " SET successorId = NULL where nodeId = ?";

    private static final String INSERT_COORDINATOR =
            "INSERT INTO " + COORDINATOR_TABLE_NAME + " VALUES (?, True) ON DUPLICATE KEY UPDATE coordinatorId = ?";

    private final Logger logger = LoggerFactory.getLogger();
    private final String nodelistpath;
    private final boolean doFullRestart;

    /**
     * Creates a database ring store instance with the path to the file it can use to initialize the database.
     *
     * @param nodelistpath path to file with node ids and socket addresses
     */
    public DatabaseRingStore(String nodelistpath, boolean doFullRestart) {
        this.nodelistpath = nodelistpath;
        this.doFullRestart = doFullRestart;
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
                final PreparedStatement coordinatorStatement = conn.prepareStatement(COORDINATOR_SCHEMA)
        ) {
            conn.setAutoCommit(false);
            nodeStatement.executeUpdate();
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

    @Override
    public void initialize() {
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
            if (doFullRestart) {
                dropEverything(conn);
            }

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

            ps = conn.prepareStatement(SELECT_ALL);
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
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement ps = conn.prepareStatement(INSERT_COORDINATOR)
        ) {

            conn.setAutoCommit(true);
            ps.setInt(1, newCoordinatorId); // Set insert
            ps.setInt(2, newCoordinatorId); // Set on duplicate insert
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }
    }

    @Override
    public void setNodeSuccessor(int nodeId, int successorId) {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement ps = conn.prepareStatement(INSERT_SUCCESSOR)
        ) {

            conn.setAutoCommit(true);
            ps.setInt(1, successorId); // set successor
            ps.setInt(2, nodeId); // For this node
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }
    }

    @Override
    public void removeNodeSuccessor(int nodeId) {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement ps = conn.prepareStatement(REMOVE_SUCCESSOR)
        ) {

            conn.setAutoCommit(true);
            ps.setInt(1, nodeId);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }

    }

    @Override
    public List<NodeRow> getAllNodesWithSuccessors() {
        final List<NodeRow> nodeRows = new ArrayList<>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);

            ps = conn.prepareStatement(SELECT_ALL_WITH_SUCCESSOR);
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
    public void insertIntoRing(int predecessorId, int successorId, int newNodeId) {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement ps = conn.prepareStatement(INSERT_SUCCESSOR)
        ) {

            conn.setAutoCommit(false);
            ps.setInt(1, successorId); // set successor
            ps.setInt(2, newNodeId); // For this node
            ps.addBatch();
            ps.setInt(1, newNodeId); // set successor
            ps.setInt(2, predecessorId); // For this node
            ps.addBatch();

            ps.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }

    }

    @Override
    public void removeFromRing(int predecessorId, int successorId, int nodeToRemove) {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement removeStatement = conn.prepareStatement(REMOVE_SUCCESSOR);
                final PreparedStatement insertStatement = conn.prepareStatement(INSERT_SUCCESSOR)
        ) {

            conn.setAutoCommit(false);
            insertStatement.setInt(1, successorId); // set successor
            insertStatement.setInt(2, predecessorId); // For this node

            removeStatement.setInt(1, nodeToRemove); // Unset successor for this node

            insertStatement.executeUpdate();
            removeStatement.executeUpdate();
            conn.commit();

        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }
    }
}
