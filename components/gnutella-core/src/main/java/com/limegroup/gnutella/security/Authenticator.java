package com.limegroup.gnutella.security;
import java.util.Set;
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
     * @return the set (of Strings) of domains to which user is successfully
     * authenticated. Returns null, if the authentication failed.
     */
    public Set authenticate(String username, String passwd, String domain);
}

