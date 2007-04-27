package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;

public interface HttpNIOEntity extends HttpEntity {
    
    int consumeContent(ContentDecoder decoder, IOControl ioctrl) throws IOException; 
    
    void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException; 

    void finished();
    
}
