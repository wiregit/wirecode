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


public class PGRPServerSocket{
    
        private static ServerSocket MyService = null;
        private static StringBuffer buffer = new StringBuffer(100);
        private int port = 9999;
        private XMPPConnection connection;
        private boolean passCheck = true;
        
        public PGRPServerSocket(XMPPConnection connection){           
            try {
                this.connection = connection;
                this.MyService = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("could not start server socket");
            }
            System.out.println("server socket started successfully");
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
            
            private void handleSocket(){

                //need a way to get username
                //1) get remote ip address
                //2) query database for username
                //3) store buddySession with username and socket
                if (mySocket!=null){
                    String remoteIPAddress = mySocket.getInetAddress().getHostAddress();
                    
                    //register IQ provider
                    ProviderManager providerManager = ProviderManager.getInstance();
                    providerManager.addIQProvider("serveripquery", "jabber:iq:serveripquery", new com.limegroup.gnutella.privategroups.ServerIPQueryProvider());
                    
                    
                    ServerIPQuery queryPacket = new ServerIPQuery(remoteIPAddress);
                    queryPacket.setTo("lw-intern02");
                    //queryPacket.setIPAddress(remoteIPAddress);
                    queryPacket.setType("GET");
                    
                    PacketFilter filter = new AndFilter(new PacketIDFilter(queryPacket.getPacketID()),
                            new PacketTypeFilter(IQ.class));
                    PacketCollector collector = conn.createPacketCollector(filter);

                    conn.sendPacket(queryPacket);
                    
                    IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
                    
                    if (result instanceof ServerIPQuery) {
                        ServerIPQuery data = (ServerIPQuery) result;
                        BuddyListManager.getInstance().addBuddySession(data.getUsername(), mySocket);
                    }
                }                
            }
            
        }
        
        public static void main(String[] args) {
            PGRPServerSocket socket = new PGRPServerSocket(new XMPPConnection("lw-intern02"));
            socket.start();
        }
}

    

