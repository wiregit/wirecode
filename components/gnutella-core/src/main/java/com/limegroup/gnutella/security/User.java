package com.limegroup.gnutella.security;
import java.io.Serializable;

/**
 * Contains authentication information for a user
 * @author Anurag Singla
 */
public class User implements Serializable
{
    /**
     * For compatibility between different versions of this class
     */
     static final long serialVersionUID = -3608126644033971628L;
    
    /**
     * Unique User name
     */
    private String _username;
    
    /**
     * password for the user, to be used for authentication
     */
    private String _password;
    
    /**
     * creates a new user
     * @param username unique name for the user
     * @param password User's password to be used for authentication
     */
    public User(String username, String password)
    {
        this._username = username;
        this._password = password;
    }
    
    /**
     * Returns the username for the user
     * @return the username for the user
     */
    public String getUsername()
    {
        return _username;
    }
    
    /**
     * Returns the password for the user
     * @return the password for the user
     */
    public String getPassword()
    {
        return _password;
    }
    
    /**
     * returns the string representation of the contents
     * @return the string representation of the contents
     */
    public String toString()
    {
        return "username=" + _username + "&password=" + _password;
    }
    
}
