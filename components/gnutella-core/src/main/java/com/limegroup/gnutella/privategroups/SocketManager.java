package com.limegroup.gnutella.privategroups;

import java.net.Socket;
import java.util.HashMap;

public class SocketManager {

    private SocketManager instance = new SocketManager();
    private HashMap<String, Socket> msgSockets = new HashMap<String, Socket>();
    
    public SocketManager(){
    }
    
    public SocketManager getInstance(){
        return instance;
    }
    
    public void putHashMap(String key, Socket socketValue){
        msgSockets.put(key, socketValue);
    }
    
    public Socket getSocket(String key){
        return (Socket) msgSockets.get(key);  
    }
    
}
