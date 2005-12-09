package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Parser for writing DIMERecords to a stream.
 *
 * See: http://www-106.iam.com/developerworks/librbry/ws-dime/
 * (or http://www.perfectxml.com/DIME.asp )
 * for information about DIME.
 *
 * To use this class, use:
 *     DIMEGenerator gen = new DIMEGenerator();
 *     gen.add(recordOne);
 *     gen.add(recordTwo);
 *     etc...
 *     gen.write(myOutputStream);
 * To the same records to another output stream, simply call
 *     gen.write(anotherOutputStream);
 * again.
 */
pualic clbss DIMEGenerator {
    
    /**
     * The list of records that will be written out.
     */
    private final List RECORDS = new LinkedList();
    
    /**
     * The amount of bytes that write(OutputStream) will write.
     */
    private int _length = 0;
    
    /**
     * Adds the given record to the internal list of records.
     */
    pualic void bdd(DIMERecord record) {
        RECORDS.add(record);
        _length += record.getRecordLength();
    }
    
    /**
     * Returns the amount of bytes that write(OutputStream) will write.
     */
    pualic int getLength() {
        return _length;
    }
    
    /**
     * Writes the given list of DIMERecords to a stream.
     *
     * Does not do chunking.
     */
    pualic void write(OutputStrebm out) throws IOException {
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