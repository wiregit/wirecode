package com.limegroup.gnutella.dime;


import com.sun.java.util.collections.List;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.NoSuchElementException;
import com.sun.java.util.collections.UnsupportedOperationException;
import com.sun.java.util.collections.Iterator;
import java.io.InputStream;
import java.io.IOException;

import com.limegroup.gnutella.util.DataUtils;

/**
 * Parser for creating DIMERecords from input.
 *
 * See: http://www.gotdotnet.com/team/xml_wsspecs/dime/dime.htm
 * (or http://www.perfectxml.com/DIME.asp )
 * for information about DIME.
 */
public class DIMEParser implements Iterator {
    
    /**
     * The input stream this parser is working off of.
     */
    private final InputStream IN;
    
    /**
     * Whether or not we've read the last record.
     */
    private boolean _lastRead = false;
    
    /**
     * Whether or not we've read the first record.
     */
    private boolean _firstRead = false;
    
    /**
     * Constructs a new DIMEParser.
     */
    public DIMEParser(InputStream in) {
        IN = in;
    }
    
    /**
     * Returns the next element.
     */
    public Object next() {
        try {
            return nextRecord();
        } catch(IOException ioe) {
            throw new NoSuchElementException(ioe.getMessage());
        }
    }
    
    /**
     * Returns the next record we can parse.
     *
     * @throws NoSuchElementException if _lastRead is already set.
     */
    public DIMERecord nextRecord() throws IOException, NoSuchElementException {
        return getNext();
    }
    
    /**
     * Return a list of all possible records we can still read from the stream.
     *
     * If all records are already read, returns an empty list.
     */
    public List getRecords() throws IOException {
        if(_lastRead)
            return DataUtils.EMPTY_LIST;
        
        List records = new LinkedList();
        while(!_lastRead)
            records.add(getNext());
        
        return records;
    }
    
    /**
     * Determines if this has more records to read.
     */
    public boolean hasNext() {
        return !_lastRead;
    }
    
    /**
     * Unsupported operation.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Reads the next record from the stream, updating the internal variables.
     * If the read record is the first and doesn't have the ME flag set,
     * throws IOException.
     * If this is called when _lastRead is already set, throws
     * NoSuchElementException.
     */
    private DIMERecord getNext() throws IOException, NoSuchElementException {
        if(_lastRead)
            throw new NoSuchElementException();
                
        DIMERecord next = DIMERecord.createFromStream(IN);
        if(next.isLastRecord())
            _lastRead = true;
            
        if(!_firstRead && !next.isFirstRecord())
            throw new IOException("middle of stream.");
            
        _firstRead = true;
        
        return next;
    }
}