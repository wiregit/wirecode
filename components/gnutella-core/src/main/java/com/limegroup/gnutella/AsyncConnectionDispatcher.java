package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.limewire.nio.channel.AbstractChannelInterestReader;
import org.limewire.util.BufferUtils;

import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * A ConnectionDispatcher that reads asynchronously from the socket.
 */
public class AsyncConnectionDispatcher extends AbstractChannelInterestReader {
    private final Socket client;
    private final String allowedWord;
    private ConnectionDispatcher dispatcher;
    
    public AsyncConnectionDispatcher(ConnectionDispatcher dispatcher, Socket client, String allowedWord) {
        // + 1 for whitespace
        super(dispatcher.getMaximumWordSize() + 1);
        
        this.dispatcher = dispatcher;
        this.client = client;
        this.allowedWord = allowedWord;
    }
    
    public void shutdown() {
        super.shutdown();
        HTTPStat.CLOSED_REQUESTS.incrementStat();
    }

    public void handleRead() throws IOException {
        // Fill up our buffer as much we can.
        int read = 0;
        while(buffer.hasRemaining() && (read = source.read(buffer)) > 0);
        
        // See if we have a full word.
        for(int i = 0; i < buffer.position(); i++) {
            if(buffer.get(i) == ' ') {
                String word = new String(buffer.array(), 0, i);
                if(allowedWord != null && !allowedWord.equals(word))
                    throw new IOException("wrong word!");
                
                buffer.limit(buffer.position()).position(i+1);
                source.interestRead(false);
                dispatcher.dispatch(word, client, true);
                return;
            }
        }
        
        // If there's no room to read more or there's nothing left to read,
        // we aren't going to read our word.
        if(!buffer.hasRemaining() || read == -1)
            close();
    }
    
    public int read(ByteBuffer dst) {
        return BufferUtils.transfer(buffer, dst, false);
    }

    public long read(ByteBuffer [] dst) {
    	return BufferUtils.transfer(buffer, dst, 0, dst.length, false);
    }
}
