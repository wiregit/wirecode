package org.limewire.util; 

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

/**
 * ObjectInputStream that converts serialized files using old packages
 * or classnames into the newer equiv.
 * 
 * This is currently hardcoded to convert:
 *  com.sun.java.util.collections.* -> java.util.*
 *  com.limegroup.gnutella.util.Comparators$CaseInsensitiveStringComparator -> org.limewire.collection.Comparators$CaseInsensitiveStringComparator
 *  com.limegroup.gnutella.util.FileComparator -> org.limewire.collection.FileComparator
 *  com.limegroup.gnutella.downloader.Interval -> org.limewire.collection.Interval
 *  com.limegroup.gnutella.util.IntervalSet -> org.limewire.collection.IntervalSet
 *  
 * None of the earlier forms of the class need to exist on the classpath.
 * 
 * TODO: Add support for adding arbitrary migrations.
 */
public class ConverterObjectInputStream extends ObjectInputStream { 
    
    private Map<String, String> lookups = new HashMap<String, String>(3);

    /**
     * Constructs a new ConverterObjectInputStream wrapping the specified
     * InputStream.
     */     
    public ConverterObjectInputStream(InputStream in) throws IOException { 
        super(in); 
        createLookups();
    } 
    
    private void createLookups() {
        lookups.put("com.limegroup.gnutella.util.FileComparator", "org.limewire.collection.FileComparator");
        lookups.put("com.limegroup.gnutella.downloader.Interval", "org.limewire.collection.Interval");
        lookups.put("com.limegroup.gnutella.util.IntervalSet", "org.limewire.collection.IntervalSet");
        lookups.put("com.limegroup.gnutella.util.Comparators$CaseInsensitiveStringComparator", "org.limewire.collection.Comparators$CaseInsensitiveStringComparator");
        lookups.put("com.limegroup.gnutella.util.StringComparator", "org.limewire.collection.StringComparator");
    }
     
    /** 
     * Overriden to manually alter the class descriptors. 
     * 
     * Note that this does NOT require the original class to be loadable. 
     */ 
    protected ObjectStreamClass readClassDescriptor() throws 
      IOException, ClassNotFoundException { 
        ObjectStreamClass read = super.readClassDescriptor(); 
        String className = read.getName(); 
        
        if(className.startsWith("com.sun.java.util.collections")) {
            // 29 == length of com.sun.java.util.collections 
            className = "java.util" + className.substring(29); 
            return ObjectStreamClass.lookup(Class.forName(className));
        } else {
            String alter = lookups.get(className);
            if(alter != null)
                return ObjectStreamClass.lookup(Class.forName(alter));
            else
                return read;
        }
    } 
}
