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
       // this.dhtManager = GuiCoreMediator.getDHTManager();
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
            
            //after login, start server socket to listen for client connections
            
            Runnable r = new ServerSocketListener();
            Thread t = new Thread(r);
            t.start();        
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
    
    
    private class ServerSocketListener implements Runnable{

        public void run() {
            //call serversocketclass 
            ServerSocketClass serverSocket = new ServerSocketClass();
            serverSocket.initializeServerSocket(socketPort);
        }
    }
    
    //dictates what happens when clicking on the register account button
    private class RegisterButtonListener implements ActionListener{

        public void actionPerformed(ActionEvent e) {
            //use e.getSource() to determine who triggered the event and perform the corresponding action 
        }
    }
    
    public static void main(String[] args) {
        //ClientSocket clientSocket = new ClientSocket("69.201.186.164", 9999);
        //clientSocket.createClientConnection();
        
    }
}
