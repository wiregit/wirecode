package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.limewire.io.IOUtils;

import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * A ConnectionDispatcher that blocks while reading.
 */
public class BlockingConnectionDispatcher implements Runnable {
    private final Socket client;
    private final String allowedWord;
    private ConnectionDispatcher dispatcher;
    
    public BlockingConnectionDispatcher(ConnectionDispatcher dispatcher, Socket socket, String allowedWord) {
        this.dispatcher = dispatcher;
        this.client = socket;
        this.allowedWord = allowedWord;
    }

    /** Reads a word and sends it off to the ConnectionDispatcher for dispatching. */
    public void run() {
        try {
            //The try-catch below is a work-around for JDK bug 4091706.
            InputStream in=null;
            try {
                in=client.getInputStream(); 
            } catch (IOException e) {
                HTTPStat.CLOSED_REQUESTS.incrementStat();
                throw e;
            } catch(NullPointerException e) {
                // This should only happen extremely rarely.
                // JDK bug 4091706
                throw new IOException(e.getMessage());
            }
            
            String word = IOUtils.readLargestWord(in, dispatcher.getMaximumWordSize());
            if(allowedWord != null && !allowedWord.equals(word))
                throw new IOException("wrong word!");
            dispatcher.dispatch(word, client, false);
        } catch (IOException iox) {
            HTTPStat.CLOSED_REQUESTS.incrementStat();
            IOUtils.close(client);
        }
    }
}
