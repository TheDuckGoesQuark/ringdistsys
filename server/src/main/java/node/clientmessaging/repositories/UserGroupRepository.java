package node.clientmessaging.repositories;

import java.io.IOException;
import java.util.Set;

/**
 * Manages user registration and group membership
 */
public interface UserGroupRepository {

    /**
     * Adds a user to the database
     *
     * @param username user to add
     * @throws IOException if unable to add to database
     */
    void addUser(String username) throws IOException;

    /**
     * Removes user from the database
     *
     * @param username user to remove
     * @throws IOException if unable to remove from database
     */
    void removeUser(String username) throws IOException;

    /**
     * Adds a user to a group. If the group does not exist, it will be created.
     *
     * @param username  user to add to group
     * @param groupname group to add user to
     * @throws IOException if unable to add user or create group
     */
    void addUserToGroup(String username, String groupname) throws IOException;

    /**
     * Removes user from a group.
     *
     * @param username  user to remove from group
     * @param groupname group to remove user from
     * @throws IOException if unable to remove user from group
     */
    void removeUserFromGroup(String username, String groupname) throws IOException;

    /**
     * Retrieves the list of users that belong to the group with the given name
     *
     * @param groupname name of group to get all users for
     * @return list of users that belong to requested group
     * @throws IOException if unable to fetch list of users
     */
    Set<String> getAllUsersInGroup(String groupname) throws IOException;
}
