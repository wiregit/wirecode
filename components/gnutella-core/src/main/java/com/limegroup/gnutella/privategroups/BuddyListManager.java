package com.limegroup.gnutella.privategroups;

import java.net.Socket;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.listener.Event;
import org.limewire.listener.EventListener;
import org.limewire.listener.WeakEventListenerList;

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
    private static final Log LOG = LogFactory.getLog(BuddyListManager.class);
    private WeakEventListenerList<Event> buddyManagerList = new WeakEventListenerList<Event>();
            
    public void addChatManager(String remoteUsername, String localUsername, Socket socket){
        LOG.debug("BuddyListManager:addChatManager");
	    ChatManager manager = buddyMap.get(remoteUsername);
	    if(manager ==null){
	        LOG.debug("chatManager did not exist before.  create a new one");  
	        manager = new ChatManager(this, socket, remoteUsername);
	        buddyMap.put(remoteUsername, manager);
	        LOG.debug("chatManager is now in the Map. start a new message window");
	        //broadcast to listeners
	        buddyManagerList.broadcast(new ChatWindowEvent(remoteUsername, localUsername));
	        manager.initReadWriteThreads();
	    }
	    else{
	        LOG.debug("chatManager existed.  simply replace socket");
	    	//replace old socket with new ones
	    	manager.replaceSocket(socket);
	    }
    }
    
    public void addListener(String strongRef, EventListener listener){
        buddyManagerList.addListener(strongRef, listener);
    }
    
    public void removeListener(String strongRef, EventListener listener){
        buddyManagerList.removeListener(strongRef, listener);
    }
    
    public void broadcastListeners(Event e){
        buddyManagerList.broadcast(e);
    }
    
    public ChatManager getManager(String name){
        return buddyMap.get(name);
    }
    
    public boolean removeChatManager(String name){
        //remove session from the list
        LOG.debug("Remove ChatManager: " + name);
        ChatManager manager = buddyMap.remove(name);
        
        //close sockets and chatmanagers
        return manager.closeChatManager();
    }
    
    
    
    
    
    
    
}
