package com.limegroup.gnutella.privategroups;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private BufferedReader is;
    private PrintWriter os;    
    private Thread readThread;
    private Thread writeThread;
    private final WeakEventListenerList<Event> listeners = new WeakEventListenerList<Event>();
    private static final Log LOG = LogFactory.getLog(ChatManager.class);
    private String chatManagerKey;
    private boolean remoteWindowExists = true;
    private BuddyListManager buddyListManager;
    private LinkedBlockingQueue<Packet> writerLinkedBlockingQueue= new LinkedBlockingQueue<Packet>();
    
    public void setRemoteWindowExists(boolean existence){
        remoteWindowExists = existence;
    }
    
    public ChatManager(BuddyListManager buddyListManager, Socket socket, String chatManagerKey){
        
        LOG.debug("ChatManager: constructor");
        this.socket = socket;
        this.buddyListManager = buddyListManager;
        this.chatManagerKey = chatManagerKey;
    }
    
    public void registerListener(Object strongRef, EventListener listener){
        LOG.debug("ChatManager: registerListener");
        listeners.addListener(strongRef, listener);
    }
    
    public void removeListener(Object strongRef, EventListener listener){
        listeners.removeListener(strongRef, listener);
    }
    
    public boolean checkIfRemoteWindowExists(){
        return remoteWindowExists;
    }
    
    //initialize reading and writing threads
    public void initReadWriteThreads(){
        
        LOG.debug("ChatManager: initThreads");
        
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
    
    //replaces the socket only.  the chat window already exists.
    public void replaceSocket(Socket newSocket){
        closeChatManager();
        socket = newSocket;
        initReadWriteThreads();
    }
    
    //to send a something, user simply appends to the buffer.  The writer thread will automatically send 
    //buffer data to the output stream
    public void send(Packet packet){
        
        if(packet instanceof Message)
        {
            Message msg = (Message) packet;
            
            //this will print to the local msg window
            listeners.broadcast(new MessageEvent(msg, null));
        }
        pushQueue(packet);
    } 
    
    private void pushQueue(Packet packet){
        writerLinkedBlockingQueue.add(packet);
    }

    private class WriterThread implements Runnable{
        
        private Socket writeSocket;
        
        private WriterThread(Socket writeSocket) {
            LOG.debug("WriterThread initialization");
            this.writeSocket = writeSocket;
            try {
                LOG.debug("establish PrintWriter for writer thread");
                os = new PrintWriter(writeSocket.getOutputStream(), true);
            } catch (IOException e) {
                LOG.debug("got a problem with establishing PrintWriter");
                e.printStackTrace();
            }
        }

        public void run() {
           
            while(!writeSocket.isClosed()){
                LOG.debug("WriterThread: run()");
                //if buffer is empty, do not write
                try {
                    Packet popPacket = writerLinkedBlockingQueue.take(); 
                    if(popPacket instanceof Message){
                        String xml = popPacket.toXML();
                        os.println(xml);
                        
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }   
    }
    
    @Override
    protected void finalize() throws Throwable {
        // TODO Auto-generated method stub
        super.finalize();
        
        System.out.println("I AM FINALIZING!!");
        Thread.dumpStack();
    }
    
    private class ReaderThread implements Runnable{
       
        private Socket readSocket;
        
        public ReaderThread(Socket readSocket){
            LOG.debug("ReaderThread initialization");
            this.readSocket = readSocket;

            try {
                LOG.debug("establish BufferedReader for reader thread");
                is = new BufferedReader(new InputStreamReader(readSocket.getInputStream()));
            } catch (IOException e) {
                LOG.debug("got a problem with establishing BufferedReader");
                e.printStackTrace();
            }
        }

        public void run() {
            LOG.debug("ReaderThread: run()");
            while(!readSocket.isClosed()){
                LOG.debug("socket is not closed - run while loop");

                try{      
                    MXParser parser = new MXParser();
                    
                    synchronized(is){
                        Message parsedMsg = null; 
                        
                        parser.setInput(is);
                        int eventType = parser.next();
                       
                        if (eventType == XmlPullParser.START_TAG){
                            LOG.debug("got a START tag in the XML message - there is something to parse");
                            if (parser.getName().equals("message")) {
                                LOG.debug("got a message object - begin parsing");
                                //parseMessage(parser) returns a SMACK message object
                                parsedMsg = (Message) PacketParserUtils.parseMessage(parser);
                                LOG.debug("done parsing message object");
                            }
                            listeners.broadcast(new MessageEvent(parsedMsg, null));
                        }
                        else{
                            LOG.debug("EventType is not a START_TAG.  It is " + eventType);
                        }
                    }
                }catch(IOException e){
                    LOG.debug("Socket Exception: caused by the remote user closing their window");
                    //remote user disconnected so remove chatmanager.  check if manager exists (exists if the window has not been closed)
                    if(buddyListManager.getManager(chatManagerKey)!= null){
                        remoteWindowExists = false;
                        break;
                    }
                } catch (XmlPullParserException e) {
                    LOG.debug("Caught XMLPullException in ReaderThread: run()");
                } catch (Exception e) {
                    LOG.debug("Caught Exception in ReaderThread: run()");
                }
            }
        }
    }
}
