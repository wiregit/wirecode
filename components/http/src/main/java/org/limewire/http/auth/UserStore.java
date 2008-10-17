package org.limewire.http.auth;

import org.apache.http.auth.Credentials;

public interface UserStore {
    void register(UserStoreRegistry registry);
    //Credentials getCredentials(Principal principal);
    //void addUser(Credentials credentials);
    boolean authenticate(Credentials credentials);
}
