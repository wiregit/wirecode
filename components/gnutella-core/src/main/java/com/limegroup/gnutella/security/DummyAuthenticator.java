package com.limegroup.gnutella.security;
import java.util.Set;
import java.util.HashSet;

/**
 * A dummy authenticator. Does nothing
 * @author Anurag Singla
 */
public class DummyAuthenticator implements Authenticator
{
    /**
     * Always return a set with default domain
     * @param username the "user" to be authenticated.
     * @param password the password of the user.
     * @param domain The domain for which to authenticate user
     * @return the set (of Strings) consisting of just the default domain
     */
    public Set authenticate(String username, String passwd, String domain)
    {
        return User.createDefaultDomainSet();
    }
}
