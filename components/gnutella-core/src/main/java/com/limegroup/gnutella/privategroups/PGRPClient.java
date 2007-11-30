package com.limegroup.gnutella.privategroups;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

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
import org.limewire.io.NetworkUtils;
import org.limewire.util.PrivateGroupsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PGRPClient{

    private BigInteger publicKey;
    private BigInteger privateKey;
    private BigInteger modulus;
    private String ipAddress;
    private String port;
    private String localUsername;
    private static PGRPClient instance;
    private PGRPServerSocket serverSocket;
    private static String servername = "lw-intern02";
    private XMPPConnection connection;
    
    @Inject
    public PGRPClient(XMPPConnection connection){
        this.connection = connection;
        PGRPClient.instance = this;
    }
    
    /**
     * Returns a singleton PGRPClient instance.
     *
     * @return a PGRPClient instance.
     */
    public static PGRPClient getInstance() {
        return instance;
    }
    
    public static XMPPConnection connectToServerNoPort(String serverAddress){
        
        // Create a connection to the jabber.org server.
        XMPPConnection conn1 = new XMPPConnection(serverAddress);
 
        try {
            conn1.connect();
            System.out.println("connection successful");
            
        } catch (XMPPException e) {
            System.out.println("could not connect to the server: " + serverAddress);
            e.printStackTrace();
        }
        return conn1;
    }
    
    public XMPPConnection connectToServerPort(String serverAddress, int portAddress){
        
     // Create a connection to the jabber.org server on a specific port.
        ConnectionConfiguration config = new ConnectionConfiguration(serverAddress, portAddress);
        XMPPConnection conn2 = new XMPPConnection(config);
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
    public void viewRoster(){
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
    
    public boolean createAccount(String username, String password){
        AccountManager accountManager = new AccountManager(connection);
        if(accountManager.supportsAccountCreation()){
            try {
                
                accountManager.createAccount(username, password);
                System.out.println("created account successfully");  
            } catch (XMPPException e) {
                e.printStackTrace();
                return false;
            }
            
        }
        else
            System.out.println(accountManager.getAccountInstructions());
        
        return true;
    }
    
    public boolean removeAccount() throws XMPPException{
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
            return false;
        }
        
        logoff();
        return true;
    }
    
    public boolean loginAccount(String username, String password){
        try {

            connection.login(username, password);
            System.out.println("login successful");
            
            localUsername = username;
            
            //set subscription manual mode
            setSubscriptionModeManual();
            
            //register IQ provider
            ProviderManager providerManager = ProviderManager.getInstance();
            providerManager.addIQProvider("stor", "jabber:iq:stor", new com.limegroup.gnutella.privategroups.ValueStorageProvider());
            
            //generate keys
            
            //RSA keyGen = new RSA(32);
            
            publicKey = new BigInteger("19");//keyGen.getPublicKey();
            //privateKey = keyGen.getPrivateKey();
            //modulus= keyGen.getModulus();
            
            try {
                ipAddress = NetworkUtils.ip2string((InetAddress.getLocalHost()).getAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }//"10.254.0.30";//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            port = "5222";//new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();
            
            
            //store ip address, port, and public key in the server
            ValueStorage storagePacket = new ValueStorage();
            storagePacket.setType("SET");
            storagePacket.setUsername(username);
            storagePacket.setTo(servername);
            storagePacket.setIPAddress(ipAddress);
            storagePacket.setPort(port);
            storagePacket.setPublicKey(publicKey.toString());
            
            connection.sendPacket(storagePacket);
            
            //start serverSocket
            serverSocket = new PGRPServerSocket(connection);
            serverSocket.start();

            return true;
            
        } catch (XMPPException e) {
            System.out.println("Could not login to the server");
            return false;
        }
    }
    
    public boolean logoff(){
        try{
            
            if(serverSocket!=null)
                serverSocket.closeSocket();
            connection.disconnect();
            System.out.println("logged off");
        
        }catch(Exception e){
            System.out.println("could not disconnect user");
            return false;
        }
        return true;
    }
    
    public boolean sendMessage(String username, String message){
        
        //check list to see if buddy is a buddy
        if(findRosterUserName(username)){
        
            BuddySession buddySession = BuddyListManager.getInstance().getSession(username);
            if(buddySession== null){
                //need to get remote user info and establish session
                if(setRemoteConnection(username)){
                    buddySession = BuddyListManager.getInstance().getSession(username);
                    buddySession.send(PrivateGroupsUtils.createMessage(localUsername, message));
                    return true;
                }
            }
            else{
                buddySession.send(PrivateGroupsUtils.createMessage(localUsername, message));
                return true;
            }
            return false;
        }
        else{
            System.out.println("you cannot send a message to somebody not on your buddy list!");
            return false;
        }
    }
    
    private boolean setRemoteConnection(String username){
        
        //use valueStorage packet to get ip address, port, and public key
        ValueStorage storagePacket = new ValueStorage();
        storagePacket.setTo(servername);
        storagePacket.setUsername(username);
        storagePacket.setType("GET");
        
        PacketFilter filter = new AndFilter(new PacketIDFilter(storagePacket.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);

        connection.sendPacket(storagePacket);

        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        if (result instanceof ValueStorage) {
            ValueStorage data = (ValueStorage) result;
            if(data.getIPAddress()!=null){
                //create new session and add to buddyListManager
                try {
                    BuddyListManager.getInstance().addBuddySession(username, new Socket(data.getIPAddress(),  9999));
                    return true;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    
    public boolean addToRoster(String username, String nickName, String groupName) {
        Roster roster = connection.getRoster();
        String jid = username + "@"+ servername;
        
        if (groupName==null)
            try {

                roster.createEntry(jid, nickName, null);
            } catch (XMPPException e1) {
                e1.printStackTrace();
                return false;
            }
        else{
            try{
                //see if group already exists
                try{
                    roster.createGroup(groupName);
                }catch(Exception e){
                    //group already exists
                }
                roster.createEntry(jid, nickName, new String[] {groupName});
            }
            catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
    
    public boolean removeFromRoster(String username, String groupName){
        Roster roster = connection.getRoster();
        String usernameserver = username+ "@" + servername;

        Collection <RosterGroup> group = roster.getGroups();
        
        if (!group.isEmpty()){
            Iterator i = group.iterator();
            
            while(i.hasNext()){
                RosterGroup rosterGroup = (RosterGroup) i.next();
                if (rosterGroup.getName().equals(groupName)){
                    //found group, remove user
                    try {
                        RosterEntry entry = rosterGroup.getEntry(usernameserver);
                        roster.removeEntry(entry);
                    } catch (XMPPException e) {
                        System.out.println("couldn't remove entry");
                        return false;
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
                    e.printStackTrace();
                    return false;
                }
        }   
        
        return true;
    }
  
    /**
     * test method to view attribute values
     */
    private void viewAttributes() {
        Map <String, String> attributes = connection.getAccountManager().getAccountAttributesMap();
        System.out.println("passphrase: " + attributes.get("passphrase"));
        System.out.println("publickey: " + attributes.get("publickey"));
        System.out.println("DONE");
    }
    
    /**
     *  sets subscription mode to manual so that other users must agree to allow 
     *  others to see their online status
     * 
     */
  
    private void setSubscriptionModeManual() {

        Roster roster = connection.getRoster();
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
    } 
    
    /**
     * test method to find user in the roster
     */
    public boolean findRosterUserName(String username){
        Roster roster = connection.getRoster();
        Collection <RosterEntry> usernames = roster.getEntries();
        for(Iterator i = usernames.iterator(); i.hasNext();){
            String user = ((RosterEntry)i.next()).getUser();
             if(user.equals(username + "@" + servername))
                 return true;
        }
        return false;
    }
    
    /**
     * test method only --> for the client to not have a server socket (when testing on the same computer)
     */
    public boolean loginAccountNoServerSocket(String username, String password){
        try {

            connection.login(username, password);
            System.out.println("login successful");
            
            localUsername = username;
            
            //set subscription manual mode
            setSubscriptionModeManual();
            
            //register IQ provider
            ProviderManager providerManager = ProviderManager.getInstance();
            providerManager.addIQProvider("stor", "jabber:iq:stor", new com.limegroup.gnutella.privategroups.ValueStorageProvider());
            
            //generate keys
            
            //RSA keyGen = new RSA(32);
            
            publicKey = new BigInteger("19");//keyGen.getPublicKey();
            //privateKey = keyGen.getPrivateKey();
            //modulus= keyGen.getModulus();
            
            ipAddress = "10.254.0.30";//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            port = "5222";//new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();
            
            
            //store ip address, port, and public key in the server
            ValueStorage storagePacket = new ValueStorage();
            storagePacket.setType("SET");
            storagePacket.setUsername(username);
            storagePacket.setTo(servername);
            storagePacket.setIPAddress(ipAddress);
            storagePacket.setPort(port);
            storagePacket.setPublicKey(publicKey.toString());
            
            connection.sendPacket(storagePacket);

            return true;
            
        } catch (XMPPException e) {
            System.out.println("Could not login to the server");
            return false;
        }
    }
    

    public static void main(String[] args) throws XMPPException {
        
        XMPPConnection.DEBUG_ENABLED = true;
        
        PGRPClient client = new PGRPClient(connectToServerNoPort(servername));
        
        //manager.loginAccount("admin", "admin", connection);
    
        //  Create account
        //manager.createAccount("user1", "password1");   
        //manager.createAccount("user2", "password2"); 
        
          client.loginAccount("user2", "password2");
          client.viewRoster();
        
//        client.loginAccountNoServerSocket("lulu4", "Lulu4");
//        for(int i = 0; i <800; i++){System.out.println(i);}
//        client.sendMessage("lulu", "hi");
//        for(int i = 0; i <800; i++){System.out.println(i);}
//        client.sendMessage("lulu", "wassup");
//        for(int i = 0; i <800; i++){System.out.println(i);}
//        client.sendMessage("lulu", "this is a test message from Anthony");
//        for(int i = 0; i <800; i++){System.out.println(i);}
//        client.sendMessage("lulu", "end of test");
//        for(int i = 0; i <800; i++){System.out.println(i);}
//        client.sendMessage("lulu", "later");
//        for(int i = 0; i <100; i++){System.out.println(i);}
  
        //  Login with an account
        
        //manager.setSubscriptionMode(connection);

        
        //client.logoff();
    }
}
