package com.limegroup.gnutella.security;

/**
 * An interface for authentication purposes.
 * @author Anurag Singla
 * @version 
 */
public interface Authenticator
{
    /**
     * Authenticates the username and password passed for the given domain
     * @param username the "user" to be authenticated.
     * @param password the password of the user.
     * @param domain The domain for which to authenticate user
     * @return true if the user is recognized as an authenticated user,
     * false otherwise.
     */
    public boolean authenticate(String username, String passwd, String domain);
}

