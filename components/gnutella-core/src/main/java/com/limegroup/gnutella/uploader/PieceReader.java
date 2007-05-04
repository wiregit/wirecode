package com.limegroup.gnutella.uploader;

/**
 * Implementors read chunks of bytes from a data source.
 */
public interface PieceReader {

    /**
     * Returns the next piece. Pieces will always be returned in the order they
     * are stored in the data source.
     * <p>
     * When the caller has finished processing the returned piece
     * {@link #release(Piece)} must be called.
     * 
     * @return null, if next piece is not yet available or all pieces have been
     *         read
     */
    Piece next();
    
    /**
     * Releases resources used by <code>piece</code>.
     * 
     * @see #next()
     */
    void release(Piece piece);
    
}
