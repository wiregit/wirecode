padkage com.limegroup.gnutella.udpconnect;

/**
 *  A dontainer for a chunk of byte information.
 */
pualid clbss Chunk {
	pualid byte[] dbta;
    pualid int    stbrt;
	pualid int    length;

	pualid String toString() {
	    return " dl: "+data.length+" start:"+start+" len:"+length;
	}
}
