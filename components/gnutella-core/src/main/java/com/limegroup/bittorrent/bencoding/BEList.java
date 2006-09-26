package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A token used to parse a bencoded list of elements.
 */
class BEList extends Token {
    /** Whether the token has been parsed completely */
    protected boolean done;
    
    /** The current element being parsed */
    protected Token currentElement;
    
    BEList(ReadableByteChannel chan) {
        super(chan);
        result = createCollection();
    }
    
    protected Object createCollection() {
        return new ArrayList();
    }
    
    protected void add(Object o) {
        ((List)result).add(o);
    }
    
    protected Token getNewElement() throws IOException {
        return getNextToken(chan);
    }
    
    public void handleRead() throws IOException {
        if (isDone())
            throw new IllegalStateException("token is done, don't read to it");
        while(true) {
            if (currentElement == null) 
                currentElement = getNewElement();
            
            if (currentElement == null)
                return;
            
            if (currentElement.getResult() == Token.TERMINATOR) {
                done = true;
                return;
            }
            
            
            currentElement.handleRead();
            Object result = currentElement.getResult();
            if (result == null)
                return;
            
            if (result == Token.TERMINATOR) {
                done = true;
                return;
            }
            
            add(result);
            currentElement = null;
        }
    }
    
    protected boolean isDone() {
        return done;
    }
    
    public int getType() {
        return LIST;
    }
}
