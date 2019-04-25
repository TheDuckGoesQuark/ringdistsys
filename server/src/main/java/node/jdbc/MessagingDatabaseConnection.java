package node.jdbc;

import logging.LoggerFactory;
import node.clientmessaging.messages.ChatMessage;
import node.clientmessaging.repositories.MessageRepository;
import node.clientmessaging.repositories.UserGroupRepository;

import java.io.IOException;
import java.sql.*;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class MessagingDatabaseConnection implements UserGroupRepository, MessageRepository {

    private static final String CONNECTION_STRING = "jdbc:mariadb://jm354.host.cs.st-andrews.ac.uk/jm354_distsys";
    private static final String USERNAME = "jm354";
    private static final String PASSOWRD = "722Em!9LLknjhZ";

    private static final String CLIENT_TABLE_NAME = "clients";
    private static final String GROUP_TABLE_NAME = "groups";
    private static final String PART_OF_GROUP_TABLE_NAME = "partofgroup";

    private static final String MESSAGE_TABLE_NAME = "messages";
    private static final String MESSAGE_DESTINATIONS_NAME = "messagedestinations";

    private static final String CLIENT_SCHEMA =
            "CREATE TABLE " + CLIENT_TABLE_NAME + " ( " +
                    "username VARCHAR(255)," +
                    "PRIMARY KEY (username)" +
                    ")";

    private static final String GROUP_SCHEMA =
            "CREATE TABLE " + GROUP_TABLE_NAME + " ( " +
                    "groupname VARCHAR(255)," +
                    "PRIMARY KEY (groupname)" +
                    ")";

    private static final String PART_OF_GROUP_SCHEMA =
            "CREATE TABLE " + PART_OF_GROUP_TABLE_NAME + " ( " +
                    "username VARCHAR(255)," +
                    "groupname VARCHAR(255)," +
                    "PRIMARY KEY (username, groupname), " +
                    "FOREIGN KEY (username) REFERENCES " + CLIENT_TABLE_NAME + "(username)" +
                    "ON DELETE CASCADE," +
                    "FOREIGN KEY (groupname) REFERENCES " + GROUP_TABLE_NAME + "(groupname)" +
                    "ON DELETE CASCADE" +
                    ")";

    private static final String MESSAGE_SCHEMA =
            "CREATE TABLE " + MESSAGE_TABLE_NAME + " ( " +
                    "messageId INT AUTO_INCREMENT," +
                    "sentAt INT," +
                    "contents VARCHAR(255)," +
                    "fromUsername VARCHAR(255)," +
                    "PRIMARY KEY (messageId)," +
                    "FOREIGN KEY (fromUsername) REFERENCES " + CLIENT_TABLE_NAME + "(username)" +
                    ")";

    private static final String DESTINATION_SCHEMA =
            "CREATE TABLE " + MESSAGE_DESTINATIONS_NAME + " ( " +
                    "messageId INT," +
                    "toUsername VARCHAR(255) NOT NULL," +
                    "PRIMARY KEY (messageId, toUsername)," +
                    "FOREIGN KEY (messageId) REFERENCES " + MESSAGE_SCHEMA + "(messageId)," +
                    "FOREIGN KEY (fromUsername) REFERENCES " + CLIENT_TABLE_NAME + "(username) " +
                    "ON DELETE CASCADE" +
                    ")";

    private static final String DROP_TABLE = "DROP TABLE ";

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

        try (ResultSet rs = meta.getTables(null, null, CLIENT_TABLE_NAME, null)) {
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
                final PreparedStatement clientSchema = conn.prepareStatement(CLIENT_SCHEMA);
                final PreparedStatement groupSchema = conn.prepareStatement(GROUP_SCHEMA);
                final PreparedStatement partOfGroupSchema = conn.prepareStatement(PART_OF_GROUP_SCHEMA);
                final PreparedStatement messageSchema = conn.prepareStatement(MESSAGE_SCHEMA);
                final PreparedStatement messageDestSchema = conn.prepareStatement(DESTINATION_SCHEMA)
        ) {
            conn.setAutoCommit(false);
            clientSchema.executeUpdate();
            groupSchema.executeUpdate();
            partOfGroupSchema.executeUpdate();
            messageSchema.executeUpdate();
            messageDestSchema.executeUpdate();
            conn.commit();
        }
    }

    /**
     * Removes all tables from the database
     */
    private void dropEverything(Connection conn) throws SQLException {
        try (
                final PreparedStatement dropClientSchema = conn.prepareStatement(DROP_TABLE + CLIENT_TABLE_NAME);
                final PreparedStatement dropGroupSchema = conn.prepareStatement(DROP_TABLE + GROUP_TABLE_NAME);
                final PreparedStatement dropPartOfGroupSchema = conn.prepareStatement(DROP_TABLE + PART_OF_GROUP_TABLE_NAME);
                final PreparedStatement dropMessageSchema = conn.prepareStatement(DROP_TABLE + MESSAGE_TABLE_NAME);
                final PreparedStatement dropMessageDestSchema = conn.prepareStatement(DROP_TABLE + MESSAGE_DESTINATIONS_NAME)
        ) {
            conn.setAutoCommit(false);
            dropMessageDestSchema.executeUpdate();
            dropMessageSchema.executeUpdate();
            dropPartOfGroupSchema.executeUpdate();
            dropGroupSchema.executeUpdate();
            dropClientSchema.executeUpdate();
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
    public Optional<ChatMessage> getNextMessageForUser(Set<String> usernames) {
        return Optional.empty();
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
