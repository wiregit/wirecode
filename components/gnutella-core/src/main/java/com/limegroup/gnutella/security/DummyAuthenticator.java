package com.limegroup.gnutella.security;

/**
 * A dummy authenticator. Does nothing
 * @author Anurag Singla
 */
public class DummyAuthenticator implements Authenticator
{
    /**
     * Always return true
     * @param username the "user" to be authenticated.
     * @param password the password of the user.
     * @param domain The domain for which to authenticate user
     * @return Always returns true
     */
    public boolean authenticate(String username, String passwd, String domain)
    {
        return true;
    }
}
