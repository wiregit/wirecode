package com.limegroup.gnutella.util;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.sun.java.util.collections.*;

public class HTTPHeaderValueSet implements HTTPHeaderValue {
    
    private Set _delegate;
    
    public HTTPHeaderValueSet() {
        _delegate = new HashSet();
    }
    
    public synchronized void clear() { _delegate.clear(); }

    public synchronized boolean contains(Object o) { 
        return _delegate.contains(o);
    }
     
    public synchronized boolean add(Object o) {
        Assert.that(o instanceof HTTPHeaderValue,"adding wrong type");
        return _delegate.add(o);
    }

    public synchronized boolean remove(Object o) {
        return _delegate.remove(o);
    }

    public Iterator iterator () {
        return _delegate.iterator();
    }

    public synchronized int size() { return _delegate.size(); }

    public synchronized String httpStringValue() {
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
