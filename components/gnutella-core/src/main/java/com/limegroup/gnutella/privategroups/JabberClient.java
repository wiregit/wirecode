package com.limegroup.gnutella.privategroups;

import java.math.BigInteger;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;

public class JabberClient{

    private BigInteger publicKey;
    private BigInteger privateKey;
    private BigInteger modulus;
    private String ipAddress;
    private String port;
    private String localUsername;
    private HashMap<String, Socket> msgSockets = new HashMap<String, Socket>();
    
    
    
    
    public JabberClient(){
    }
    
    
    /**
     * Returns a singleton UserManager instance.
     *
     * @return a UserManager instance.
     */
//    public static JabberClient getInstance() {
//        return instance;
//    }
    
    public XMPPConnection connectToServerNoPort(String serverAddress){
        
        // Create a connection to the jabber.org server.
        XMPPConnection conn1 = new XMPPConnection(serverAddress);
 
        try {
            conn1.connect();
            System.out.println("connection successful");
            //conn1.login("username", "password");
            
        } catch (XMPPException e) {
            System.out.println("could not connect to the server: " + serverAddress);
            e.printStackTrace();
        }
        return conn1;
    }
    
    public XMPPConnection connectToServerPort(String serverAddress, int portAddress){
        
     // Create a connection to the jabber.org server on a specific port.
        ConnectionConfiguration config = new ConnectionConfiguration(serverAddress, portAddress);
        XMPPConnectionPGRP conn2 = new XMPPConnectionPGRP(config);
        try {
            conn2.connect();
        } catch (XMPPException e) {
            System.out.println("could not connect to the server: " + serverAddress + ", on port: " + portAddress);
            e.printStackTrace();
        }
        return conn2;
    }
    
    /**
     * see the roster list
     * 
     */
    public void viewRoster(XMPPConnection connection){
        System.out.println("view roster");
        Roster roster = connection.getRoster();
        if(roster!=null){
            Collection<RosterEntry> entries = roster.getEntries();
            for (RosterEntry entry : entries) {
                System.out.println(entry + "status is " + ", " + entry.getStatus() + ", " + entry.getType());

            }
        }
        else{
            System.out.println("empty roster");
        }
    }
    
    public void createAccount(String username, String password, XMPPConnection connection){
        AccountManager accountManager = new AccountManager(connection);
        if(accountManager.supportsAccountCreation()){
            try {
                
                accountManager.createAccount(username, password);
                //Roster roster = connection.getRoster();
                //roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
                System.out.println("created account successfully");
                
            } catch (XMPPException e) {
                e.printStackTrace();
            }
            
        }
        else
            System.out.println(accountManager.getAccountInstructions());
    }
    
    public void removeAccount(XMPPConnection connection) throws XMPPException{
        AccountManager accountManager = new AccountManager(connection);
        
        //remove all roster entries
        Roster roster = connection.getRoster();
        Collection<RosterEntry> collection = roster.getEntries();
        Iterator i = collection.iterator();
        
        while(i.hasNext()){
            //remove entries
            RosterEntry entry= (RosterEntry)i.next();
            roster.removeEntry(entry);
        }
        
        //delete user account
        try {    
            accountManager.deleteAccount();
            System.out.println("account deletion successful");
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }
    
    public void loginAccount(String username, String password, XMPPConnection conn){
        try {

            conn.login(username, password);
            System.out.println("connected successfully");
            
            localUsername = username;
            
            //register IQ provider
            ProviderManager providerManager = ProviderManager.getInstance();
            providerManager.addIQProvider("stor", "jabber:iq:stor", new com.limegroup.gnutella.privategroups.ValueStorageProvider());
            

            //generate keys
            
            RSA keyGen = new RSA(32);
            
            publicKey = keyGen.getPublicKey();
            privateKey = keyGen.getPrivateKey();
            modulus= keyGen.getModulus();
            
            ipAddress = "10.254.0.30";//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            port = "5222";//new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();
            
            
            //store ip address, port, and public key in the server
            ValueStorage storagePacket = new ValueStorage();
            storagePacket.setType("SET");
            storagePacket.setUsername(username);
            storagePacket.setTo("lw-intern02");
            storagePacket.setIPAddress(ipAddress);
            storagePacket.setPort(port);
            storagePacket.setPublicKey(publicKey.toString());
            
            conn.sendPacket(storagePacket);
            
            //start serverSocket
//            ServerSocketClass serverSocket = new ServerSocketClass();
//            serverSocket.initializeServerSocket(9999);
            
        } catch (XMPPException e) {
            System.out.println("Could not connect to the server");
        }
    }
    
    public void sendMessage(String username, String message){
        
        //get socket from hashmap
        ClientSocket msgSocket = (ClientSocket) msgSockets.get(username);
        if (msgSocket!=null){
            msgSocket.sendMessage(message);
        }
    }
    
    private void setRemoteConnection(String username, XMPPConnection conn){
        
        //use valueStorage packet to get ip address, port, and public key
        ValueStorage storagePacket = new ValueStorage();
        storagePacket.setTo("lw-intern02");
        storagePacket.setUsername(username);
        storagePacket.setType("GET");
        
        PacketFilter filter = new AndFilter(new PacketIDFilter(storagePacket.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = conn.createPacketCollector(filter);

        conn.sendPacket(storagePacket);

        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        if (result instanceof ValueStorage) {
            ValueStorage data = (ValueStorage) result;
            System.out.println(data.getIPAddress());
            System.out.println(data.getPort());
            System.out.println(data.getPublicKey());
            
            //establish connection with remote user and put connection into map
            ClientSocket clientSocket = new ClientSocket(data.getIPAddress(), 9999, localUsername);
            clientSocket.createClientConnection();
            
            msgSockets.put(username, clientSocket);
        }   
    }
    
    public void addToRoster(String username, String nickName, String groupName, XMPPConnection connection) {
        Roster roster = connection.getRoster();
        String jid = username + "@lw-intern02";
        
        if (groupName==null)
            try {

                roster.createEntry(jid, nickName, null);
            } catch (XMPPException e1) {
                e1.printStackTrace();
            }
        else{
            try{
                roster.createGroup(groupName);
                roster.createEntry(jid, nickName, new String[] {groupName});
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    
    public void removeFromRoster(String username, String groupName, XMPPConnection connection){
        Roster roster = connection.getRoster();

        Collection <RosterGroup> group = roster.getGroups();
        
        if (!group.isEmpty()){
            Iterator i = group.iterator();
            
            while(i.hasNext()){
                RosterGroup rosterGroup = (RosterGroup) i.next();
                if (rosterGroup.getName().equals(groupName)){
                    //found group, remove user
                    try {
                        RosterEntry entry = rosterGroup.getEntry(username);
                        roster.removeEntry(entry);
                    } catch (XMPPException e) {
                        System.out.println("couldn't remove entry");
                    }
                }
            }
        }
        
        //no groups
        Collection<RosterEntry> entries = roster.getEntries();
        Iterator i = entries.iterator();
        
        while(i.hasNext()){
            RosterEntry nextEntry = ((RosterEntry)i.next());
            //remove entries
            if(nextEntry.getUser().equals(username))
                try {
                    roster.removeEntry(nextEntry);
                } catch (XMPPException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }   
    }
  
//    /**
//     * testing method to view the attribute values
//     * @param connection
//     */
//    private void viewAttributes(XMPPConnection connection) {
//        Map <String, String> attributes = connection.getAccountManager().getAccountAttributesMap();
//        System.out.println("passphrase: " + attributes.get("passphrase"));
//        System.out.println("publickey: " + attributes.get("publickey"));
//        System.out.println("DONE");
//    }
    
    /**
     *  sets subscription mode to manual so that other users must agree to allow 
     *  others to see their online status
     * 
     */
  
    public void setSubscriptionMode(XMPPConnection connection) {

        Roster roster = connection.getRoster();
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
        System.out.println("Current Subscription mode is " + roster.getSubscriptionMode());
    } 
    
    /**
     * finds username in the roster
     */
    public boolean findRosterUserName(XMPPConnection connection, String username){
        Roster roster = connection.getRoster();
        Collection <RosterEntry> usernames = roster.getEntries();
        for(Iterator i = usernames.iterator(); i.hasNext();){
             if(((RosterEntry)i.next()).getUser().equals(username))
                 return true;
        }
        return false;
    }
    
    
 
    public static void main(String[] args) throws XMPPException {
        
        XMPPConnection.DEBUG_ENABLED = true;
        
        JabberClient client = new JabberClient();
        XMPPConnection connection = client.connectToServerNoPort("lw-intern02");
        //XMPPConnection connection = manager.connectToServerPort("10.254.0.30", 5222);
        //manager.loginAccount("admin", "admin", connection);
        //System.out.println(manager.findRosterUserName(connection, "anthony@lw-intern02"));
        //  Create account
        //manager.createAccount("lulu4", "Lulu4", connection);      
        client.loginAccount("lulu4", "Lulu4", connection);

        //get remote user info
        client.setRemoteConnection("lulu", connection);
        client.sendMessage("lulu", "hi");
       
        
//        client.loginAccount("lulu", "Lulu", connection);
        
  
        //  Login with an account
        
        //manager.setSubscriptionMode(connection);
  
        //Remove user account
        //manager.removeAccount(connection);
                       
        //  Add to roster group
        
        //manager.addRosterGroup("dominic", "Dominic", null, connection);
        //manager.addRosterGroup("intern3", "Anthony", "Limewire-intern", connection);
        //manager.addRosterGroup("intern2", "MikeT", "Limewire-intern", connection);
        //manager.addRosterGroup("fulltime1", "MikeE", "Limewire-full", connection);
        //manager.addRosterGroup("fulltime2", "Dan", "Limewire-full", connection);
        //manager.addRosterGroup("fulltime3", "Felix", "Limewire-full", connection);
        //manager.addRosterGroup("fulltime4", "Sam", "Limewire-full", connection);
        //manager.addRosterGroup("fulltime5", "Zlatin", "Limewire-full", connection);
        //manager.addRosterGroup("ex-employee", "Steffen", "Limewire-ex", connection);
        
        
        // Look at roster
//        System.out.println("************************* roster list after add **********************************");
        //manager.viewAttributes(connection);
        
//        client.viewRoster(connection);
        
        //  Remove from roster group
        
       //manager.removeRosterGroup("anthony@lw-intern02", null, connection);
        /*manager.removeRosterGroup("intern2", "Limewire-intern", connection);
        manager.removeRosterGroup("fulltime1", "Limewire-full", connection);
        manager.removeRosterGroup("fulltime2", "Limewire-full", connection);
        manager.removeRosterGroup("fulltime3", "Limewire-full", connection);
        manager.removeRosterGroup("fulltime4", "Limewire-full", connection);
        manager.removeRosterGroup("fulltime5", "Limewire-full", connection);
        manager.removeRosterGroup("ex-employee", "Limewire-ex", connection);*/
        
        // Look at roster
//        System.out.println("************************* roster list after delete **********************************");
        
        //manager.getUserID()
        //manager.viewRoster(connection);
        
        
        
        //connection.disconnect();
    }

}
