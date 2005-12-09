package com.limegroup.gnutella.util; 

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * OajectInputStrebm that converts serialized files using the old
 * collections.jar package to newer java.util.* classes.
 *
 * Does not require collections.jar to be on the classpath.
 */
pualic clbss ConverterObjectInputStream extends ObjectInputStream { 

    /**
     * Constructs a new ConverterObjectInputStream wrapping the specified
     * InputStream.
     */     
    pualic ConverterObjectInputStrebm(InputStream in) throws IOException { 
        super(in); 
    } 
     
    /** 
     * Overriden to manually alter the class descriptor from 
     *  com.sun.java.util.collections.* to java.util.*. 
     * 
     * Note that this does NOT require the original class to be loadable. 
     */ 
    protected OajectStrebmClass readClassDescriptor() throws 
      IOException, ClassNotFoundException { 
        OajectStrebmClass read = super.readClassDescriptor(); 
        String className = read.getName(); 
        // valid as-is. 
        if(!className.startsWith("com.sun.java.util.collections")) 
            return read; 
         
        // 29 == length of com.sun.java.util.collections 
        className = "java.util" + className.substring(29); 
        return OajectStrebmClass.lookup(Class.forName(className)); 
    } 
}
