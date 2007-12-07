package com.limegroup.gnutella.privategroups;

import junit.framework.TestCase;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public class PGRPClientTest extends TestCase {
    
    private PGRPClientImpl client1;
    private PGRPClientImpl client2;
    private String servername = "lw-intern02";
    
    public PGRPClientTest(){
        
    }
    
    /**
     * test to..
     * 1) login with an account that does not exist
     * 2) create an account
     * 3) login with a created account
     * 4) logging off an account and making sure the server socket stops accordingly
     * 5) remove account
     */
    public void testCreateAndLoginAccount(){
        System.out.println("testCreateAndLoginAccount start");
        
        String username = "user";
        String password = "password";

        client1 = new PGRPClientImpl(new BuddyListManager());
        
        //try to login with a user that does not exist       
        assertFalse(client1.loginAccount(username, password));
        
        //create account and try to login again
        assertTrue(client1.createAccount(username, password));
        
        //logoff
        assertTrue(client1.logoff());
        
        //try to login again.  this time the user should exist
        client1 = new PGRPClientImpl(new BuddyListManager());
        assertTrue(client1.loginAccount(username, password));
        
        assertTrue(client1.logoff());
        
        //now remove account
        client1 = new PGRPClientImpl(new BuddyListManager());
        
        assertTrue(client1.loginAccount(username, password));
        //remove account and try to login again
        try {
            assertTrue(client1.removeAccount());
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        client1 = new PGRPClientImpl(new BuddyListManager());

        //try to see if account still exists

        assertFalse(client1.loginAccount(username, password));
        
        assertTrue(client1.logoff());
        
        System.out.println("testCreateAndLoginAccount done\n");
    }
    
    /**
     * test to add to roster, find user in roster, and remove from roster
     */
    public void testRoster(){
        
        System.out.println("testRoster start");
        
        String username = "user";
        String password = "password";
        
        //create initial jabber client
        PGRPClientImpl client = new PGRPClientImpl(new BuddyListManager());
        
        //create account
        assertTrue(client.createAccount(username, password));
        
        assertTrue(client.logoff());
        
        client = new PGRPClientImpl(new BuddyListManager());
        
        assertTrue(client.loginAccount(username, password));
        
        
        //add users to roster list
        client.addToRoster("testusername", "nickName", "friend");
        client.addToRoster("testusername2", "nickName2", "friend");
        client.addToRoster("testusername3", "nickName3", "friend");
        
        assertTrue(client.findRosterUserName("testusername"));
        assertTrue(client.findRosterUserName("testusername2"));
        assertTrue(client.findRosterUserName("testusername3"));
        
        client.viewRoster();
        
        client.removeFromRoster("testusername", "friend");
        client.removeFromRoster("testusername2", "friend");
        client.removeFromRoster("testusername3", "friend");
        
       
        client.viewRoster();
        
        assertFalse(client.findRosterUserName("testusername@"+servername));
        assertFalse(client.findRosterUserName("testusername2@"+servername));
        assertFalse(client.findRosterUserName("testusername3@"+servername));
        
        try {
            assertEquals(client.removeAccount(), true);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        
        System.out.println("testRoster done\n");
    }
    
    public void testMessaging(){
        
        System.out.println("testMessaging start");
        //need 2 clients
        client1 = new PGRPClientImpl(new BuddyListManager());
        client1.createAccount("user1", "password1");
        
        client2 = new PGRPClientImpl(new BuddyListManager());
        client2.createAccount("user2", "password2");
        
        client1.loginAccount("user1", "password1");
        client1.addToRoster("user2", "", "");
        client2.loginAccountNoServerSocket("user2", "password2");
        
        //send a message to a user who exists
        assertTrue(client2.sendMessage("user1", "hello user 1"));


        try {
            assertEquals(client1.removeAccount(), true);
            assertEquals(client2.removeAccount(), true);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        
        System.out.println("testMessaging done\n");
    }
}
