
/*
 * Roger Kapsi's Java Package
 * Copyright (C) 2003 Roger Kapsi
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package de.kapsi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * OSAScript is a simple scripting interface for Apple's Open Scripting Architecture (OSA).
 */
public class OSAScript implements Serializable {
    
    static {
        System.loadLibrary("OpenScripting");
    }
    
    private int ptr;
    private String source;
    private boolean compiled;
    
    private OSAScript() {}
    
    /**
     * Creates a new OSAScript from the passed source code. E.g.
     * <p>
     * OSAScript os = new OSAScript("tell application \"Finder\"\nactivate\nend tell");<br>
     * os.execute();
     * </p>
     */
    public OSAScript(String source) 
        throws UnsatisfiedLinkError, OSAException {
        
        ptr = NewOSAScriptWithSource(source);
        
        this.compiled = false;
        this.source = source;
    }
    
    /**
     * Loads a precompiled .scpt file
     */
    public OSAScript(File file) 
        throws UnsatisfiedLinkError, IOException, OSAException {
        
        BufferedInputStream in = null;
        
        try {
            
            byte[] bytes = new byte[(int)file.length()];
            in = new BufferedInputStream(new FileInputStream(file));
            if (in.read(bytes, 0, bytes.length) != bytes.length) {
                throw new IOException();
            }

            ptr = NewOSAScriptWithBinaries(bytes);
            
            this.compiled = true;
            this.source = null;
            
        } finally {
            if (in != null) { in.close(); }
        }
    }
    
    private void readObject(java.io.ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        
        compiled = stream.readBoolean();
        source = (String)stream.readObject();
        
        if (compiled) {
            byte[] bytes = (byte[])stream.readObject();
            ptr = NewOSAScriptWithBinaries(bytes);
            source = null;
        }
    }
    
    private void writeObject(java.io.ObjectOutputStream stream)
        throws IOException {
        
        stream.writeBoolean(isCompiled());
        stream.writeObject(source);
        
        if (compiled) {
            byte[] bytes = GetOSAScriptBinaries(ptr);
            stream.writeObject(bytes);
        }
    }
    
     /**
      * Returns the source of this script or null if script was loaded
      * from a file.
      */ 
    public String getSource() {
        return source;
    }
    
    /**
     * Returns true if script is compiled
     */
    public boolean isCompiled() {
        return compiled;
    }
    
    /**
     * Returns the binaries of this Script or null if script
     * is not compiled. The binaries have the same format as
     * precompiled .scpt files!
     */
    public byte[] getBytes() throws OSAException {
        if (compiled) {
            return GetOSAScriptBinaries(ptr);
        }
        
        return null;
    }
    
    /**
     * Compiles the script
     */
    public void compile() throws OSAException {
        if (!compiled) {
            compiled = CompileOSAScript(ptr);
        }
    }
    
    /**
     * Executes the script and returns the results as byte-array.
     * It is up to you to interpret the data (usually Strings). The
     * script will be compiled automatically if necessary.
     */
    public byte[] execute() throws OSAException {
    
        if (!compiled) {
            compile();
        }

        return ExecutOSAScript(ptr);
    }
    
    /**
     * Executes a specific subroutine of the script and returns the results as byte-array.
     * It is up to you to interpret the data (usually Strings). The script will be compiled 
     * automatically if necessary.
     *
     * <p>The name of the subroutine must be written in lower case!</p>
     */
    public byte[] execute(String subroutine) throws OSAException {
    
        if (!compiled) {
            compile();
        }

        return ExecuteOSAScriptEvent(ptr, subroutine, null);
    }
    
    /**
     * Executes a specific subroutine of the script with optional parameters and returns the 
     * results as byte-array. It is up to you to interpret the data (usually Strings). The script 
     * will be compiled automatically if necessary.
     *
     * <p>The name of the subroutine must be written in lower case!</p>
     */
    public byte[] execute(String subroutine, String[] args) throws OSAException {
    
        if (!compiled) {
            compile();
        }

        return ExecuteOSAScriptEvent(ptr, subroutine, args);
    }
    
    protected void finalize() throws Throwable {
        ReleaseOSAScript(ptr);
    }

    private static native synchronized int NewOSAScriptWithSource(String source) throws OSAException;
    private static native synchronized int NewOSAScriptWithBinaries(byte[] bytes) throws OSAException;
    private static native synchronized void ReleaseOSAScript(int ptr) throws OSAException;
    
    private static native synchronized boolean CompileOSAScript(int ptr) throws OSAException;
    private static native synchronized byte[] ExecutOSAScript(int ptr) throws OSAException;
    private static native synchronized byte[] ExecuteOSAScriptEvent(int ptr, String subroutine, String[] args) throws OSAException;
    
    private static native synchronized byte[] GetOSAScriptBinaries(int ptr) throws OSAException;
}
