package com.limegroup.gnutella.security;

import java.util.*;
import java.io.*;

import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.FileUtils;

/**
 * Maintains user's information for variety of services
 * Note: Implements Singleton Design Pattern
 * @author Anurag Singla
 */
public class Cookies implements Serializable
{
    //NOTE that the User information in a cookie always have the domains
    //authenticated, as default only
    //This is however not a problem, as only username and password are used from
    //the User info contained in a cookie
    
    /**
     * Map from hostname/IPAddressString to corresponding user authentication
     * info for the host (String -> User)
     */
    private Map /* of String -> User */ _hostUserInfoMap = new HashMap();
    
    /**
     * an instance of Cookies
     * Note: Implements Singleton Design Pattern
     */
    private static Cookies _instance = new Cookies();
    
    /**
     * Returns an instance of Cookies
     * Note: Implements Singleton Design Pattern
     */
    public static Cookies instance()
    {
        //return the initialized instance
        return _instance;
    }
    
    /**
     * Initializes the Cookies from file
     */
    private void initialize()
    {
        //load the mappings from file
        try
        {
            //read from file
            _hostUserInfoMap = readCookies();
        }
        catch(Exception e)
        {
            //if not able to read from file, create a new instance
//            e.printStackTrace();
            _hostUserInfoMap = new HashMap(); 
        }
    }
    
    /** 
     * Creates new Cookies 
     * Note: Implements Singleton Design Pattern
     */
    protected Cookies()
    {
        //initialize the map
        initialize();
    }
    
    /**
     * Reads the cookies from the file
     * @return Map from hostname to corresponding user authentication
     * info for the host (String -> User)
     */
    private Map readCookies() throws IOException, ClassNotFoundException
    {
        return FileUtils.readMap(
            SecuritySettings.COOKIES_FILE.getValue());
    }
    
    /**
     * Returns the user data corresponding to the given host name
     * @param host The name of the host for whom the corresponding information
     * is requested.
     * @return The user data corresponding to the given host
     * Returns null if no data exists for the given host
     */
    public synchronized User getUserInfo(String host)
    {
        //get the data from the map
        return (User)_hostUserInfoMap.get(host);
    }
    
    /**
     * Adds the passed user info for the given host
     * @param host The name of the host for whom to add information
     * @param user The user info to be added
     *
     */
    public synchronized void putCookie(String host, User user)
    {
        //add the cookie
        _hostUserInfoMap.put(host, user);
    }
    
    
    /**
     * Saves the cookies to file
     */
    public void save()
    {
        try
        {
            Map clone;
            synchronized(this)
            {
                clone = (Map)((HashMap)_hostUserInfoMap).clone();
            }
            //save to file
            FileUtils.writeMap(SecuritySettings.COOKIES_FILE.getValue(),
                clone);
        }
        catch(Exception e)
        {
            //nothing we can do
            //just print the exception
            e.printStackTrace();
        }
    }
    
    /**
     * returns the string representation of the contents
     * @return the string representation of the contents
     */
    public String toString()
    {
        return _hostUserInfoMap.toString();
    }
    
    //Unit Test
    public static void main(String[] args)
    {
        System.out.println(Cookies.instance().toString());
    }
    
}
