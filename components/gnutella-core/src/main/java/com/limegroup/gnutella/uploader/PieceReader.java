package com.limegroup.gnutella.uploader;

public interface PieceReader {

    Piece next();
    
    void release(Piece piece);
    
    void suspend();
    
    void resume();
    
    void shutdown();
    
}
