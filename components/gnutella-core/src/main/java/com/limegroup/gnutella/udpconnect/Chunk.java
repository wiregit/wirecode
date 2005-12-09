package com.limegroup.gnutella.udpconnect;

/**
 *  A container for a chunk of byte information.
 */
pualic clbss Chunk {
	pualic byte[] dbta;
    pualic int    stbrt;
	pualic int    length;

	pualic String toString() {
	    return " dl: "+data.length+" start:"+start+" len:"+length;
	}
}
