package com.limegroup.gnutella.privategroups;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.limewire.util.PrivateGroupsUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ClientSocket {
    
    private String ipaddress;
    private int portNumber;
    private Socket clientSocketConnection;
    private String localUsername;
    
    
    public ClientSocket(String ipaddress, int portNumber, String localUsername){
        this.ipaddress = ipaddress;
        this.portNumber = portNumber;
        this.localUsername = localUsername;
       
    }

    public void createClientConnection(){
        try {
               clientSocketConnection = new Socket(ipaddress, portNumber);
               System.out.println("connected");
                             
               //execute thread for receiving input
               Runnable r = new ReceiveMessage(clientSocketConnection);
               Thread t = new Thread(r);
               t.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
    
    /**
     *  Create message object - for extensibility, it's possible to create other methods such as sendFile, 
     *  sendPresence etc. to create diff. objects
     */
    public void sendMessage(String message){
        
        PrintWriter out;
        try {

            if(clientSocketConnection != null){          
                out = new PrintWriter(clientSocketConnection.getOutputStream(), true);
                
                //a method that constructs a SMACK message object and initializes the object properties
                Message msg = PrivateGroupsUtils.createMessage(localUsername, "yay! this message was sent successfully");
                out.println(msg.toXML());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private class ReceiveMessage implements Runnable{
        
        private BufferedReader in;
        private Socket myConnection;
        
        public ReceiveMessage(Socket myConnection){
            this.myConnection = myConnection;
            try {
                this.in = new BufferedReader(new InputStreamReader(myConnection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            
            while(true){
                try{      
                    MXParser parser = new MXParser();
                    if(in.ready()){
                        
                        Message parsedMsg = null; 
                        IQ newIQ = null;
                        
                        parser.setInput(in);
                        int eventType = parser.next();
                       
                        if (eventType == XmlPullParser.START_TAG){
                            if (parser.getName().equals("message")) {
                                //parseMessage(parser) returns a SMACK message object
                                parsedMsg = (Message) PacketParserUtils.parseMessage(parser);
                            }
                            
                            //now with the SMACK message object, we can extract values that we want eg. sender, body text
                            System.out.println("From: " + parsedMsg.getFrom());
                            System.out.println("Body: " + parsedMsg.getBody());
                        }
                    }
                }catch(IOException e){
                    //continue to loop for input
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        //ClientSocket clientSocket = new ClientSocket("69.201.186.164", 9999, "Anthony");
        
        ClientSocket clientSocket = new ClientSocket("127.0.0.1", 9999, "Anthony");
        clientSocket.createClientConnection();
        clientSocket.sendMessage("Greetings");
    }
    
//    /**
//     *  1) send own user name
//     *  2) gets public key from remote user
//     *  3) generate keys and send own public key and ip to remote user
//     *  4) gets encrypted ack signal, and sends ack signal to remote user with remote user pub key
//     */
//    public void authenticateUser(){
//        PrintWriter out;
//        try {
//            if(clientSocketConnection != null){          
//                out = new PrintWriter(clientSocketConnection.getOutputStream(), true);
//                
//                //send own username
//                out.println("username");
//                out.println(localUsername);
//                
//                //gets public key from remote user
//                while(remotePublicKey==null){}
//                
//                if(remotePublicKey!=null){
//                    
//                    //send own public key, modulus, and ip to remote user
//                    out.println("public key");
//                    out.println(localPublicKey);
//                    
//                    out.println("modulus");
//                    out.println(localModulus);
//                    
//                    //encrypt with remote user ip
//                    
//                    out.println("ip");
//                    System.out.println(remotePublicKey);
//                    System.out.println(remoteModulus);
//                    out.println(rsaKey.encrypt(new BigInteger("123456789"), remotePublicKey, remoteModulus));
//                    
//                    while(!ack)
//                    
//                    out.println("ack");
//                    setAuthenticated(true);
//                    System.out.println(authenticated);
//                    
//                    System.out.println("done in client");
//                        
//                }
//            }
//        
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        
//    }
    
    
    protected void finalize() throws IOException{
        clientSocketConnection.close();
   
    }

    
//    public static void main(String[] args) {
//        try {
//     // Create client SocketChannel
//        SocketChannel client = SocketChannel.open();
//
//        // nonblocking I/O
//        client.configureBlocking(false);
//
//        // Connection to host port 9999
//        client.connect(new java.net.InetSocketAddress("69.201.186.164",9999));
//
//        // Create selector
//        Selector selector = Selector.open();
//
//        // Record to selector (OP_CONNECT type)
//        SelectionKey clientKey = client.register(selector, SelectionKey.OP_CONNECT);
//
//        // Waiting for the connection
//        
//            while (selector.select(5000)> 0) {
//              System.out.println("connected");
//
//              // Get keys
//              Set keys = selector.selectedKeys();
//              Iterator i = keys.iterator();
//
//              // For each key...
//              while (i.hasNext()) {
//                SelectionKey key = (SelectionKey)i.next();
//
//                // Remove the current key
//                i.remove();
//
//                // Get the socket channel held by the key
//                SocketChannel channel = (SocketChannel)key.channel();
//
//                // Attempt a connection
//                if (key.isConnectable()) {
//
//                  // Connection OK
//                  System.out.println("Server Found");
//
//                  // Close pendent connections
//                  if (channel.isConnectionPending())
//                    channel.finishConnect();
//                  
//                  System.out.println("Connected to server " + channel);
//
//                  // Write continuously on the buffer
//                  ByteBuffer buffer = null;
//                  while(true){
//                    buffer = 
//                      ByteBuffer.wrap(
//                        new String("Lime Wire LLC").getBytes());
//                   
//                    channel.write(buffer);
//                    buffer.clear();
//                  }
//                }
//              }
//            }
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        
//        System.out.println("done");
//        
//    }
    
    
}
