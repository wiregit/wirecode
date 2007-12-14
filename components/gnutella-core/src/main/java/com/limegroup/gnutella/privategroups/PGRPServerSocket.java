package com.limegroup.gnutella.privategroups;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;


/**
 * The PGRPServerSocket handles an incoming socket connection.  Once the handler accepts the incoming client
 * socket connection, the handler extracts the ip address and queries the server to get the corresponding username.
 * The username and socket are stored in the singleton BuddyListManager.
 *
 */
public class PGRPServerSocket{
    
        private ServerSocket MyService = null;
        private int port = 9999;
        private XMPPConnection connection;
        private boolean passCheck = true;
        private BuddyListManager buddyListManager;
        private static String servername;
        private String localUsername;
        private static final Log LOG = LogFactory.getLog(BuddyListManager.class);
        
        public PGRPServerSocket(String localUsername, String servername, XMPPConnection connection, BuddyListManager buddyListManager){           
            try {
                LOG.debug("PGRPServerSocket:constructor");
                this.connection = connection;
                this.MyService = new ServerSocket(port);
                this.buddyListManager = buddyListManager;
                this.localUsername = localUsername;
                this.servername = servername;
                
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("could not start server socket");
            }
            System.out.println("server socket started successfully");
        }
        
        public void start(){
            LOG.debug("PGRPServerSocket:start server socket()");
            
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
                LOG.debug("SocketHandler:handleSocket");
                ServerIPQuery data;
                
                if (mySocket!=null){
                    LOG.debug("got a new socket");
                    String remoteIPAddress = mySocket.getInetAddress().getHostAddress();
                    
                    LOG.debug("register IQ ServerIPQuery Provider");
                    //register IQ provider
                    ProviderManager providerManager = ProviderManager.getInstance();
                    providerManager.addIQProvider("serveripquery", "jabber:iq:serveripquery", new com.limegroup.gnutella.privategroups.ServerIPQueryProvider()); 
                    
                    
                    ServerIPQuery queryPacket = new ServerIPQuery(remoteIPAddress);
                    queryPacket.setTo(servername);
                    queryPacket.setType(IQ.Type.GET);
                    
                    
                    PacketFilter filter = new AndFilter(new PacketIDFilter(queryPacket.getPacketID()),
                            new PacketTypeFilter(IQ.class));
                    PacketCollector collector = conn.createPacketCollector(filter);

                    conn.sendPacket(queryPacket);
                    
                    LOG.debug("get result back from server");
                    IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
                    
                    if (result instanceof ServerIPQuery) {
                        LOG.debug("result is of type ServerIPQuery");
                        data = (ServerIPQuery) result;
                        
                        //need to concatenate the servername with the username because the server returns the username only
                        String usernameExt = data.getUsername()+ "@" + servername; 
                        
                        LOG.debug("let's add a chatmanager now");                    
                        
                        buddyListManager.addChatManager(usernameExt, localUsername, mySocket);
                        System.out.println("got a conversation request from: " + usernameExt + ". let's open gui window now");
                        LOG.debug("end of SocketHandler");
                    }
                }     
            }    
        }
}

    

