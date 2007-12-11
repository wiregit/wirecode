package com.limegroup.gnutella.privategroups;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;

public interface PGRPClient {
    
    /**
     * returns the buddy list manager associated with this client
     */
    public BuddyListManager getBuddyListManager();
    
    /**
     * create a new user account with the server.  This method should only be used by the administrator
     */
    public boolean createAccount(String username, String password);
    
    /**
     * removes a user account with the server.  This method should only be used by the administrator
     */
    public boolean removeAccount() throws XMPPException;
    
    /**
     * logs in a user with the given username and password 
     */
    public boolean loginAccount(String username, String password);
    
    /**
     * test method for logging in a user with the given username and password without a server socket being started
     */
    public boolean loginAccountNoServerSocket(String username, String password);
    
    /**
     * logs off current client from the server
     */
    public boolean logoff();
    
    /**
     * sends a message to a remote user specified with the given username and message
     */
    public boolean sendMessage(String username, String message);
    
    
    /**
     * gets ip address of remote user and establishes new chat manager
     */
    public boolean setRemoteConnection(String remoteUserNameServer, String localUsername);
    
    /**
     * adds a user with the specified username, nickname, and groupname to the roster
     */
    public boolean addToRoster(String username, String nickName, String groupName);
    
    /**
     * removes a user with the specified username, nickname, and groupname from the roster
     */
    public boolean removeFromRoster(String username, String groupName);
    
    /**
     * finds a user in the roster. returns true if found and false if not.
     */
    public boolean findRosterUserName(String username);
    
    /**
     * gets the current user's roster.
     */
    public Roster getRoster();
    
    /**
     * returns the name of the server that has openfire
     */        
    public String getServername();
    
}
