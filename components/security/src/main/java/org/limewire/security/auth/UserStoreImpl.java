package org.limewire.security.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.auth.Credentials;
import org.limewire.http.auth.UserStoreRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserStoreImpl implements UserStore, org.limewire.http.auth.UserStore{
    private final Map<String, String> users;

    @Inject
    UserStoreImpl() {
        users = new ConcurrentHashMap<String, String>();
    }

    @Inject
    public void register(UserStoreRegistry registry) {
        registry.addStore(this);
    }

    public String getPassword(String user) {
        return users.get(user);
    }

    public void addUser(String user, String password) {
        users.put(user, password);
    }

    public void authenticate(String username, String password) {
        String pw = users.get(username);
        if(pw == null || !pw.equals(password)) {
            throw new RuntimeException(); // TODO checked exception OR return false
        }
    }

    public void authenticate(Credentials credentials) {
        authenticate(credentials.getUserPrincipal().getName(), credentials.getPassword());
    }
}
