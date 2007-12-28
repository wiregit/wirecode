package com.limegroup.gnutella.privategroups;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.limewire.io.NetworkUtils;
import org.limewire.privategroups.utils.PrivateGroupsUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Defines implementation of the interface, PGRPClient
 */
@Singleton
public class PGRPClientImpl implements PGRPClient{

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String ipAddress;
    private String port;
    private String localUsername;
    private PGRPServerSocket serverSocket;
    private static String servername = "lw-intern02";
    private XMPPConnection connection;
    private BuddyListManager buddyListManager;
    private static final Log LOG = LogFactory.getLog(PGRPClientImpl.class);

    @Inject
    public PGRPClientImpl(BuddyListManager buddyListManager) {
        this.buddyListManager = buddyListManager;
    }
    
    public String getServername(){
        return servername;
    }
    
    private void connectToServerNoPort(String serverAddress){
        
        // Create a connection to the jabber.org server.
        connection = new XMPPConnection(serverAddress);
 
        try {
            connection.connect();
            System.out.println("connection successful");
            
        } catch (XMPPException e) {
            LOG.debug("could not connect to the server: " + serverAddress);
            e.printStackTrace();
        }
    }
    
    public BuddyListManager getBuddyListManager(){
        return buddyListManager;
    }
    
    public void connectToServerPort(String serverAddress, int portAddress){
        
     // Create a connection to the jabber.org server on a specific port.
        ConnectionConfiguration config = new ConnectionConfiguration(serverAddress, portAddress);
        XMPPConnection conn2 = new XMPPConnection(config);
        try {
            conn2.connect();
        } catch (XMPPException e) {
            System.out.println("could not connect to the server: " + serverAddress + ", on port: " + portAddress);
            e.printStackTrace();
        }
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
                LOG.debug(entry + "status is " + ", " + entry.getStatus() + ", " + entry.getType());

            }
        }
        else{
            LOG.debug("empty roster");
        }
    }
    
    public boolean createAccount(String username, String password){
        AccountManager accountManager = new AccountManager(connection);
        if(accountManager.supportsAccountCreation()){
            try {
                
                accountManager.createAccount(username, password);
                LOG.debug("created account successfully");  
            } catch (XMPPException e) {
                e.printStackTrace();
                return false;
            }
            
        }
        else
            LOG.debug("You need to fill in the following information to create a new account." + accountManager.getAccountInstructions());
        
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
            LOG.debug("account deletion successful");
            
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
        
        logoff();
        return true;
    }
    
    public void sendSubscriptionResponse(boolean allow, String from){
        
        LOG.debug("PGRPClientImpl: sendSubscriptionResponse");
        Presence response;
        if(allow)
            response = new Presence(Presence.Type.subscribed);
        else
            response = new Presence(Presence.Type.unsubscribed);
        
        response.setTo(from);
        connection.sendPacket(response);
    }
    
    
    public boolean loginAccount(String username, String password){
        try {
            
            LOG.debug("begin loginAccount");
            connectToServerNoPort(servername);
            connection.login(username, password);
            LOG.debug("login successful");
            
            localUsername = username;
            
            LOG.debug("set Subscription to Manual");
            //set subscription manual mode
            setSubscriptionModeManual();
            
            //register to listen for subscription requests
            connection.addPacketListener(new SubscriptionListener(), new PacketFilter(){

                public boolean accept(Packet packet) {
                    if(packet instanceof Presence)
                        if(((Presence)packet).getType().equals(Presence.Type.subscribe))
                            return true;
                    return false;
                }});
            
            LOG.debug("Register IQStor Provider");
            //register IQ provider
            ProviderManager providerManager = ProviderManager.getInstance();
            
            /**
             * name and namespace identify the iq handler that handles ValueStorage packets.  "stor" is the name, and "jabber:iq:stor" is the namespace.
             */
            providerManager.addIQProvider("stor", "jabber:iq:stor", new com.limegroup.gnutella.privategroups.ValueStorageProvider());
                       
            try {
                ipAddress = NetworkUtils.ip2string((InetAddress.getLocalHost()).getAddress());//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            
            //should automatically detect the local port
            //"new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();"
            //for now the port will be hardcoded
            port = "5222";
            
            LOG.debug("Generate key pairs");
            //generate public and private keys
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "DSA" );
            keyGen.initialize( 1024 );
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            
            LOG.debug("Create ValueStorage packet to set user credentials and send to server");
            //store ip address, port, and public key in the server
            ValueStorage storagePacket = new ValueStorage();
            storagePacket.setType(IQ.Type.SET);
            storagePacket.setUsername(username);
            storagePacket.setTo(servername);
            storagePacket.setIPAddress(ipAddress);
            storagePacket.setPort(port);
            storagePacket.setPublicKey(publicKey.toString());
            
            connection.sendPacket(storagePacket);
            
            LOG.debug("Start ServerSocket");
            //start serverSocket
            serverSocket = new PGRPServerSocket(localUsername, servername, connection, buddyListManager);
            serverSocket.start();

            return true;
            
        } catch (XMPPException e) {
            LOG.debug("Could not login to the server");
            connection.disconnect();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        
        return false;
    }
    
    public boolean logoff(){
        try{
            
            if(serverSocket!=null)
                serverSocket.closeSocket();
            connection.disconnect();
            
            LOG.debug("logged off");
        
        }catch(Exception e){
            LOG.debug("could not disconnect user");
            return false;
        }
        return true;
    }
    
    public boolean sendPacket(String username, Packet packet){
        
        LOG.debug("Send Message Method");
          
        //check list to see if buddy is a buddy
        if(findRosterUserName(username)){
            LOG.debug("find user name. is buddy or not?");
            ChatManager chatManager = buddyListManager.getManager(username);
            if((chatManager== null)||(!chatManager.checkIfRemoteWindowExists())){
                LOG.debug("Could not find chatManager");
                //need to get remote user info and establish session
                if(setRemoteConnection(username, localUsername)){
                    LOG.debug("get chatmanager");
                    chatManager = buddyListManager.getManager(username);
                    LOG.debug("send packet through chatmanager");
                    chatManager.send(packet);
                    LOG.debug("set remote window exists to true");
                    chatManager.setRemoteWindowExists(true);
                    return true;
                }
            }
            else{
                LOG.debug("found chatManager");
                chatManager.send(packet);
                return true;
            }
            return false;
        }
        else{
            LOG.debug("you cannot send a message to somebody not on your buddy list!");
            return false;
        }
    }
    

    
    public boolean addToRoster(String username, String nickName, String groupName) {
        LOG.debug("PGRPClientImpl: addToRoster");
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
        LOG.debug("addtoRoster is finished");
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
                        LOG.debug("couldn't remove entry");
                        return false;
                    }
                }
            }
        }
        
        //no groups
        Collection<RosterEntry> entries = roster.getUnfiledEntries();
        Iterator i = entries.iterator();
        
        while(i.hasNext()){
            RosterEntry nextEntry = ((RosterEntry)i.next());
            //remove entries
            if(nextEntry.getUser().equals(usernameserver))
                try {
                    roster.removeEntry(nextEntry);
                } catch (XMPPException e) {
                    e.printStackTrace();
                    return false;
                }
        }   
        
        return true;
    }
    

    public boolean findRosterUserName(String username){
        
        Roster roster = connection.getRoster();
        Collection <RosterEntry> usernames = roster.getEntries();
        for(Iterator i = usernames.iterator(); i.hasNext();){
            String user = ((RosterEntry)i.next()).getUser();
             if(user.equals(username))
                 return true;
        }
        return false;
    }
    
    public Roster getRoster(){
        return connection.getRoster();
    }
  
    /**
     * queries the server to find the ip address associated with the remote username
     */
    public boolean setRemoteConnection(String remoteUserNameServer, String localUsername){
        LOG.debug("setRemoteConnection method");
        //remove the server name part of the string
        int index = remoteUserNameServer.lastIndexOf('@');
        String remoteUserNameOnly = remoteUserNameServer.substring(0, index);
        
        LOG.debug("Send storage packet to server to get username associated with ip address: " + remoteUserNameOnly);
        //use valueStorage packet to get ip address, port, and public key
        ValueStorage storagePacket = new ValueStorage();
        storagePacket.setTo(servername);
        storagePacket.setUsername(remoteUserNameOnly);
        storagePacket.setType(IQ.Type.GET);
        

        PacketFilter filter = new AndFilter(new PacketIDFilter(storagePacket.getPacketID()),
                new PacketTypeFilter(IQ.class));
        
        //PacketCollector blocks
        PacketCollector collector = connection.createPacketCollector(filter);

        connection.sendPacket(storagePacket);

        
        LOG.debug("Get Result IQ from the collector");
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        
        if (result instanceof ValueStorage) {
            LOG.debug("Result is an instance of ValueStorage");
            ValueStorage data = (ValueStorage) result;
            if(data.getIPAddress()!=null){
                LOG.debug("Result has an IP Address");
                //create new session and add to buddyListManager
                try {
                    LOG.debug("Add a new chat manager for the local user for the new conversation");
                    LOG.debug("remoteConnection: " + remoteUserNameServer + " and their ip address is "+ data.getIPAddress());   
                    buddyListManager.addChatManager(remoteUserNameServer, localUsername, new Socket(data.getIPAddress(), 9999));
                    return true;
                } catch (NumberFormatException e) {
                    LOG.debug("Encountered NumberFormatException");
                    e.printStackTrace();
                } catch (IOException e) {
                    LOG.debug("Encountered IOException");
                    e.printStackTrace();
                }
            }else{
                LOG.debug("no ip address found");
            }
        }
        return false;
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
     * test method only --> for the client to not have a server socket (when testing on the same computer)
     */
    public boolean loginAccountNoServerSocket(String username, String password){
        try {

            connection.login(username, password);
            LOG.debug("login successful");
            
            localUsername = username;
            
            //set subscription manual mode
            setSubscriptionModeManual();
            
            //register IQ provider
            ProviderManager providerManager = ProviderManager.getInstance();
            /**
             * name and namespace identify the iq handler that handles ValueStorage packets.  "stor" is the name, and "jabber:iq:stor" is the namespace.
             */
            providerManager.addIQProvider("stor", "jabber:iq:stor", new com.limegroup.gnutella.privategroups.ValueStorageProvider());
            
            try {
                ipAddress = NetworkUtils.ip2string((InetAddress.getLocalHost()).getAddress());//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            

            //should automatically detect the local port
            //"new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();"
            //for now the port will be hardcoded
            port = "5222";
            
            //generate public and private keys
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "DSA" );
            keyGen.initialize( 1024 );
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            
            //store ip address, port, and public key in the server
            ValueStorage storagePacket = new ValueStorage();
            storagePacket.setType(IQ.Type.SET);
            storagePacket.setUsername(username);
            storagePacket.setTo(servername);
            storagePacket.setIPAddress(ipAddress);
            storagePacket.setPort(port);
            storagePacket.setPublicKey(publicKey.toString());
            
            connection.sendPacket(storagePacket);

            return true;
            
        } catch (XMPPException e) {
            LOG.debug("Could not login to the server");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        
        return false;
    }
    
    public PrivateKey getPrivateKey(){
        return privateKey;
    }
    

    public static void main(String[] args) throws XMPPException {
        
        XMPPConnection.DEBUG_ENABLED = true;
        
        PGRPClientImpl client = new PGRPClientImpl(new BuddyListManager());
        client.connectToServerNoPort(servername);
        
//        client.loginAccount("admin", "admin");
//        client.createAccount("MikeT", "1234");

        //  Create account
//        client.createAccount("Dan", "1234");   
//        client.createAccount("Bob", "1234"); 
//        client.createAccount("Mary", "1234"); 
        
          client.loginAccount("Bob", "1234");
//          
//          client.addToRoster("Mary", "Mary LimeWire", "Tester");
          client.viewRoster();
          client.logoff();
        
//        client.loginAccountNoServerSocket("Anthony", "1234");
        
//        client.setRemoteConnection("anthony@lw-intern02", "dan");
//      client.loginAccount("Anthony", "1234");
//          client.sendMessage("dan@lw-intern02", "test");
//        ChatManager temp = client.getBuddyListManager().getManager("anthony@lw-intern02");
        
        
//          client.addToRoster("anthony", "Anthony Bow", "");
//          client.removeFromRoster("anthony", "");
//          client.viewRoster();
//        client.addToRoster("lulu", "", "");

//        client.addToRoster("anthony","","");
//        client.loginAccountNoServerSocket("Anthony", "1234");
//        client.sendMessage("miket", "hi");
//        client.loginAccount("Anthony", "1234");
//        client.loginAccountNoServerSocket("Anthony", "1234");
//          client.addToRoster("miket","Mike Tiraborelli","ClientDev");
//          client.addToRoster("mikee","Mike Everett","ClientDev");
//          client.addToRoster("sam","Sam Berlin","ClientDev");
//          client.addToRoster("dan","Dan Sullivan","ClientDev");
//          client.addToRoster("zlatin","Zlatin Balvesky","ClientDev");
//          client.addToRoster("felix","Felix Berger","ClientDev");
//          client.addToRoster("curtis","Curtis Jones","ClientDev");
//          client.addToRoster("tim","Tim Julien","ClientDev");
//          client.addToRoster("anthony", "Anthony Bow", "ClientDev");
        
        
//      client.removeFromRoster("miket","ClientDev");
//      client.removeFromRoster("mikee","ClientDev");
//      client.removeFromRoster("sam","ClientDev");
//      client.removeFromRoster("dan","ClientDev");
//      client.removeFromRoster("zlatin","ClientDev");
//      client.addToRoster("felix","Felix Berger","ClientDev");
//      client.removeFromRoster("curtis","ClientDev");
//      client.removeFromRoster("tim","ClientDev");
//      client.removeFromRoster("anthony", "ClientDev");
        
//        
//      client.viewRoster();
//        client.sendMessage("dan", "hi");

//        client.sendMessage("lulu", "wassup");

//        client.sendMessage("lulu", "this is a test message from Anthony");

//        client.sendMessage("lulu", "end of test");

//        client.sendMessage("lulu", "later");

  
        //  Login with an account
        
        //manager.setSubscriptionMode(connection);

        
        //client.logoff();
    }
}
