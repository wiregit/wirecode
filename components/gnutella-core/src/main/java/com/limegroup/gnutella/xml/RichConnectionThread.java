package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.*;
import java.util.*;
import java.io.*;

/**
 * Opens a special connection with a known server of the metadata
 * and send it the special query
 * @author  Sumeet Thadani (11/16/01)
 */
public class RichConnectionThread extends Thread{
    private String ipAddress;
    private QueryRequest query;
    private ActivityCallback callback;
    //constructor
    public RichConnectionThread(String ip, QueryRequest qr, 
                                                    ActivityCallback callback){
        this.ipAddress = ip;
        this.query = qr;
        this.callback = callback;
    }

    /**
     * opens a connection with the specified ip address and sends it a 
     * rich query request
     */
    public void run(){
        Connection c = new Connection(ipAddress,6346);//use default port
        QueryReply qr = null;
        try{
            c.initialize();//handshake
            c.send(qr);//send the query along
            c.flush();
        }catch(IOException ee){//could not send? return
            return;
        }
        byte[] queryGUID = query.getGUID();
        while(true){//keep receiving 'em 
            try{
                //lets give the server 10 seconds to respond
                qr = (QueryReply)c.receive(10000);
            }catch(Exception e){//exception? close connection and get out
                if(c.isOpen())
                    c.close();
                break;
            }
            //handle the query reply...
            //we know its for us...so we need not route...just consume it
            byte[] replyGUID = qr.getGUID();
            if(Arrays.equals(replyGUID,queryGUID))
                callback.handleQueryReply(qr);
        }
    }
}
