package com.limegroup.gnutella.http;

import java.util.Collection;

public class HTTPHeaderValueCollection implements HTTPHeaderValue {
    
    private final Collection<? extends HTTPHeaderValue> _delegate;
    
    public HTTPHeaderValueCollection(Collection<? extends HTTPHeaderValue> d) {
        _delegate = d;
    }

    public String httpStringValue() {
        final String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		boolean wrote = false;
        
        for(HTTPHeaderValue value : _delegate) {
            writeBuffer.append(value.httpStringValue()).append(commaSpace);
            wrote = true;
        }
        
		// Truncate the last comma from the buffer.
		// This is arguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-2);		    
		return writeBuffer.toString();
    }

}
