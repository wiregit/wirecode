package org.limewire.swarm.file;

import java.io.IOException;

import org.apache.http.nio.ContentDecoder;

public interface SwarmFileWriter {

    long transferFrom(ContentDecoder decoder, long start) throws IOException;
    
    void finish();
    
    void initialize() throws IOException;

}
