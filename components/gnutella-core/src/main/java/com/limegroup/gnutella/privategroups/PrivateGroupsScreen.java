package com.limegroup.gnutella.privategroups;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jivesoftware.smack.XMPPConnection;

import com.limegroup.gnutella.dht.DHTManager;

/**
 * GUI Interface for private groups
 * 1) Login/Create Account Screen
 */
public class PrivateGroupsScreen{

    
    private static JabberClient client;
    private XMPPConnection connection;
    private String serverName;
    private DHTManager dhtManager;
    private int socketPort = 600;
    
    
    public PrivateGroupsScreen(){
        this.client = new JabberClient();
    }
    
    
    public void setServerName(String serverName){
        this.serverName = serverName;
    }
    
    public String getServerName(String serverName){
        return serverName;
    }
    
    private static XMPPConnection createConnection(String serverName){
        return (client.connectToServerNoPort(serverName));
    }
    
    private static XMPPConnection createConnection(String serverName, int port){
        return (client.connectToServerPort(serverName, port));
    }
   
    private boolean userLogin(String username, String password){
        
        try{
            connection = createConnection(serverName);
            client.loginAccount(username, password, connection); 
        }
        catch(Exception e){
            //could not login properly
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    private boolean userLogoff(){
        try{
            connection.disconnect();
        }
        catch(Exception e){
            //could not logoff properly
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
