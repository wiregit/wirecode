package org.limewire.http.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.auth.Credentials;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AuthenticatorRegistryImpl implements Authenticator, AuthenticatorRegistry {
    final ReadWriteLock lock;
    final Set<Authenticator> userStores;
    
    @Inject
    public AuthenticatorRegistryImpl() {
        lock = new ReentrantReadWriteLock();
        userStores = new HashSet<Authenticator>();
    }

    public void register(AuthenticatorRegistry registry) {
    }

    public boolean authenticate(Credentials credentials) {
        lock.readLock().lock();
        try {
            for(Authenticator store : userStores) {
                if(store.authenticate(credentials)) {
                    return true;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return false;
    }

    public void addAuthenticator(Authenticator userStore) {
        lock.writeLock().lock();
        try {
            userStores.add(userStore);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
