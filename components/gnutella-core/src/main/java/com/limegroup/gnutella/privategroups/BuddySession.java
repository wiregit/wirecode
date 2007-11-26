package com.limegroup.gnutella.privategroups;

import java.net.Socket;

import org.jivesoftware.smack.packet.Packet;


/**
 * 
 * Each buddy session has 1 chatmanager
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
