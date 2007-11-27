package com.limegroup.gnutella.privategroups;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
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
import org.limewire.util.PrivateGroupsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class JabberClient{

    private BigInteger publicKey;
    private BigInteger privateKey;
    private BigInteger modulus;
    private String ipAddress;
    private String port;
    private String localUsername;
    private static JabberClient instance = new JabberClient();
    //private SocketsManagerImpl socketsManager = new SocketsManagerImpl();
    private BuddyListManager listManager = new BuddyListManager();
    private Thread serverSocketThread;
    private static String servername = "lw-intern02";
    
    @Inject
    public JabberClient(){
    }
    
    
    /**
     * Returns a singleton UserManager instance.
     *
     * @return a UserManager instance.
     */
    public static JabberClient getInstance() {
        return instance;
    }
    
    
    public XMPPConnection connectToServerNoPort(String serverAddress){
        
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
    
    public boolean createAccount(String username, String password, XMPPConnection connection){
        AccountManager accountManager = new AccountManager(connection);
        if(accountManager.supportsAccountCreation()){
            try {
                
                accountManager.createAccount(username, password);
                //Roster roster = connection.getRoster();
                //roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
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
    
    public boolean removeAccount(XMPPConnection connection) throws XMPPException{
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
        return true;
    }
    
    public boolean loginAccount(String username, String password, XMPPConnection conn){
        try {

            conn.login(username, password);
            System.out.println("login successful");
            
            localUsername = username;
            
            //set subscription mode
            setSubscriptionMode(conn);
            
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
            
            conn.sendPacket(storagePacket);
            
            //start serverSocket
//            Thread serverSocketThread = new Thread(new ServerSocketClass(9999, conn));
//            serverSocketThread.start();

            return true;
            
        } catch (XMPPException e) {
            System.out.println("Could not login to the server");
            return false;
        }
    }
    
    public boolean logoff(XMPPConnection connection){
        try{
            connection.disconnect();
            System.out.println("logged off");
        
        }catch(Exception e){
            System.out.println("could not disconnect user");
            return false;
        }
        return true;
    }
    
    public void sendMessage(String username, String message){
        
        BuddySession buddySession = BuddyListManager.getInstance().getSession(username);
        if(buddySession!=null)
            buddySession.send(PrivateGroupsUtils.createMessage(localUsername, message));
        else{
            System.out.println("error sending message");
        }
    }
    
    private void setRemoteConnection(String username, XMPPConnection conn){
        
        //use valueStorage packet to get ip address, port, and public key
        ValueStorage storagePacket = new ValueStorage();
        storagePacket.setTo(servername);
        storagePacket.setUsername(username);
        storagePacket.setType("GET");
        
        PacketFilter filter = new AndFilter(new PacketIDFilter(storagePacket.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = conn.createPacketCollector(filter);

        conn.sendPacket(storagePacket);

        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        if (result instanceof ValueStorage) {
            ValueStorage data = (ValueStorage) result;
            
            //create new session and add to buddyListManager
            try {
                BuddyListManager.getInstance().addBuddySession(username, new Socket(data.getIPAddress(),  9999));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   
    }
    
    public boolean addToRoster(String username, String nickName, String groupName, XMPPConnection connection) {
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
    
    public boolean removeFromRoster(String username, String groupName, XMPPConnection connection){
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
  
    private void setSubscriptionMode(XMPPConnection connection) {

        Roster roster = connection.getRoster();
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
    } 
    
    /**
     * test method to find user in the roster
     */
    public boolean findRosterUserName(XMPPConnection connection, String username){
        Roster roster = connection.getRoster();
        Collection <RosterEntry> usernames = roster.getEntries();
        for(Iterator i = usernames.iterator(); i.hasNext();){
            String user = ((RosterEntry)i.next()).getUser();
             if(user.equals(username))
                 return true;
        }
        return false;
    }
    
    
    public static void main(String[] args) throws XMPPException {
        
        XMPPConnection.DEBUG_ENABLED = true;
        
        JabberClient client = new JabberClient();
        XMPPConnection connection = client.connectToServerNoPort(servername);
        
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
        //client.addRosterGroup("intern3", "Anthony", "Limewire-intern", connection);
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
