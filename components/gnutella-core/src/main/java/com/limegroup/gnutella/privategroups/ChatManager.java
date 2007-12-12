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

import com.limegroup.gnutella.gui.GuiCoreMediator;

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
    private static final Log LOG = LogFactory.getLog(ChatManager.class);
    private String chatManagerKey;
    
    private LinkedBlockingQueue<Packet> linkedBlockingQueue= new LinkedBlockingQueue<Packet>();
    
  
    public ChatManager(Socket socket, String chatManagerKey){
        
        LOG.debug("ChatManager: constructor");
        this.socket = socket;
        this.chatManagerKey = chatManagerKey;
        initThreads(socket);

    }
    
    public void registerListener(String strongRef, EventListener listener){
        LOG.debug("ChatManager: registerListener");
        listeners.addListener(strongRef, listener);
    }
    
    public void removeListener(String strongRef, EventListener listener){
        listeners.removeListener(strongRef, listener);
    }
    
    
    private void handleEvent(Event E) {
        //can check if window exists
        LOG.debug("broadcast to listeners");
        listeners.broadcast(E);
    }
    
    //initialize reading and writing threads
    private void initThreads(Socket currentSocket){
        
        LOG.debug("ChatManager: initThreads");
        
        readThread = new Thread(new ReaderThread(currentSocket));
        readThread.start();
        
        writeThread = new Thread(new WriterThread(currentSocket));
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
    
    public void replaceSocket(Socket newSocket){
        closeChatManager();
        socket = newSocket;
        initThreads(socket);
    }
    
    //to send a something, user simply appends to the buffer.  The writer thread will automatically send 
    //buffer data to the output stream
    public void send(Packet packet){
        
        
        if(packet instanceof Message)
        {
            Message msg = (Message) packet;
            
            //this will print to the local msg window
            handleEvent(new MessageEvent(msg, null));
        }
        
        pushQueue(packet);
    } 
    
    private void pushQueue(Packet packet){
        linkedBlockingQueue.add(packet);
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
                    Packet popPacket = linkedBlockingQueue.take(); 
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
                    //System.out.println("buffer text is : "+ is.readLine());
                    
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
                                
                                handleEvent(new MessageEvent(parsedMsg, null));
                            }
                            else{
                                LOG.debug("EventType is not a START_TAG.  It is " + eventType);
                            }
                    }
                }catch(IOException e){
                    LOG.debug("Caught IOException in ReaderThread: run()");
                    //remote user disconnected so remove chatmanager
                    GuiCoreMediator.getPGRPClient().getBuddyListManager().removeChatManager(chatManagerKey);
                    
                } catch (XmlPullParserException e) {
                    LOG.debug("Caught XMLPullException in ReaderThread: run()");
                } catch (Exception e) {
                    LOG.debug("Caught Exception in ReaderThread: run()");
                }
            }
        }
    }
}
