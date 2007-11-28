package com.limegroup.gnutella.privategroups;

import java.net.Socket;

import org.jivesoftware.smack.packet.Packet;


/**
 * 
 * A buddy session represents a live direct ip connection with another user. Every buddy session 
 * contains 1 chat manager.  A user can send packets to another user through their established sessions.
 *
 */
public class BuddySession {
    
    private ChatManager chatManager;

    
    public BuddySession(Socket socket){
        this.chatManager = new ChatManager(socket);
    }
    
    public ChatManager getChatManager(){
        return chatManager;  
    }
    
    public boolean closeSession(){
        return chatManager.closeChatManager();
        
    }
    
    public void send(Packet packet){
        chatManager.send(packet);
    }

}
