package com.limegroup.gnutella.privategroups;

import java.net.Socket;
import java.util.HashMap;

import com.google.inject.Singleton;
import com.limegroup.gnutella.gui.privategroups.RosterListMediator;


/**
 * A manager that contains a list of buddy sessions, which represents a live direct
 * connection with another user. 
 * 
 */
@Singleton
public class BuddyListManager {

    //map to store user and ChatManager info
    private HashMap<String, ChatManager> buddyMap = new HashMap<String, ChatManager>();
            
    public void addChatManager(String remoteUsername, String localUsername, Socket socket){
	    ChatManager manager = buddyMap.get(remoteUsername);
	    if(manager ==null){
	        manager = new ChatManager(socket);
	        buddyMap.put(remoteUsername, manager);
	        RosterListMediator.getInstance().initMessageWindow("felix@lw-intern02", localUsername);
	    }
	    else{
	    	//replace old socket with new ones
	    	manager.replaceSocket(socket);
	    	
	    }
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
