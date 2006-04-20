package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.ReadState;

public class AsyncDimeRecordReader extends ReadState {
    
    private static final Log LOG = LogFactory.getLog(AsyncDimeRecordReader.class);
    
    private ByteBuffer header;
    private ByteBuffer options;
    private ByteBuffer id;
    private ByteBuffer type;
    private ByteBuffer data;
    private ByteBuffer padding;
    
    public AsyncDimeRecordReader() {
        header = ByteBuffer.allocate(12);
        padding = ByteBuffer.allocate(4);
    }
    
    /**
     * Returns the next record if it can be constructed or null if it isn't
     * processed yet.
     * 
     * @return
     * @throws IOException
     */
    public DIMERecord getRecord() throws DIMEException {
        if(data == null || data.hasRemaining() || padding.hasRemaining()) {
            return null;
        } else {
            try {
            return new DIMERecord(header.get(0), header.get(1),
                                  options.array(), id.array(),
                                  type.array(), data.array());
            } catch(IllegalArgumentException iae) {
                throw new DIMEException(iae);
            }
        }
    }

    protected boolean processRead(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
        // NOTE: Fill is designed to skip padding, but because the header mod 4 == 0,
        //       no padding will ever be skipped for the header.
        fill(null, header, rc, buffer);
        // Header must be completely read before continuing...
        if(header.hasRemaining()) {
            LOG.debug("Header not full, leaving.");
            return true;
        }
        
        // If we haven't created things yet, create them.
        if(options == null)
            createOtherStructures();

        fill(null, options, rc, buffer);
        fill(options, id,   rc, buffer);
        fill(id, type,      rc, buffer);
        fill(type, data,    rc, buffer);
        fill(data, null,    rc, buffer);
        
        return data.hasRemaining() || padding.hasRemaining();
    }
    
    /**
     * Attempts to read as much data as possible into 'current', only if 'prior' is null
     * or has been filled.  Data will be transferred from 'buffer' into 'current' and then
     * read from 'channel' into 'current'.
     * 
     * This will also ensure that any data that should be skipped for 'prior' is skipped before
     * 'current' begins to be read.
     * 
     * An IOException will be thrown if more data needs to be read but the last read returned -1.
     * 
     * @param prior
     * @param current
     * @param rc
     * @param buffer
     * @throws IOException
     */
    private void fill(ByteBuffer prior, ByteBuffer current, ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
        if(prior != null && prior.hasRemaining()) {
            LOG.debug("Leaving because prior still needs data.");
            return;
        }
        
        int read = 0;
        
        if(prior != null) {
            int paddedSize = DIMERecord.calculatePaddingLength(prior.capacity());
            padding.limit(paddedSize); // it is okay to reset the limit.
            if(paddedSize != 0) {
                read = BufferUtils.readAll(rc, padding, buffer);
                LOG.debug("Filling padding of: " + paddedSize + ", left to pad: " + padding.remaining());
                if(padding.hasRemaining() && read == -1)
                    throw new IOException("EOF");
                if(padding.hasRemaining())
                    return;
            }
        }
        
        if(current != null) {    
            read = BufferUtils.readAll(rc, current, buffer);
            LOG.debug("Filling current.  Left: " + current.remaining());
            if(current.hasRemaining() && read == -1)
                throw new IOException("EOF");
            if(!current.hasRemaining())
                padding.clear();
        }
    }
    
    /**
     * Validates the header bytes & constructs options, id, type & data.
     * @throws IOException
     */
    private void createOtherStructures() throws DIMEException {
        try {
            DIMERecord.validateFirstBytes(header.get(0), header.get(1));
        } catch (IllegalArgumentException iae) {
            throw new DIMEException(iae);
        }

        byte[] headerArr = header.array();
        int optionsLength = ByteOrder.beb2int(headerArr, 2, 2);
        int idLength = ByteOrder.beb2int(headerArr, 4, 2);
        int typeLength = ByteOrder.beb2int(headerArr, 6, 2);
        int dataLength = ByteOrder.beb2int(headerArr, 8, 4);

        if(LOG.isDebugEnabled()) {
            LOG.debug("creating dime record." + 
                      "  optionsLength: " + optionsLength +
                      ", idLength: " + idLength +
                      ", typeLength: " + typeLength + 
                      ", dataLength: " + dataLength);
        }

        // The DIME specification allows this to be a 32-bit unsigned field,
        // which in Java would be a long -- but in order to hold the array
        // of the data, we can only read up to 16 unsigned bits (an int), in order
        // to size the array correctly.
        if (dataLength < 0)
            throw new DIMEException("data too big.");

        options = createBuffer(optionsLength);
        id      = createBuffer(idLength);
        type    = createBuffer(typeLength);
        data    = createBuffer(dataLength);
    }
    
    private ByteBuffer createBuffer(int length) {
        if(length == 0)
            return BufferUtils.getEmptyBuffer();
        else
            return ByteBuffer.allocate(length);
    }

    public long getAmountProcessed() {
        long read = header.position();
        read += count(options, id);
        read += count(id, type);
        read += count(type, data);
        read += count(data, null);
        return read;
    }
    
    private long count(ByteBuffer buffer, ByteBuffer next) {
        long count = 0;
        if(buffer != null) {
            count += buffer.position();
            if(!buffer.hasRemaining()) {
                if(next != null) {
                    if(next.position() == 0)
                        count += padding.position();
                    else
                        count += DIMERecord.calculatePaddingLength(buffer.capacity());
                } else {
                    count += padding.position();
                }
            }
        }
        return count;
    }

}
