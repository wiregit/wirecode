package org.limewire.util; 

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts older package and class names to the new equivalent name and is useful 
 * in code refactoring. For example, if packages or classes are renamed, 
 * <code>ObjectInputStream</code> fails to find 
 * the older name. <code>ConverterObjectInputStream</code> looks up the old name 
 * and if a new name exists, returns the {@link ObjectStreamClass} object for
 * the new name. Then the corresponding new class/package name is loaded. If the
 * new name doesn't exist in <code>ConverterObjectInputStream</code>'s mapping,
 * then the original <code>ObjectStreamClass</code> object is returned.
 * <p>
 * <code>ConverterObjectInputStream</code> comes with a pre-set package and class 
 * mapping between the old and new name, and you can add more lookups with 
 * {@link #addLookup(String, String)}.
 * <p>
 * Pre-set package and class mapping:
 * <table cellpadding="5">
 * <tr>
 * <td><b>Old Name</b></td>
 * <td><b>New Name</b></td>
 * </tr>
 * <td>com.limegroup.gnutella.util.FileComparator</td>
 * <td>org.limewire.collection.FileComparator</td>
 * </tr>
 * <tr>
 * <td>com.limegroup.gnutella.downloader.Interval</td>
 * <td>org.limewire.collection.Interval</td>
 * </tr>
 * <tr>
 * <td>com.limegroup.gnutella.util.IntervalSet</td>
 * <td> org.limewire.collection.IntervalSet</td>
 * </tr>
 * <tr>
 * <td>com.limegroup.gnutella.util.Comparators$CaseInsensitiveStringComparator</td>
 * <td> org.limewire.collection.Comparators$CaseInsensitiveStringComparator</td>
 * </tr>
 * <td>com.limegroup.gnutella.util.StringComparator</td>
 * <td> org.limewire.collection.StringComparator</td>
 * </tr>
 * <tr>
 * <td>com.sun.java.util.collections</td>
 * <td>java.util</td>
 * </tr>
 * </table>
 * None of the earlier forms of the class need to exist in the classpath.
 */

public class ConverterObjectInputStream extends ObjectInputStream { 
    
    private Map<String, String> lookups = new HashMap<String, String>(8);

    /**
     * Constructs a new <code>ConverterObjectInputStream</code> wrapping the 
     * specified <code>InputStream</code>.
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
     * Overridden to manually alter the class descriptors. 
     * Note this does NOT require the original class to be loadable.
     * <p>
     * Lookup works as follows:
     * <ul>
     * <li>The serialized (old) class name is looked up, if a corresponding new
     * class name exists the <code>ObjectStreamClass</code> object for it is returned.</li>
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
