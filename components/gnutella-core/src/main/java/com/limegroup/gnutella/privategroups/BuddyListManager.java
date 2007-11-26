package com.limegroup.gnutella.privategroups;

import java.net.Socket;
import java.util.HashMap;

import com.google.inject.Singleton;

@Singleton
public class BuddyListManager {

    //map to store user and buddysession info
    private HashMap<String, BuddySession> buddySessionMap = new HashMap<String, BuddySession>();;
    private static BuddyListManager instance = new BuddyListManager();
    
    public BuddyListManager(){    
    }
    
    public static BuddyListManager getInstance(){
        return instance;
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
