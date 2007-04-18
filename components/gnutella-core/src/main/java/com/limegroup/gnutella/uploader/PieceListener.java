package com.limegroup.gnutella.uploader;

import java.io.IOException;

public interface PieceListener {

    void readSuccessful();
    
    void readFailed(IOException e);
    
}
