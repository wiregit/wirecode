package com.limegroup.gnutella.security;

import java.util.Set;

/**
 * Authenticates a user for access to resources.
 */
public class ServerAuthenticator implements Authenticator
{
    /**
     * Reference to user manager
     */
    private UserManager _userManager = UserManager.instance();
    
    /**
     * Constructs a new Authenticator
     */
    public ServerAuthenticator()
    {
    }
    
    //inherit doc comments
    public Set authenticate(String username, String passwd, String domain)
    {
        //get the corresponding user information
        User user = _userManager.getUser(username);

        //if null, returned then the user doesn't exist.
        if (user == null)
            return null;

        //now, verify the password.  Note that the password passed
        //should be in the same format as in the UserManger. 
        if (passwd.equals(user.getPassword()))
            return user.getDomains();
        else
            return null;
    }
}







