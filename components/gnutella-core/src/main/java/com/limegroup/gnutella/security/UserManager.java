/*
 * UserManager.java
 *
 * Created on October 22, 2001, 11:45 AM
 */

package com.limegroup.gnutella.security;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.xml.LimeXMLProperties;

/**
 * Manages the user authentication data
 * Note: Implements Singleton Design Pattern
 * Note: The methods in this class are synchronized, as different processes
 * may be accessing same instance. And so the process using this class 
 * doesnt have enough information to figure out when to synchronize.
 * @author Anurag Singla
 */
public class UserManager
{
    //NOTE that this class uses com.limegroup.gnutella.xml.LimeXMLProperties 
    //class for UserMap property
    //This should eventually be changed to use SettingsManager, as and when
    //the directory conventions for two property files are standardized
    
    /**
     * Mapping from username to user information. (String -> User)
     */
    private Map /* String -> User */ _userMap = new HashMap();
    
    /**
     * an instance of UserManager
     * Note: Implements Singleton Design Pattern
     */
    private static UserManager _instance = new UserManager();
    
    /**
     * Returns an instance of UserManager
     * Note: Implements Singleton Design Pattern
     */
    public static UserManager instance()
    {
        //return the initialized instance
        return _instance;
    }
    
    /**
     * Initializes the UserManager from file
     * @modifies _instance
     */
    private void initialize()
    {
        //load the userMap from file
        try
        {
            //read from file
            _userMap = readUserMap();
        }
        catch(Exception e)
        {
            //if not able to read from file, create a new instance
            _userMap = new HashMap(); 
        }
    }
    
    /** 
     * Creates new UserManager 
     * Note: Implements Singleton Design Pattern
     */
    private UserManager()
    {
        //initialize the map
        initialize();
    }
    
    /**
     * Writes the user information map to corresponding file
     * @param userMap The map to be stored
     */
    private static void writeUserMap(Map userMap)
        throws IOException, ClassNotFoundException
    {
        FileUtils.writeMap(
            LimeXMLProperties.instance().getUserMapFile(), userMap);
    }

    /**
    * Reads the user information map from the file where it is stored
    * @return The map that was read
    */
    private static Map readUserMap()
        throws IOException, ClassNotFoundException
    {
        return FileUtils.readMap(
            LimeXMLProperties.instance().getUserMapFile());
    }
    
    /**
     * Returns the user data corresponding to the given username
     * @param username Unique name identifying the user, whose information
     * is requested.
     * @return The user data corresponding to the given username.
     * Returns null if no data exists for the given user (i.e. UserManager
     * doesnt know about this user)
     */
    public synchronized User getUser(String username)
    {
        //get the data from the map
        return (User)_userMap.get(username);
    }
    
    /**
     * Adds the passed user to the list of users. If a user exists earlier 
     * with the same username as the passed user, its old information
     * is replaced by the new data passed.
     * @param user The user to be added
     *
     */
    public synchronized void putUser(User user)
    {
        //add the user to the map
        _userMap.put(user.getUsername(),user);
    }
    
    /**
     * Returns all the users
     * @return all the users. 
     */
    public synchronized String[] getAllUsers()
    {
        
        return (String[])_userMap.keySet().toArray(new String[0]);
        
    }
    
    /**
     * Removes the user data corresponding to the given username
     * @param username Unique name identifying the user, who has to 
     * be deleted.
     *
     */
    public synchronized void removeUser(String username)
    {
        //remove the data from the map
        _userMap.remove(username);
    }
    
    /**
     * Saves the user map to file
     */
    public void save()
    {
        try
        {
            Map clone;
            synchronized(this)
            {
                clone = (Map)((HashMap)_userMap).clone();
            }
            //save to file
            writeUserMap(clone);
        }
        catch(Exception e)
        {
            //nothing we can do
        }
    }
    
    /**
     * returns the string representation of the contents
     * @return the string representation of the contents
     */
    public synchronized String toString()
    {
        return _userMap.toString();
    }
    
    //Unit Test
    public static void main(String[] args)
    {
        System.out.println(UserManager.instance().toString());
    }
    
}
