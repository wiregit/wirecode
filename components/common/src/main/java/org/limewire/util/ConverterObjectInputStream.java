package org.limewire.util; 

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts older package names to the new equivalent name version and extends the <code>ObjectInputStream</code> 
 * used for serialization. <code>ConverterObjectInputStream</code> is an input stream
 * and is useful in code refactoring. If classes are renamed, <code>ObjectInputStream</code> fails to find
 * the older name. Therefore, <code>ConverterObjectInputStream</code> fixes the problem with changed class names.
 * 
 * 
 * <p><table cellpadding="5">
 * <tr> <td><b> Former Package Name</b></td>                    <td> <b>New Package Name</b></td> </tr>
 * <tr> <td>com.sun.java.util.collections.* </td>               <td> java.util.* </td> </tr>
 * <tr> <td>com.limegroup.gnutella.util.FileComparator</td>     <td> org.limewire.collection.FileComparator</td> </tr>
 * <tr> <td>com.limegroup.gnutella.downloader.Interval</td>     <td> org.limewire.collection.Interval</td> </tr>
 * <tr> <td>com.limegroup.gnutella.util.IntervalSet</td>        <td> org.limewire.collection.IntervalSet</td> </tr>
 * <tr> <td>com.limegroup.gnutella.util.Comparators$CaseInsensitiveStringComparator</td> <td> org.limewire.collection.Comparators$CaseInsensitiveStringComparator</td> </tr>
 * <tr> <td>com.limegroup.gnutella.util.StringComparator</td>   <td> org.limewire.collection.StringComparator</td> </tr>
 * </table><p>
 * None of the earlier forms of the class need to exist in the classpath.<p> 
 */
// TODO: Add support for adding arbitrary migrations.
public class ConverterObjectInputStream extends ObjectInputStream { 
    
    private Map<String, String> lookups = new HashMap<String, String>(8);

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
        lookups.put("com.sun.java.util.collections", "java.util");
    }

    /**
     * Adds a mapping between an old package or class name to a new name.
     * @param oldName the name of the old package or class
     * @param newName the name of the new package or class
     */
    public void addLookup(String oldName, String newName) {
        lookups.put(oldName, newName);
    }
     
    /** 
     * Overriden to manually alter the class descriptors. 
     * Note that this does NOT require the original class to be loadable.
     * 
     * Lookup works as follows:
     * <ul>
     * <li>The serialized (old) class name is looked up, if a corresponding new
     * class name exists the ObjectStreamClass object for it is returned.</li>
     * <li>The package name of the serialized class name is extracted and
     * looked up if a new package name exists, it is prepended to the name of
     * the class the corresponding class is loaded.</li>
     * <li>Otherwise the original ObjectStreamClass is returned.</li> 
     * <ul>
     */ 
    protected ObjectStreamClass readClassDescriptor() throws 
      IOException, ClassNotFoundException { 
        ObjectStreamClass read = super.readClassDescriptor(); 
        String className = read.getName(); 
        
        String newName = lookups.get(className);
        if (newName != null) {
            return ObjectStreamClass.lookup(Class.forName(newName));
        }
        else {
            int index = className.lastIndexOf('.');
            // use "" as lookup key for default package
            String oldPackage = index != -1 ? className.substring(0, index) : "";
            String newPackage = lookups.get(oldPackage);
            if (newPackage != null) {
                if (newPackage.length() == 0) {
                    // mapped to default package
                    ObjectStreamClass.lookup(Class.forName(className.substring(index + 1)));
                }
                return ObjectStreamClass.lookup(Class.forName(newPackage + '.' + className.substring(index + 1)));
            }
        }
        return read;
    } 
}
