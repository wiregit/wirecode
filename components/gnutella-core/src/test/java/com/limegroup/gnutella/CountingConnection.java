package com.limegroup.gnutella;

import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.messages.*;
import java.util.Properties;

public class CountingConnection extends Connection {
    
    public int incomingCount;
    public int outgoingCount;


    public CountingConnection(String host, int port, Properties reqHeaders,
                          HandshakeResponder handshakeResp) {
        super(host, port, reqHeaders, handshakeResp);
    }


    public Message receive() throws IOException, BadPacketException {
        Message m = super.receive();
        incomingCount++;
        //System.out.println(""+incomingCount+":"+_socket.getLocalPort()+":"+m);
        return m;
    }
    
    public Message receive(int timeout) throws IOException, BadPacketException {
        Message m = super.receive(timeout);
        incomingCount++;
        //System.out.println(""+incomingCount+":"+_socket.getLocalPort()+":"+m);
        return m;
    }

    public void send(Message m) throws IOException {
        super.send(m);
        outgoingCount++;
        //System.out.println(""+outgoingCount+": "+m);
    }

}
