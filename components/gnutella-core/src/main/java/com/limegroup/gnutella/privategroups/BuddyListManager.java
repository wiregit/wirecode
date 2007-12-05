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

    //map to store user and ChatManager info
    private HashMap<String, ChatManager> buddyMap = new HashMap<String, ChatManager>();
    private static BuddyListManager buddyListManager;
            
    public void addChatManager(String remoteUsername, String localUsername, Socket socket){
        ChatManager manager = new ChatManager(localUsername, socket);
        buddyMap.put(remoteUsername, manager);
    }
    
    public ChatManager getManager(String name){
        return buddyMap.get(name);
    }
    
    public boolean removeChatManager(String name){
        //remove session from the list
        ChatManager manager = buddyMap.remove(name);
        
        //close sockets and chatmanagers
        return manager.closeChatManager();
    }
    
    
    
    
    
    
    
}
