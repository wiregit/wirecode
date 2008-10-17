package org.limewire.security.auth;

public interface UserStore {
    String getPassword(String user);
    void addUser(String user, String password);
    boolean authenticate(String username, String password);
}
