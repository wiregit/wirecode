package com.limegroup.gnutella.security;
import java.io.Serializable;
import com.sun.java.util.collections.Set;
import com.sun.java.util.collections.HashSet;

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
     * The set of domains to which the user belongs
     */
    private Set /* of String */ _domains;
    
    private static final String DEFAULT_DOMAIN = "DEFAULT";
    
    /**
     * creates a new user
     * @param username unique name for the user
     * @param password User's password to be used for authentication
     */
    public User(String username, String password)
    {
        //delegate to the other constructor, and pass default domain set
        //as an additional parameter
        this(username, password, createDefaultDomainSet());
    }
    
    /**
     * creates a new user
     * @param username Unique name for the user
     * @param password User's password to be used for authentication
     * @param domains The set of domains to which the user belongs
     */
    public User(String username, String password, Set domains)
    {
        //initialize with parameters
        this._username = username;
        this._password = password;
        this._domains = domains;
    }
    
    /**
     * creates a default domain set, that contains defualt domain
     * @return Set (of String) of default domains
     */
    public static Set createDefaultDomainSet(){
        Set domainSet = new HashSet();
        //add the default domain to the list of domains
        domainSet.add(DEFAULT_DOMAIN);
        return domainSet;
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
     * Adds the passed domain to the set of domains user belongs to
     * @param domain The domain to be added
     */
    public void addDomain(String domain)
    {
        _domains.add(domain);
    }
    
    /**
     * Removes the passed domain from the set of domains user belongs to
     * @param domain The domain to be removed
     */
    public void removeDomain(String domain)
    {
        _domains.remove(domain);
    }
    
    /**
     * Returns the set of domains to which the user belongs
     * @return the set of domains to which the user belongs
     */
    public Set getDomains()
    {
        return _domains;
    }
    
    /**
     * returns the string representation of the contents
     * @return the string representation of the contents
     */
    public String toString()
    {
        return "username=" + _username + "&password=" + _password 
            + "&domains=" + _domains;
    }
    
}
