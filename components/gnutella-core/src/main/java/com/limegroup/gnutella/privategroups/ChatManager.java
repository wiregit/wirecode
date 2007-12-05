package com.limegroup.gnutella.privategroups;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.limewire.listener.Event;
import org.limewire.listener.EventListener;
import org.limewire.listener.WeakEventListenerList;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * 
 * ChatManager holds socket and threads for reading/writing
 *
 */
public class ChatManager{
    
    private Socket socket;
    private StringBuffer buffer = new StringBuffer(100);
    private BufferedReader is;
    private PrintWriter os;    
    private Thread readThread;
    private Thread writeThread;
    private final WeakEventListenerList<Event> listeners = new WeakEventListenerList<Event>();
    private PrivateGroupsScreen screen;
    private String localUsername;
    
  
    public ChatManager(String localUsername, Socket socket){
        this.socket = socket;
        this.localUsername = localUsername;
        
        initThreads(); 
    }
    
    private void handleEvent(Event E) {
        listeners.broadcast(E);
    }
    
    //initialize reading and writing threads
    private void initThreads(){
        
        readThread = new Thread(new ReaderThread(socket));
        readThread.start();
        
        writeThread = new Thread(new WriterThread(socket));
        writeThread.start();

    }
    
    public boolean closeChatManager(){
        //close socket
        try {
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    //to send a something, user simply appends to the buffer.  The writer thread will automatically send 
    //buffer data to the output stream
    public void send(Packet packet){
        
        
        if(packet instanceof Message)
        {
            Message msg = (Message) packet;
            
            openDialogScreen(msg.getTo());
            
            screen.printMessage(localUsername, msg.getBody());
        }
        
        appendBuffer(packet);
    } 
    
    private void appendBuffer(Packet packet){
        if(packet instanceof Message)
            buffer.append(packet.toXML());
    }
    
    private void emptyBuffer(){
        buffer.delete(0, buffer.length());
    }
    
    
    private class WriterThread implements Runnable{
        
        private Socket writeSocket;
        
        private WriterThread(Socket writeSocket){
            this.writeSocket = writeSocket;
            try {
                os = new PrintWriter(writeSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
           
            while(writeSocket.isConnected()){
                //if buffer is empty, do not write
                synchronized(buffer){
                    while(buffer.length()!=0)
                    {
                        os.println(buffer);
                        emptyBuffer();                        
                    }
                }
            }
        }   
    }
    
    private void openDialogScreen(String dialogName){
        if(screen == null){
            screen = new PrivateGroupsScreen(dialogName, localUsername, this);
            
            //register screen for new listener
            listeners.addListener(dialogName, screen);
        }
        
    }
    
    private class ReaderThread implements Runnable{
       
        private Socket readSocket;
        
        public ReaderThread(Socket readSocket){
            this.readSocket = readSocket;
            try {
                is = new BufferedReader(new InputStreamReader(readSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while(readSocket.isConnected()){
                try{      
                    MXParser parser = new MXParser();
                    //System.out.println("buffer text is : "+ is.readLine());
                    
                    synchronized(is){
                        if(is.ready()){
                        Message parsedMsg = null; 
                        
                        parser.setInput(is);
                        int eventType = parser.next();
                       
                            if (eventType == XmlPullParser.START_TAG){
                                if (parser.getName().equals("message")) {
                                    //parseMessage(parser) returns a SMACK message object
                                    parsedMsg = (Message) PacketParserUtils.parseMessage(parser);
                                }
                                
                                //broadcast event
                                openDialogScreen(parsedMsg.getFrom());
                                
                                handleEvent(new MessageEvent(parsedMsg, null));
                            }
                        }
                    }
                    //}
                }catch(IOException e){
                    //continue to loop for input
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("remote host has closed the connection!!");
                }
            }
        }
    }
}
