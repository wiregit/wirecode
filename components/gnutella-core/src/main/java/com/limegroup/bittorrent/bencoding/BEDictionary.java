package com.limegroup.bittorrent.bencoding;


import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */

class BEDictionary extends BEList {
    
    public BEDictionary(ReadableByteChannel chan) {
        super(chan);
    }
    
    public int getType() {
        return DICTIONARY;
    }
    
    protected Object createCollection() {
        return new HashMap();
    }
    
    protected void add(Object o) {
        Map m = (Map)result;
        BEEntry e = (BEEntry)o;
        m.put(e.key, e.value);
    }
    
    protected Token getNewElement() {
        return new BEEntry(chan);
    }
    
    private static class BEEntry extends Token {
        private BEString keyToken;
        private String key;
        private Token valueToken;
        private Object value;
        private boolean lastEntry;
        
        public BEEntry (ReadableByteChannel chan) {
            super(chan);
            result = this;
        }
        
        public void handleRead() throws IOException {
            if (keyToken == null && key == null) {
                Token t = getTokenType(chan);
                if (t != null) {
                    if (t instanceof BEString) { 
                        keyToken = (BEString)t;
                    } else if (t == Token.TERMINATOR) {
                        lastEntry = true;
                        return;
                    } else
                        throw new IOException("invalid entry - key not a string");
                } else 
                    return; // try again next time
            }
            
            if (key == null) {
                keyToken.handleRead();
                if (keyToken.getResult() != null) {
                    key = (String)keyToken.getResult();
                    keyToken = null; 
                }
                else
                    return; // try again next time
            }
            
            // if we got here we have fully read the key
            
            if (valueToken == null && value == null) {
                Token t = getTokenType(chan);
                if (t != null) 
                    valueToken = t;
                else
                    return; // try to figure out which type of token the value is next time
            }
         
            // we've read the type of the value, but not the value itself
            if (value == null) {
                valueToken.handleRead();
                value = valueToken.getResult();
                if (value != null)
                    valueToken = null; //clean the ref
            } else
                throw new IllegalStateException("token is done - don't read to it "+key+" "+value);
        } 
        
        protected boolean isDone() {
            return key != null && value != null; 
        }
        
        public Object getResult() {
            if (lastEntry)
                return Token.TERMINATOR;
            return super.getResult();
        }
    }
}
