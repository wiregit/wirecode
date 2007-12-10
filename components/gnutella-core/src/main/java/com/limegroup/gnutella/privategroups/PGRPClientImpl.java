package com.limegroup.gnutella.privategroups;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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

    @Inject
    public PGRPClientImpl(BuddyListManager buddyListManager) {
        this.buddyListManager = buddyListManager;
        connectToServerNoPort(servername);
    }
    
    private void connectToServerNoPort(String serverAddress){
        
        // Create a connection to the jabber.org server.
        connection = new XMPPConnection(serverAddress);
 
        try {
            connection.connect();
            System.out.println("connection successful");
            
        } catch (XMPPException e) {
            System.out.println("could not connect to the server: " + serverAddress);
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
                       
            try {
                ipAddress = NetworkUtils.ip2string((InetAddress.getLocalHost()).getAddress());//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            
            port = "5222";//new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();
            
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
            
            //start serverSocket
            serverSocket = new PGRPServerSocket(localUsername, connection, buddyListManager);
            serverSocket.start();

            return true;
            
        } catch (XMPPException e) {
            System.out.println("Could not login to the server");
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
            
            System.out.println("logged off");
        
        }catch(Exception e){
            System.out.println("could not disconnect user");
            return false;
        }
        return true;
    }
    
    public boolean sendMessage(String username, String message){
        //hack to append servername - will change later
        if(username.indexOf('@')==-1){
            //append servername
            username = username + "@" + servername;
        }
        
        
        //check list to see if buddy is a buddy
        if(findRosterUserName(username)){
            ChatManager chatManager = buddyListManager.getManager(username);
            if(chatManager== null){
                    //need to get remote user info and establish session
                    if(setRemoteConnection(username, localUsername)){
                        chatManager = buddyListManager.getManager(username);
                        chatManager.send(PrivateGroupsUtils.createMessage(localUsername, username, message));
                        return true;
                    }
            }
            else{
                chatManager.send(PrivateGroupsUtils.createMessage(localUsername, username, message));
                return true;
            }
            return false;
        }
        else{
            System.out.println("you cannot send a message to somebody not on your buddy list!");
            return false;
        }
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
                        System.out.println("found group");
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
        //hack to append servername - will change later
        if(username.indexOf('@')==-1){
            //append servername
            username = username + "@" + servername;
        }
        
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
        
        //remove the server name part of the string
        int index = remoteUserNameServer.lastIndexOf('@');
        String remoteUserNameOnly = remoteUserNameServer.substring(0, index);
        
        //use valueStorage packet to get ip address, port, and public key
        ValueStorage storagePacket = new ValueStorage();
        storagePacket.setTo(servername);
        storagePacket.setUsername(remoteUserNameOnly);
        storagePacket.setType(IQ.Type.GET);
        
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
                    System.out.println("remoteConnection: " + remoteUserNameServer + " and their ip address is "+ data.getIPAddress());
//                    Socket socket = new Socket();
//                    socket.setSoTimeout(5000);
//                    
//                    socket.connect(new InetSocketAddress(data.getIPAddress(), 9999));
                    
                    buddyListManager.addChatManager(remoteUserNameServer, localUsername, new Socket(data.getIPAddress(), 9999));
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
                       
            try {
                ipAddress = NetworkUtils.ip2string((InetAddress.getLocalHost()).getAddress());//NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            
            port = "5222";//new Integer(GuiCoreMediator.getNetworkManager().getPort()).toString();
            
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
            System.out.println("Could not login to the server");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        
        return false;
    }
    

    public static void main(String[] args) throws XMPPException {
        
        XMPPConnection.DEBUG_ENABLED = true;
        
        PGRPClientImpl client = new PGRPClientImpl(new BuddyListManager());
//        client.connectToServerNoPort(servername);
        
//        client.loginAccount("admin", "admin");
//        client.createAccount("MikeT", "1234");

        //  Create account
//        client.createAccount("Dan", "1234");   
//        client.createAccount("Anthony", "1234"); 
        
//          client.loginAccountNoServerSocket("Dan", "1234");
        
//        client.loginAccountNoServerSocket("Felix", "1234");
        
//        client.setRemoteConnection("anthony@lw-intern02", "dan");
      client.loginAccount("Dan", "1234");
//          client.sendMessage("anthony@lw-intern02", "test");
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
