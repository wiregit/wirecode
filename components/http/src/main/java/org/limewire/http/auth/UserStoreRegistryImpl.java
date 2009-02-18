package org.limewire.http.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.auth.Credentials;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserStoreRegistryImpl implements UserStore, UserStoreRegistry {
    final ReadWriteLock lock;
    final Set<UserStore> userStores;
    
    @Inject
    public UserStoreRegistryImpl() {
        lock = new ReentrantReadWriteLock();
        userStores = new HashSet<UserStore>();
    }

    public void register(UserStoreRegistry registry) {
    }

    public void authenticate(Credentials credentials) {
        lock.readLock().lock();
        try {
            for(UserStore store : userStores) {
                store.authenticate(credentials);
            }
        } finally {
            lock.readLock().unlock();
        }
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
