package node.jdbc;

import logging.LoggerFactory;
import node.clientmessaging.messages.ChatMessage;
import node.clientmessaging.repositories.MessageRepository;
import node.clientmessaging.repositories.UserGroupRepository;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
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
                    "toGroup VARCHAR(255)," +
                    "PRIMARY KEY (messageId)," +
                    "FOREIGN KEY (fromUsername) REFERENCES " + CLIENT_TABLE_NAME + "(username)," +
                    "FOREIGN KEY (toGroup) REFERENCES " + GROUP_TABLE_NAME + "(groupname)" +
                    ")";

    private static final String DESTINATION_SCHEMA =
            "CREATE TABLE " + MESSAGE_DESTINATIONS_NAME + " ( " +
                    "messageId INT," +
                    "toUsername VARCHAR(255) NOT NULL," +
                    "PRIMARY KEY (messageId, toUsername)," +
                    "FOREIGN KEY (messageId) REFERENCES " + MESSAGE_TABLE_NAME + "(messageId)," +
                    "FOREIGN KEY (toUsername) REFERENCES " + CLIENT_TABLE_NAME + "(username) " +
                    "ON DELETE CASCADE" +
                    ")";

    private static final String DROP_TABLE_PREFIX = "DROP TABLE IF EXISTS ";

    private static final String INSERT_MESSAGE = "INSERT INTO " + MESSAGE_TABLE_NAME +
            " (sentAt, contents, fromUsername, toGroup) " +
            "VALUES (?, ?, ?, ?)";

    private static final String INSERT_DESTINATION = "INSERT INTO " + MESSAGE_DESTINATIONS_NAME +
            " (messageId, toUsername)" +
            " VALUES (?, ?)";

    private static final String GET_MESSAGES_FOR_TWO_USERS_LIMIT_ONE_OLDEST_FIRST =
            "SELECT m.*, d.toUsername FROM " + MESSAGE_TABLE_NAME + " m " +
                    "INNER JOIN " + MESSAGE_DESTINATIONS_NAME + " d ON m.messageId = d.messageId " +
                    "WHERE toUsername IN (?, ?) ORDER BY sentAt " +
                    "LIMIT 1";

    private static final String DELETE_MESSAGES_WITHOUT_RECIPIENTS =
            "DELETE FROM " + MESSAGE_TABLE_NAME + " WHERE messageId IN " +
                    "(" +
                    "SELECT m.messageId FROM " + MESSAGE_TABLE_NAME + " m " +
                    "LEFT JOIN " + MESSAGE_DESTINATIONS_NAME + " d ON m.messageId = d.messageId " +
                    "WHERE d.messageId IS NULL " +
                    "GROUP BY m.messageId" +
                    ")";

    private static final String DELETE_RECIPIENT =
            "DELETE FROM " + MESSAGE_DESTINATIONS_NAME + " d WHERE d.messageId = ? AND d.toUsername = ?";

    private static final String INSERT_USER =
            "INSERT INTO " + CLIENT_TABLE_NAME + " VALUES (?)";

    private static final String DELETE_USER =
            "DELETE FROM " + CLIENT_TABLE_NAME + " WHERE username = ?";

    private static final String INSERT_GROUP_IF_NOT_EXIST =
            "INSERT IGNORE INTO " + GROUP_TABLE_NAME + " VALUES (?)";

    private static final String INSERT_USER_INTO_GROUP =
            "INSERT INTO " + PART_OF_GROUP_TABLE_NAME + " VALUES (?, ?)";

    private static final String REMOVE_USER_FROM_GROUP =
            "DELETE FROM " + PART_OF_GROUP_TABLE_NAME + " WHERE username = ? AND groupname = ?";

    private static final String GET_ALL_USERS_IN_GROUP =
            "SELECT username FROM " + PART_OF_GROUP_TABLE_NAME + " WHERE groupname = ?";

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
                final PreparedStatement dropClientSchema = conn.prepareStatement(DROP_TABLE_PREFIX + CLIENT_TABLE_NAME);
                final PreparedStatement dropGroupSchema = conn.prepareStatement(DROP_TABLE_PREFIX + GROUP_TABLE_NAME);
                final PreparedStatement dropPartOfGroupSchema = conn.prepareStatement(DROP_TABLE_PREFIX + PART_OF_GROUP_TABLE_NAME);
                final PreparedStatement dropMessageSchema = conn.prepareStatement(DROP_TABLE_PREFIX + MESSAGE_TABLE_NAME);
                final PreparedStatement dropMessageDestSchema = conn.prepareStatement(DROP_TABLE_PREFIX + MESSAGE_DESTINATIONS_NAME)
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

    /**
     * Inserts message into database for all recipients
     */
    private void insertMessageForAllRecipients(final Message message, Set<String> recipients, final Connection conn) throws IOException {
        try (
                final PreparedStatement insertMessage = conn.prepareStatement(INSERT_MESSAGE, Statement.RETURN_GENERATED_KEYS);
                final PreparedStatement insertDestination = conn.prepareStatement(INSERT_DESTINATION)
        ) {
            conn.setAutoCommit(true);
            insertMessage.setLong(1, message.getSentAt());
            insertMessage.setString(2, message.getContents());
            insertMessage.setString(3, message.getFromUsername());
            insertMessage.setString(4, message.getToGroup());

            final int affectedRows = insertMessage.executeUpdate();

            if (affectedRows == 0) {
                throw new IOException("Message could not be inserted.");
            }

            final int messageId;
            try (ResultSet generatedKeys = insertMessage.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    messageId = generatedKeys.getInt(1);
                } else {
                    throw new IOException("Message could not be inserted.");
                }
            }

            conn.setAutoCommit(false);
            for (String recipient : recipients) {
                insertDestination.setInt(1, messageId);
                insertDestination.setString(2, recipient);
                insertDestination.addBatch();
            }

            insertDestination.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void sendMessage(ChatMessage chatMessage) throws IOException {
        final Set<String> recipients;
        final Message message;
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
        ) {
            if (chatMessage.forGroup()) {
                final String toGroup = chatMessage.getToGroup().get();
                recipients = getAllUsersInGroup(toGroup, conn);
                message = new Message(
                        chatMessage.getSentAt().toEpochMilli(),
                        chatMessage.getMessageContent(),
                        chatMessage.getFromName(),
                        toGroup
                );
            } else {
                recipients = Collections.singleton(chatMessage.getToUsername());
                message = new Message(
                        chatMessage.getSentAt().toEpochMilli(),
                        chatMessage.getMessageContent(),
                        chatMessage.getFromName()
                );
            }

            insertMessageForAllRecipients(message, recipients, conn);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Removes the given user as a recipient of the given message.
     * If there are no more recipients then the message is deleted entirely
     *
     * @param messageId  id of message
     * @param toUsername recipient of message to remove
     * @param conn       connection to database
     */
    private void removeMessage(int messageId, String toUsername, Connection conn) throws SQLException {
        try (
                final PreparedStatement deleteRecipient = conn.prepareStatement(DELETE_RECIPIENT);
                final PreparedStatement deleteUnreferencedMessages = conn.prepareStatement(DELETE_MESSAGES_WITHOUT_RECIPIENTS);
        ) {
            conn.setAutoCommit(false);
            // Delete as recipient
            deleteRecipient.setInt(1, messageId);
            deleteRecipient.setString(2, toUsername);
            deleteRecipient.executeUpdate();

            // Delete messages with no recipients left
            deleteUnreferencedMessages.executeUpdate();

            // Commit
            conn.commit();
        }
    }

    @Override
    public Optional<ChatMessage> getNextMessageForUser(Set<String> usernames) throws IOException {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement getNextMessageForUsers = conn.prepareStatement(GET_MESSAGES_FOR_TWO_USERS_LIMIT_ONE_OLDEST_FIRST);
        ) {
            // Add clients to IN clause
            final Iterator<String> recipientIter = usernames.iterator();
            for (int i = 0; i < 2; i++) {
                if (recipientIter.hasNext()) {
                    getNextMessageForUsers.setString(i + 1, recipientIter.next());
                } else {
                    getNextMessageForUsers.setString(i + 1, null);
                }
            }

            // Query
            final ResultSet rs = getNextMessageForUsers.executeQuery();

            if (!rs.next()) {
                return Optional.empty();
            }

            // Construct message
            final Instant sentAt = Instant.ofEpochMilli(rs.getLong("sentAt"));
            final String fromName = rs.getString("fromUsername");
            final String toUsername = rs.getString("toUsername");
            final String toGroup = rs.getString("toGroup");
            final String messageContent = rs.getString("contents");
            final int messageId = rs.getInt("messageId");

            removeMessage(messageId, toUsername, conn);

            return Optional.of(new ChatMessage(sentAt, fromName, toUsername, toGroup, messageContent));

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void addUser(String username) throws IOException {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement insertUser = conn.prepareStatement(INSERT_USER)
        ) {
            conn.setAutoCommit(true);
            insertUser.setString(1, username);
            insertUser.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void removeUser(String username) throws IOException {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement deleteUser = conn.prepareStatement(DELETE_USER)
        ) {
            conn.setAutoCommit(true);
            deleteUser.setString(1, username);
            deleteUser.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void addUserToGroup(String username, String groupname) throws IOException {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement insertGroupIfNotExist = conn.prepareStatement(INSERT_GROUP_IF_NOT_EXIST);
                final PreparedStatement insertUserIntoGroup = conn.prepareStatement(INSERT_USER_INTO_GROUP)
        ) {
            conn.setAutoCommit(true);

            insertGroupIfNotExist.setString(1, groupname);
            insertGroupIfNotExist.executeUpdate();

            insertUserIntoGroup.setString(1, username);
            insertUserIntoGroup.setString(2, groupname);
            insertUserIntoGroup.executeUpdate();

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void removeUserFromGroup(String username, String groupname) throws IOException {
        try (
                final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD);
                final PreparedStatement removeUserFromGroup = conn.prepareStatement(REMOVE_USER_FROM_GROUP)
        ) {
            conn.setAutoCommit(true);
            removeUserFromGroup.setString(1, username);
            removeUserFromGroup.setString(2, groupname);
            removeUserFromGroup.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Set<String> getAllUsersInGroup(String groupname) throws IOException {
        try (final Connection conn = DriverManager.getConnection(CONNECTION_STRING, USERNAME, PASSOWRD)) {
            return getAllUsersInGroup(groupname, conn);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Get all users when connection is already created
     *
     * @param groupname group to fetch users for
     * @param conn      connection to database
     * @return set of users that are part of group
     * @throws IOException
     */
    private Set<String> getAllUsersInGroup(String groupname, Connection conn) throws IOException {
        try (final PreparedStatement getUsersInGroup = conn.prepareStatement(GET_ALL_USERS_IN_GROUP)) {
            getUsersInGroup.setString(1, groupname);
            final ResultSet rs = getUsersInGroup.executeQuery();
            final Set<String> users = new HashSet<>();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            return users;

        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }
}
