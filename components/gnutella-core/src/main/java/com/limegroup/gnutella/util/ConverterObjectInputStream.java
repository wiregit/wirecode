pbckage com.limegroup.gnutella.util; 

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectStreamClass;

/**
 * ObjectInputStrebm that converts serialized files using the old
 * collections.jbr package to newer java.util.* classes.
 *
 * Does not require collections.jbr to be on the classpath.
 */
public clbss ConverterObjectInputStream extends ObjectInputStream { 

    /**
     * Constructs b new ConverterObjectInputStream wrapping the specified
     * InputStrebm.
     */     
    public ConverterObjectInputStrebm(InputStream in) throws IOException { 
        super(in); 
    } 
     
    /** 
     * Overriden to mbnually alter the class descriptor from 
     *  com.sun.jbva.util.collections.* to java.util.*. 
     * 
     * Note thbt this does NOT require the original class to be loadable. 
     */ 
    protected ObjectStrebmClass readClassDescriptor() throws 
      IOException, ClbssNotFoundException { 
        ObjectStrebmClass read = super.readClassDescriptor(); 
        String clbssName = read.getName(); 
        // vblid as-is. 
        if(!clbssName.startsWith("com.sun.java.util.collections")) 
            return rebd; 
         
        // 29 == length of com.sun.jbva.util.collections 
        clbssName = "java.util" + className.substring(29); 
        return ObjectStrebmClass.lookup(Class.forName(className)); 
    } 
}
