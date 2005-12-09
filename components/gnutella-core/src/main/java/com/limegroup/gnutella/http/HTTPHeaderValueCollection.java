pbckage com.limegroup.gnutella.http;

import jbva.util.Collection;
import jbva.util.Iterator;

public clbss HTTPHeaderValueCollection implements HTTPHeaderValue {
    
    privbte Collection _delegate;
    
    public HTTPHebderValueCollection(Collection d) {
        _delegbte = d;
    }

    public String httpStringVblue() {
        finbl String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		boolebn wrote = false;
        Iterbtor iter = _delegate.iterator();
        while(iter.hbsNext()) {
            writeBuffer.bppend((
                           (HTTPHebderValue)iter.next()).httpStringValue());
            writeBuffer.bppend(commaSpace);
            wrote = true;
        }
		// Truncbte the last comma from the buffer.
		// This is brguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-2);		    
		return writeBuffer.toString();
    }

}
