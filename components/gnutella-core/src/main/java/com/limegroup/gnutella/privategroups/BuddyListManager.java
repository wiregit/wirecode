package com.limegroup.gnutella.privategroups;

import java.net.Socket;
import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * A manager that contains a list of buddy sessions, which represents a live direct
 * connection with another user. 
 * 
 */
@Singleton
public class BuddyListManager {

    //map to store user and buddysession info
    private HashMap<String, BuddySession> buddySessionMap = new HashMap<String, BuddySession>();;
    private static BuddyListManager instance = new BuddyListManager();
    private static BuddyListManager buddyListManager;
       
    public BuddyListManager(){
    }
        
    
    public static BuddyListManager getInstance(){
        return instance;
    }
    
    @Inject
    public BuddyListManager(BuddyListManager buddyListmanager) {
     this.buddyListManager = buddyListManager;
    }
    
    // register listener on new buddy session and add to the manager
    public void addBuddySession(String name, Socket socket){
        BuddySession session = new BuddySession(socket);
        buddySessionMap.put(name, session);
    }
    
    public BuddySession getSession(String name){
        return buddySessionMap.get(name);
    }
    
    public boolean removeBuddySession(String name){
        //remove session from the list
        BuddySession buddySession = buddySessionMap.remove(name);
        
        //close sockets and chatmanagers
        return buddySession.closeSession();

    }
    
    
    
    
    
    
    
}
