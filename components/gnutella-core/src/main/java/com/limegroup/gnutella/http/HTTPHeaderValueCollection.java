package com.limegroup.gnutella.http;

import java.util.Collection;
import java.util.Iterator;

public class HTTPHeaderValueCollection implements HTTPHeaderValue {
    
    private Collection _delegate;
    
    public HTTPHeaderValueCollection(Collection d) {
        _delegate = d;
    }

    public String httpStringValue() {
        final String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		boolean wrote = false;
        Iterator iter = _delegate.iterator();
        while(iter.hasNext()) {
            writeBuffer.append((
                           (HTTPHeaderValue)iter.next()).httpStringValue());
            writeBuffer.append(commaSpace);
            wrote = true;
        }
		// Truncate the last comma from the buffer.
		// This is arguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-2);		    
		return writeBuffer.toString();
    }

}
