package com.limegroup.gnutella.privategroups;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import junit.framework.TestCase;

public class JabberClientTest extends TestCase {
    
    private JabberClient client1 = new JabberClient();
    private JabberClient client2 = new JabberClient();
    private String servername = "lw-intern02";
    
    public JabberClientTest(){
        
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
        
        String username = "user";
        String password = "password";

        XMPPConnection connection1 = client1.connectToServerNoPort(servername);
        
        //try to login with a user that does not exist       
        assertFalse(client1.loginAccount(username, password, connection1));
        
        //create account and try to login again
        assertTrue(client1.createAccount(username, password, connection1));
        
        //logoff
        assertTrue(client1.logoff(connection1));
        
        //try to login again.  this time the user should exist
        connection1 = client1.connectToServerNoPort(servername);
        assertTrue(client1.loginAccount(username, password, connection1));
        
        assertTrue(client1.logoff(connection1));
        
        //now remove account
        connection1 = client1.connectToServerNoPort(servername);
        
        assertTrue(client1.loginAccount(username, password, connection1));
        //remove account and try to login again
        try {
            assertTrue(client1.removeAccount(connection1));
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        //logoff
        assertTrue(client1.logoff(connection1));
        
        connection1 = client1.connectToServerNoPort(servername);

        //try to see if account still exists

        assertFalse(client1.loginAccount(username, password, connection1));
        
        assertTrue(client1.logoff(connection1));
    }
    
    /**
     * test to add to roster, find user in roster, and remove from roster
     */
    public void testRoster(){
        
        String username = "user";
        String password = "password";
        
        //create initial jabber client
        JabberClient client = new JabberClient();
        XMPPConnection conn = client.connectToServerNoPort(servername);
        
        //create account
        assertTrue(client.createAccount(username, password, conn));
        
        assertTrue(client1.logoff(conn));
        
        conn = client.connectToServerNoPort(servername);
        
        assertTrue(client.loginAccount(username, password, conn));
        
        
        //add users to roster list
        client.addToRoster("testusername", "nickName", "friend", conn);
        client.addToRoster("testusername2", "nickName2", "friend", conn);
        client.addToRoster("testusername3", "nickName3", "friend", conn);
        
        assertTrue(client.findRosterUserName(conn, "testusername@"+servername));
        assertTrue(client.findRosterUserName(conn, "testusername2@"+servername));
        assertTrue(client.findRosterUserName(conn, "testusername3@"+servername));
        
        client.viewRoster(conn);
        
        client.removeFromRoster("testusername", "friend", conn);
        client.removeFromRoster("testusername2", "friend", conn);
        client.removeFromRoster("testusername3", "friend", conn);
        
       
        client.viewRoster(conn);
        
        assertFalse(client.findRosterUserName(conn, "testusername@"+servername));
        assertFalse(client.findRosterUserName(conn, "testusername2@"+servername));
        assertFalse(client.findRosterUserName(conn, "testusername3@"+servername));
        
        try {
            assertEquals(client.removeAccount(conn), true);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        
        assertEquals(client.logoff(conn), true);
        
    }
    
    public void testMessaging(){
        //need 2 clients
        XMPPConnection connection1 = client1.connectToServerNoPort(servername);
        client1.createAccount("user1", "password1", connection1);
        
        XMPPConnection connection2 = client2.connectToServerNoPort(servername);
        client2.createAccount("user2", "password2", connection2);
        
        
        try {
            assertEquals(client1.removeAccount(connection1), true);
            assertEquals(client1.removeAccount(connection2), true);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        
        //assertEquals(client.logoff(conn), true);
        
        
    }
    
//    public void testremove(){
//        String username = "user";
//        String password = "password";
//        
//        //create initial jabber client
//        JabberClient client = new JabberClient();
//        XMPPConnection conn = client.connectToServerNoPort(servername);
//        assertEquals(client.loginAccount(username, password, conn), true);
//        
//        client.removeFromRoster("testusername", "", conn);
//        client.removeFromRoster("testusername2", "", conn);
//        client.removeFromRoster("testusername3", "group", conn);
//        
//        try {
//            assertEquals(client.removeAccount(conn), true);
//        } catch (XMPPException e) {
//            e.printStackTrace();
//        }
//        
//        assertEquals(client.logoff(conn), true);
//    }
    
   
    

}
