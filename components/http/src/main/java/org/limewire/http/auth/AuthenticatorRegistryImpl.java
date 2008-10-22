package org.limewire.http.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.auth.Credentials;

import com.google.inject.Singleton;

@Singleton
public class AuthenticatorRegistryImpl implements Authenticator, AuthenticatorRegistry {
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<Authenticator> authenticators = new HashSet<Authenticator>();
    
    public void register(AuthenticatorRegistry registry) {
    }

    public boolean authenticate(Credentials credentials) {
        lock.readLock().lock();
        try {
            for(Authenticator store : authenticators) {
                if(store.authenticate(credentials)) {
                    return true;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return false;
    }

    public void addAuthenticator(Authenticator authenticator) {
        lock.writeLock().lock();
        try {
            authenticators.add(authenticator);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
