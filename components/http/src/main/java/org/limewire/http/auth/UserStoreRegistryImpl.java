package org.limewire.http.auth;

import java.security.Principal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.apache.http.auth.Credentials;

import com.google.inject.Singleton;
import com.google.inject.Inject;

@Singleton
public class UserStoreRegistryImpl implements UserStore, UserStoreRegistry {
    final ReadWriteLock lock;
    final Set<UserStore> userStores;
    
    @Inject
    UserStoreRegistryImpl() {
        lock = new ReentrantReadWriteLock();
        userStores = new HashSet<UserStore>();
    }
    
    public Principal authenticate(Credentials credentials) {
        lock.readLock().lock();
        try {
            for(UserStore store : userStores) {
                Principal principal = store.authenticate(credentials);
                if(principal != null) {
                    return principal;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public void addStore(UserStore userStore) {
        lock.writeLock().lock();
        try {
            userStores.add(userStore);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
