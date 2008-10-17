package org.limewire.security.auth;

public interface UserStore {
    String getPassword(String user);
    void addUser(String user, String password);
    void authenticate(String username, String password);
}
