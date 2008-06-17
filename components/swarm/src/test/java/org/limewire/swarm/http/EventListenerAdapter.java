package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.EventListener;

public class EventListenerAdapter implements EventListener {

    public void connectionClosed(NHttpConnection conn) {
        // TODO Auto-generated method stub
        
    }

    public void connectionOpen(NHttpConnection conn) {
        // TODO Auto-generated method stub
        
    }

    public void connectionTimeout(NHttpConnection conn) {
        // TODO Auto-generated method stub
        
    }

    public void fatalIOException(IOException ex, NHttpConnection conn) {
        // TODO Auto-generated method stub
        
    }

    public void fatalProtocolException(HttpException ex, NHttpConnection conn) {
        // TODO Auto-generated method stub
        
    }

}
