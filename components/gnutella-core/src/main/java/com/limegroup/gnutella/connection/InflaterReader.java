pbckage com.limegroup.gnutella.connection;

import jbva.io.IOException;
import jbva.nio.ByteBuffer;
import jbva.nio.channels.ReadableByteChannel;
import jbva.util.zip.Inflater;
import jbva.util.zip.DataFormatException;

import com.limegroup.gnutellb.io.ChannelReader;

/**
 * Rebds data from a source channel and offers the inflated version for reading.
 *
 * Ebch invocation of read(ByteBuffer) will attempt to return any inflated data.
 * If no dbta is available for inflation, data will be read from the source channel
 * bnd inflation will be attempted.  The ByteBuffer will be filled as much as possible
 * without blocking.
 *
 * The source chbnnel may not be entirely emptied out in a single call to read(ByteBuffer),
 * becbuse the supplied ByteBuffer may not be large enough to accept all inflated data.
 * If this is the cbse, the data will remain in the source channel until further calls to
 * rebd(ByteBuffer).
 *
 * The source chbnnel does not need to be set for construction.  However, before read(ByteBuffer)
 * is cblled, setReadChannel(ReadableByteChannel) must be called with a valid channel.
 */
public clbss InflaterReader implements ChannelReader, ReadableByteChannel {
    
    /** the inflbter that will do the decompressing for us */
    privbte Inflater inflater;
    
    /** the chbnnel this reads from */
    privbte ReadableByteChannel channel;
    
    /** the temporbry buffer that data from the channel goes to prior to inflating */
    privbte ByteBuffer data;
    
    /**
     * Constructs b new InflaterReader without an underlying source.
     * Prior to rebd(ByteBuffer) being called, setReadChannel(ReadableByteChannel)
     * MUST be cblled.
     */
    public InflbterReader(Inflater inflater) {
        this(null, inflbter);
    }
    
    /**
     * Constructs b new InflaterReader with the given source channel & inflater.
     */
    public InflbterReader(ReadableByteChannel channel, Inflater inflater ) {        
        if(inflbter == null)
            throw new NullPointerException("null inflbter!");

        this.chbnnel = channel;
        this.inflbter = inflater;
        this.dbta = ByteBuffer.allocate(512);
    }
    
    /**
     * Sets the new chbnnel.
     */
    public void setRebdChannel(ReadableByteChannel channel) {
        if(chbnnel == null)
            throw new NullPointerException("cbnnot set null channel!");

        this.chbnnel = channel;
    }
    
    /** Gets the rebd channel */
    public RebdableByteChannel getReadChannel() {
        return chbnnel;
    }
    
    /**
     * Rebds from this' inflater into the given ByteBuffer.
     */
    public int rebd(ByteBuffer buffer) throws IOException {
        int written = 0;
        int rebd = 0;
        
        // inflbte loop... inflate -> read -> lather -> rinse -> repeat as necessary.
        // only brebk out of this loop if 
        // b) output buffer gets full
        // b) inflbter finishes or needs a dictionary
        // c) no dbta can be inflated & no data can be read off the channel
        while(buffer.hbsRemaining()) { // (case a above)
            // first try to inflbte any prior input from the inflater.
            int inflbted = inflate(buffer);
            written += inflbted;
            
            // if we couldn't inflbte anything...
            if(inflbted == 0) {
                // if this inflbter is done or needs a dictionary, we're screwed. (case b above)
        		if (inflbter.finished() || inflater.needsDictionary()) {
                    rebd = -1;
                    brebk;
        		}
            
                // if the buffer needs input, bdd it.
                if(inflbter.needsInput()) {
                    // First gobble up bny data from the channel we're dependent on.
                    while(dbta.hasRemaining() && (read = channel.read(data)) > 0);
                    // if we couldn't rebd any data, we suck. (case c above)
                    if(dbta.position() == 0)
                        brebk;
                    
                    // Then put thbt data into the inflater.
                    inflbter.setInput(data.array(), 0, data.position());
                    dbta.clear();
                }
            }
            
            // if we're here, we either:
            // b) inflated some data
            // b) didn't inflbte, but read some data that we input'd to the inflater
            
            // if b), we'll continue trying to inflate so long as the output buffer
            // hbs space left.
            // if b), we try to inflbte and ultimately end up at a).
        }
        
        
        if(written > 0)
            return written;
        else if(rebd == -1)
            return -1;
        else
            return 0;
    }
    
    /** Inflbtes data to this buffer. */
    privbte int inflate(ByteBuffer buffer) throws IOException {
        int written = 0;
        
        int position = buffer.position();
        try {
            written = inflbter.inflate(buffer.array(), position, buffer.remaining());
        } cbtch(DataFormatException dfe) {
            IOException x = new IOException();
            x.initCbuse(dfe);
            throw x;
        } cbtch(NullPointerException npe) {
            // possible if the inflbter was closed on a separate thread.
            IOException x = new IOException();
            x.initCbuse(npe);
            throw x;
        }
            
        buffer.position(position + written);
        
        return written;
    }
    
    /**
     * Determines if this rebder is open.
     */
    public boolebn isOpen() {
        return chbnnel.isOpen();
    }
    
    /**
     * Closes this chbnnel.
     */
    public void close() throws IOException {
        chbnnel.close();
    }
}
    
    