package com.limegroup.gnutella.privategroups;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.limewire.util.PrivateGroupsUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;


public class ServerSocketClass {
    
       private ServerSocket MyService = null;
        private static StringBuffer buffer = new StringBuffer(100);
        private static BufferedReader is;
        private static PrintWriter os;
        
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

        public void initializeServerSocket(int port){
            
            try{
                MyService = new ServerSocket(port);
                System.out.println("done");
            }
            catch(IOException e){
                System.out.println(e);
            }
            
            while(true){
                
                try{
                    Socket clientSocketConnection = MyService.accept();
                    
                    Runnable r = new ClientReader(clientSocketConnection);
                    Thread t = new Thread(r);
                    t.start();
                    
                    Runnable r2 = new ClientWriter(clientSocketConnection);
                    Thread t2 = new Thread(r2);
                    t2.start();
                    
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
             
        
        public static void main(String[] args) {
            ServerSocketClass serverSocket = new ServerSocketClass();
            serverSocket.initializeServerSocket(5222);
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
                                
                                
                                buffer.append(PrivateGroupsUtils.createMessage("remote user", "received").toXML());
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

    

