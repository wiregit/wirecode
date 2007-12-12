package com.limegroup.gnutella.privategroups;

import java.net.Socket;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final Log LOG = LogFactory.getLog(BuddyListManager.class);
            
    public void addChatManager(String remoteUsername, String localUsername, Socket socket){
        LOG.debug("BuddyListManager:addChatManager");
	    ChatManager manager = buddyMap.get(remoteUsername);
	    if(manager ==null){
	        LOG.debug("chatManager did not exist before.  create a new one");
	        manager = new ChatManager(socket, remoteUsername);
	        buddyMap.put(remoteUsername, manager);
	        System.out.println(" ADDED NEW CHATMANAGER: key is " + remoteUsername);
	        LOG.debug("chatManager is now in the Map. start a new message window");
	        RosterListMediator.getInstance().initMessageWindow(remoteUsername, localUsername);
	    }
	    else{
	        LOG.debug("chatManager existed.  simply replace socket");
	    	//replace old socket with new ones
	    	manager.replaceSocket(socket);
	    	
	    }
    }
    
    public ChatManager getManager(String name){
        return buddyMap.get(name);
    }
    
    public boolean removeChatManager(String name){
        //remove session from the list
        System.out.println("REMOVE CHATMANAGEr: key is " + name);
        ChatManager manager = buddyMap.remove(name);
        
        //close sockets and chatmanagers
        return manager.closeChatManager();
    }
    
    
    
    
    
    
    
}
