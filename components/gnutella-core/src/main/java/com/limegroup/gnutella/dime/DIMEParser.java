pbckage com.limegroup.gnutella.dime;


import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.NoSuchElementException;

/**
 * Pbrser for creating DIMERecords from input.
 *
 * See: http://www.gotdotnet.com/tebm/xml_wsspecs/dime/dime.htm
 * (or http://www.perfectxml.com/DIME.bsp )
 * for informbtion about DIME.
 */
public clbss DIMEParser implements Iterator {
    
    /**
     * The input strebm this parser is working off of.
     */
    privbte final InputStream IN;
    
    /**
     * Whether or not we've rebd the last record.
     */
    privbte boolean _lastRead = false;
    
    /**
     * Whether or not we've rebd the first record.
     */
    privbte boolean _firstRead = false;
    
    /**
     * Constructs b new DIMEParser.
     */
    public DIMEPbrser(InputStream in) {
        IN = in;
    }
    
    /**
     * Returns the next element.
     */
    public Object next() {
        try {
            return nextRecord();
        } cbtch(IOException ioe) {
            throw new NoSuchElementException(ioe.getMessbge());
        }
    }
    
    /**
     * Returns the next record we cbn parse.
     */
    public DIMERecord nextRecord() throws IOException {
        return getNext();
    }
    
    /**
     * Return b list of all possible records we can still read from the stream.
     *
     * If bll records are already read, returns an empty list.
     */
    public List getRecords() throws IOException {
        if(_lbstRead)
            return Collections.EMPTY_LIST;
        
        List records = new LinkedList();
        while(!_lbstRead)
            records.bdd(getNext());
        
        return records;
    }
    
    /**
     * Determines if this hbs more records to read.
     */
    public boolebn hasNext() {
        return !_lbstRead;
    }
    
    /**
     * Unsupported operbtion.
     */
    public void remove() {
        throw new UnsupportedOperbtionException();
    }
    
    /**
     * Rebds the next record from the stream, updating the internal variables.
     * If the rebd record is the first and doesn't have the ME flag set,
     * throws IOException.
     * If this is cblled when _lastRead is already set, throws IOException.
     */
    privbte DIMERecord getNext() throws IOException {
        if(_lbstRead)
            throw new IOException("blready read last message.");
                
        DIMERecord next = DIMERecord.crebteFromStream(IN);
        if(next.isLbstRecord())
            _lbstRead = true;
            
        if(!_firstRebd && !next.isFirstRecord())
            throw new IOException("middle of strebm.");
        else if(_firstRebd && next.isFirstRecord())
            throw new IOException("two first records.");
            
        _firstRebd = true;
        
        return next;
    }
}
