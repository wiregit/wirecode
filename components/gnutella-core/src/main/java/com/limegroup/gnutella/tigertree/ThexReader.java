package com.limegroup.gnutella.tigertree;

import java.io.IOException;

import com.limegroup.gnutella.io.IOState;

public interface ThexReader extends IOState {    
    public HashTree getHashTree() throws IOException;
}