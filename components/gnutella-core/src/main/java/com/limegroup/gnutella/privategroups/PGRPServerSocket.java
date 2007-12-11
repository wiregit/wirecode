package com.limegroup.gnutella.privategroups;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.privategroups.RosterListMediator;


/**
 * The PGRPServerSocket handles an incoming socket connection.  Once the handler accepts the incoming client
 * socket connection, the handler extracts the ip address and queries the server to get the corresponding username.
 * The username and socket are stored in the singleton BuddyListManager.
 *
 */
public class PGRPServerSocket{
    
        private ServerSocket MyService = null;
        private StringBuffer buffer = new StringBuffer(100);
        private int port = 9999;
        private XMPPConnection connection;
        private boolean passCheck = true;
        private BuddyListManager buddyListManager;
        private static String servername = GuiCoreMediator.getPGRPClient().getServername();
        private String localUsername;
        
        public PGRPServerSocket(String localUsername, XMPPConnection connection, BuddyListManager buddyListManager){           
            try {
                this.connection = connection;
                this.MyService = new ServerSocket(port);
                this.buddyListManager = buddyListManager;
                this.localUsername = localUsername;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("could not start server socket");
            }
            System.out.println("server socket started successfully");
        }
        
        public void setServerName(String name){
            servername = name;
        }
        
        public StringBuffer getBuffer(){
            return buffer;
        }
        
        public void start(){
            
            new Thread(new Runnable(){ 
                    public void run(){
                        while(passCheck){
                            
                            try {
                                new SocketHandler(MyService.accept(), connection).handleSocket();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            
                        }   
                    }
                }).start();   
        }
        
        public boolean closeSocket(){
            try {
                passCheck = false;
                MyService.close();
                
            } catch (IOException e) {
                return false;
            }
            return true;
        }
        
        
        private class SocketHandler{
            
            private Socket mySocket;
            private XMPPConnection conn;
            
            private SocketHandler(Socket mySocket, XMPPConnection conn){
                this.mySocket = mySocket; 
                this.conn = conn;
            }
            
            //need a way to get username
            //1) get remote ip address
            //2) query database for username
            //3) store buddySession with username and socket
            private void handleSocket(){
                ServerIPQuery data;
                
                if (mySocket!=null){
                    String remoteIPAddress = mySocket.getInetAddress().getHostAddress();
                    
                    //register IQ provider
                    ProviderManager providerManager = ProviderManager.getInstance();
                    providerManager.addIQProvider("serveripquery", "jabber:iq:serveripquery", new com.limegroup.gnutella.privategroups.ServerIPQueryProvider());
                    
                    System.out.println("remote ip address is: " + remoteIPAddress);
                    
                    
                    ServerIPQuery queryPacket = new ServerIPQuery(remoteIPAddress);
                    queryPacket.setTo(servername);
                    queryPacket.setType(IQ.Type.GET);
                    
                    PacketFilter filter = new AndFilter(new PacketIDFilter(queryPacket.getPacketID()),
                            new PacketTypeFilter(IQ.class));
                    PacketCollector collector = conn.createPacketCollector(filter);

                    conn.sendPacket(queryPacket);
                    
                    IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
                    
                    if (result instanceof ServerIPQuery) {
                        data = (ServerIPQuery) result;
                        String usernameExt = data.getUsername()+ "@" + servername;
                        
                        //check if existing chatmanager already exists
                        
                        
                        buddyListManager.addChatManager("felix@lw-intern02", localUsername, mySocket);
                        System.out.println("got a conversation request from: " + "felix@lw-intern02" + ". let's open gui window now");
                        
                        
//                        buddyListManager.addChatManager(usernameExt, localUsername, mySocket);
//                        System.out.println("got a conversation request from: " + usernameExt + ". let's open gui window now");
//                        RosterListMediator.getInstance().initMessageWindow(usernameExt, localUsername);
                    }
                }     
            }    
        }
}

    

