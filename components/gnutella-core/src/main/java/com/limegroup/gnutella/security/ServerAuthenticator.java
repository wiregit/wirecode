package com.limegroup.gnutella.security;

import java.security.*;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.security.Authenticator;

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
    
    /**
     * Converts a 16-byte MD5 hash into a 32 byte hex string.
     */
    private static String generateHexString(byte[] hash)
    {
        StringBuffer sb = new StringBuffer();
        
        for(int i = 0; i < hash.length; i++)
        {
            if ( (((int)hash[i] & 0xff)) < 0x10)
                sb.append("0");
            sb.append(Long.toString((int)hash[i] & 0xff, 16));
        }
        
        return sb.toString();
    }

}







