package com.limegroup.gnutella.privategroups;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.limewire.util.PrivateGroupsUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;


public class ServerSocketClass implements Runnable{
    
        private static ServerSocket MyService = null;
        private static StringBuffer buffer = new StringBuffer(100);
        private static BufferedReader is;
        private static PrintWriter os;
        private int port;
        private XMPPConnection conn;
        
        public ServerSocketClass(int port, XMPPConnection connection){
            this.port = port;
            this.conn = connection;
        }
        
        public StringBuffer getBuffer(){
            return buffer;
        }
        
        public ServerSocketClass(){
        }   
        
        public void appendBuffer(String msg){
            buffer.append(PrivateGroupsUtils.createMessage("remote user", msg).toXML());
        }
        
        public void emptyBuffer(){
            buffer.delete(0, buffer.length());
        }
        
        public static void setServerSocketAcceptTimeout(int time){
            try {
                //set server socket accept timeout
                MyService.setSoTimeout(time);
            } catch (SocketException e1) {
                e1.printStackTrace();
            }
        }

        public void run() {
            try{
                MyService = new ServerSocket(port);
                System.out.println("server socket started successfully");
            }
            catch(IOException e){
                System.out.println(e);
            }
            
            setServerSocketAcceptTimeout(1000);
            
            while(conn.isConnected()==true){
                
                try{
                    
                    Socket socketConnection = MyService.accept();
                    
                    //need a way to get username
                    //1) get remote ip address
                    //2) query database for username
                    //3) store buddySession with username and socket
                    if (socketConnection!=null){
                        String remoteIPAddress = socketConnection.getInetAddress().getHostAddress();
                        
                        //register IQ provider
                        ProviderManager providerManager = ProviderManager.getInstance();
                        providerManager.addIQProvider("serveripquery", "jabber:iq:serveripquery", new com.limegroup.gnutella.privategroups.ServerIPQueryProvider());
                        
                        
                        ServerIPQuery queryPacket = new ServerIPQuery();
                        queryPacket.setTo("lw-intern02");
                        queryPacket.setIPAddress(remoteIPAddress);
                        queryPacket.setType("GET");
                        
                        PacketFilter filter = new AndFilter(new PacketIDFilter(queryPacket.getPacketID()),
                                new PacketTypeFilter(IQ.class));
                        PacketCollector collector = conn.createPacketCollector(filter);
    
                        conn.sendPacket(queryPacket);
                        
                        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
                        
                        if (result instanceof ServerIPQuery) {
                            ServerIPQuery data = (ServerIPQuery) result;
                            System.out.println("REMOTE HOST RIGHT NOW IS : " + data.getUsername());
                            BuddyListManager.getInstance().addBuddySession(data.getUsername(), socketConnection);
                            BuddyListManager.getInstance().getSession(data.getUsername()).send(PrivateGroupsUtils.createMessage("lulu", "return msg"));
                        }
                    }
                    
                }
                catch(IOException e){

                } 
            }
            
            try {
                MyService.close();
                System.out.println("byebye serversocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
        public static void main(String[] args) {
            XMPPConnection conn = new XMPPConnection("lw-intern02");
            Thread t = new Thread(new ServerSocketClass(9999, conn));
            t.start();
            
        }
        public void initializeServerSocket(int port, XMPPConnection conn){
            
//            try{
//                MyService = new ServerSocket(port);
//                System.out.println("server socket started successfully");
//            }
//            catch(IOException e){
//                System.out.println(e);
//            }
//            
//            while(true){
//                
//                try{
//                    Socket socketConnection = MyService.accept();
//                    
//                    //need a way to get username
//                    //1) get remote ip address
//                    //2) query database for username
//                    //3) store buddySession with username and socket
//                   
//                    String remoteIPAddress = socketConnection.getInetAddress().getHostAddress();
//                    
//                    //register IQ provider
//                    ProviderManager providerManager = ProviderManager.getInstance();
//                    providerManager.addIQProvider("serveripquery", "jabber:iq:serveripquery", new com.limegroup.gnutella.privategroups.ServerIPQueryProvider());
//                    
//                    
//                    ServerIPQuery queryPacket = new ServerIPQuery();
//                    queryPacket.setTo("lw-intern02");
//                    queryPacket.setIPAddress(remoteIPAddress);
//                    queryPacket.setType("GET");
//                    
//                    PacketFilter filter = new AndFilter(new PacketIDFilter(queryPacket.getPacketID()),
//                            new PacketTypeFilter(IQ.class));
//                    PacketCollector collector = conn.createPacketCollector(filter);
//
//                    conn.sendPacket(queryPacket);
//                    
//                    
//                    IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
//                    
//                    if (result instanceof ServerIPQuery) {
//                        ServerIPQuery data = (ServerIPQuery) result;
//                        System.out.println("REMOTE HOST RIGHT NOW IS : " + data.getUsername());
//                        BuddyListManager.getInstance().addBuddySession(data.getUsername(), socketConnection);
//                    }
//                    
//                }
//                catch(IOException e){
//                    e.printStackTrace();
//                } 
//            }
        }
             
        private class ClientReader implements Runnable{

            private Socket clientReader;
            
            private ClientReader(Socket client){
                this.clientReader = client;
            }
            
            public void run() {
                try {
                    is = new BufferedReader(new InputStreamReader(clientReader.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                MXParser parser = new MXParser();
                while(true){
                    //text received from the client
                    try {
                        if(is.ready()){
                            Message parsedMsg = null; 
                            parser.setInput(is);
                            int eventType = parser.next();
                           
                            if (eventType == XmlPullParser.START_TAG){
                                if (parser.getName().equals("message")) {
                                    parsedMsg = (Message) PacketParserUtils.parseMessage(parser);
                                }
                                
                                System.out.println("From: " + parsedMsg.getFrom());
                                System.out.println("Body: " + parsedMsg.getBody());
                                
                                
                                buffer.append(PrivateGroupsUtils.createMessage("remote user", "return message").toXML());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        private class ClientWriter implements Runnable{
            
            private Socket clientWriter;
            
            private ClientWriter(Socket client){
                this.clientWriter = client;
                try {
                    os = new PrintWriter(clientWriter.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void run() {
                    
                while(true){
                    //if buffer is empty, do not write
                    while(buffer.length()!=0)
                    {
                        os.println(buffer);
                        buffer.delete(0, buffer.length());
                    }
                }
            }   
        }
        
        protected void finalize(){
            //Objects created in run method are finalized when
            //program terminates and thread exits
             try{
                MyService.close();
            } catch (IOException e) {
                System.out.println("Could not close socket");
                System.exit(-1);
            }
        }


        
    

}

    

