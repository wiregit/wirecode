package com.limegroup.gnutella.dime;

import com.sun.java.util.collections.List;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.Iterator;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Parser for writing DIMERecords to a stream.
 *
 * See: http://www-106.ibm.com/developerworks/library/ws-dime/
 * (or http://www.perfectxml.com/DIME.asp )
 * for information about DIME.
 */
public class DIMEGenerator {
    
    private final List RECORDS = new LinkedList();
    
    /**
     * Adds the given record to the internal list of records.
     */
    public void add(DIMERecord record) {
        RECORDS.add(record);
    }
    
    /**
     * Writes the given list of DIMERecords to a stream.
     *
     * Does not do chunking.
     */
    public void write(OutputStream out) throws IOException {
        if(RECORDS.isEmpty())
            return;
        
        Iterator iter = RECORDS.iterator();
        int size = RECORDS.size();
        for(int i = 0; i < size; i++) {
            DIMERecord current = (DIMERecord)iter.next();
            if(i == 0)
                current.setFirstRecord(true);
            else
                current.setFirstRecord(false);

            if(i == size - 1)
                current.setLastRecord(true);
            else
                current.setLastRecord(false);
            current.write(out);
        }
    }
}