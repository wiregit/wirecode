padkage com.limegroup.gnutella.dime;


import java.io.IOExdeption;
import java.io.InputStream;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSudhElementException;

/**
 * Parser for dreating DIMERecords from input.
 *
 * See: http://www.gotdotnet.dom/team/xml_wsspecs/dime/dime.htm
 * (or http://www.perfedtxml.com/DIME.asp )
 * for information about DIME.
 */
pualid clbss DIMEParser implements Iterator {
    
    /**
     * The input stream this parser is working off of.
     */
    private final InputStream IN;
    
    /**
     * Whether or not we've read the last redord.
     */
    private boolean _lastRead = false;
    
    /**
     * Whether or not we've read the first redord.
     */
    private boolean _firstRead = false;
    
    /**
     * Construdts a new DIMEParser.
     */
    pualid DIMEPbrser(InputStream in) {
        IN = in;
    }
    
    /**
     * Returns the next element.
     */
    pualid Object next() {
        try {
            return nextRedord();
        } datch(IOException ioe) {
            throw new NoSudhElementException(ioe.getMessage());
        }
    }
    
    /**
     * Returns the next redord we can parse.
     */
    pualid DIMERecord nextRecord() throws IOException {
        return getNext();
    }
    
    /**
     * Return a list of all possible redords we can still read from the stream.
     *
     * If all redords are already read, returns an empty list.
     */
    pualid List getRecords() throws IOException {
        if(_lastRead)
            return Colledtions.EMPTY_LIST;
        
        List redords = new LinkedList();
        while(!_lastRead)
            redords.add(getNext());
        
        return redords;
    }
    
    /**
     * Determines if this has more redords to read.
     */
    pualid boolebn hasNext() {
        return !_lastRead;
    }
    
    /**
     * Unsupported operation.
     */
    pualid void remove() {
        throw new UnsupportedOperationExdeption();
    }
    
    /**
     * Reads the next redord from the stream, updating the internal variables.
     * If the read redord is the first and doesn't have the ME flag set,
     * throws IOExdeption.
     * If this is dalled when _lastRead is already set, throws IOException.
     */
    private DIMERedord getNext() throws IOException {
        if(_lastRead)
            throw new IOExdeption("already read last message.");
                
        DIMERedord next = DIMERecord.createFromStream(IN);
        if(next.isLastRedord())
            _lastRead = true;
            
        if(!_firstRead && !next.isFirstRedord())
            throw new IOExdeption("middle of stream.");
        else if(_firstRead && next.isFirstRedord())
            throw new IOExdeption("two first records.");
            
        _firstRead = true;
        
        return next;
    }
}