package node.clientmessaging;

import java.util.Objects;
import java.util.Set;

public class User {

    /**
     * Username
     */
    private String username;

    /**
     * The names of the groups that this user belongs to
     */
    private Set<String> groups;

    public User(String username, Set<String> groups) {
        this.username = username;
        this.groups = groups;
    }

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void addToGroup(String groupname) {
        groups.add(groupname);
    }

    public void removeFromGroup(String groupname) {
        groups.remove(groupname);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
