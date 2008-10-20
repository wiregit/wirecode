package org.limewire.security.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserStoreImpl implements UserStore {
    private final Map<String, String> users;

    @Inject
    UserStoreImpl() {
        users = new ConcurrentHashMap<String, String>();
    }

    public String getPassword(String user) {
        return users.get(user);
    }

    public void addUser(String user, String password) {
        users.put(user, password);
    }

    public boolean authenticate(String username, String password) {
        String pw = users.get(username);
        if(pw == null || !pw.equals(password)) {
            return false;
        } else {
            return true;
        }
    }
}
