
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


/**
 * OSAScript is a simple scripting interface for Apple's Open Scripting Architecture (OSA).
 */
public class OSAScript {
    
	static {
        System.loadLibrary("OpenScripting");
    }
	
    private int ptr = 0;
	    
    /**
     * Creates a new OSAScript from the passed source code. E.g.
     * <p>
     * OSAScript os = new OSAScript("tell application \"Finder\"\nactivate\nend tell");<br>
     * os.execute();
     * </p>
	 * <p>
	 * Note: This type of scripts does not work accurate on Mac OS X 10.3! 
	 * </p>
     */
    public OSAScript(String source) 
        throws UnsatisfiedLinkError, OSAException {
        
		int[] tmp = new int[1]; // Call by reference
        int errorCode = NewOSAScriptWithSrc(tmp, source);
		ptr = tmp[0];
		
		if (ptr == 0) {
            throw (new IllegalStateException());
        }
		
		if (errorCode < 0) {
			String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
		}
		
		compile();
    }
    
    /**
     * Loads a Script from a file (.scpt).
     */
    public OSAScript(File file) 
        throws UnsatisfiedLinkError, IOException, OSAException {
        
        BufferedInputStream in = null;
        
        try {
            
            byte[] buf = new byte[(int)file.length()];
            in = new BufferedInputStream(new FileInputStream(file));
            if (in.read(buf, 0, buf.length) != buf.length) {
                throw new IOException();
            }
			
			int[] tmp = new int[1]; // Call by reference
            int errorCode = NewOSAScriptWithBin(tmp, buf);
			ptr = tmp[0];
			
			if (ptr == 0) {
				throw (new IllegalStateException());
			}
			
			if (errorCode < 0) {
				String msg = GetErrorMessage(ptr);
				int errorNum = GetErrorNumber(ptr);
				throw (new OSAException(msg, errorNum, errorCode));
			}
		
        } finally {
            if (in != null) { 
				in.close(); 
				in = null;
			}
        }
    }
	
    /**
     * Creates an OSAScript from a byte buffer
     */
    public OSAScript(byte[] script)
        throws UnsatisfiedLinkError, OSAException {
        
        int[] tmp = new int[1]; // Call by reference
        int errorCode = NewOSAScriptWithBin(tmp, script);
        ptr = tmp[0];
        
        if (errorCode < 0) {
            String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
        }
    }
    
    /**
     * Returns the binaries of this Script or null if script
     * is not compiled. The binaries have the same format as
     * precompiled .scpt files!
     */
    public byte[] getBytes() throws OSAException, IllegalStateException {
        
        if (ptr == 0) {
            throw (new IllegalStateException());
        }
        
        int size = GetOSAScriptSize(ptr);
        if (size == 0) { return null; }
        
        byte[] dst = new byte[size];
        
        int errorCode = GetOSAScript(ptr, dst, 0, size);
        
        if (errorCode < 0) {
            String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
        }
        
		return dst;
    }
    
    /**
     * Compiles the script
     */
    private void compile() throws OSAException, IllegalStateException {
		
        if (ptr == 0) {
            throw (new IllegalStateException());
        }
        	
		int errorCode = CompileOSAScript(ptr);
			
		if (errorCode < 0) {
			String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
		}
    }
    
    /**
     * Executes the scrip. The script will be compiled automatically if necessary.
     */
    public void execute() throws OSAException, IllegalStateException {

        if (ptr == 0) {
            throw (new IllegalStateException());
        }
        
		int errorCode = ExecuteOSAScript(ptr);
		
		if (errorCode < 0) {
			String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
		}
    }
    
    /**
     * Executes a specific subroutine of the script. The script will be compiled 
     * automatically if necessary.
     *
     * <p>The name of the subroutine must be written in lower case!</p>
     */
    public void execute(String subroutine) throws OSAException, IllegalStateException {
    
        if (ptr == 0) {
            throw (new IllegalStateException());
        }
        
		int errorCode = ExecuteOSAScriptEvent(ptr, subroutine, null);
		
		if (errorCode < 0) {
			String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
		}
    }
    
    /**
     * Executes a specific subroutine of the script with optional parameters. The script 
     * will be compiled automatically if necessary.
     *
     * <p>The name of the subroutine must be written in lower case!</p>
     */
    public void execute(String subroutine, String[] args) throws OSAException, IllegalStateException {
    
        if (ptr == 0) {
            throw (new IllegalStateException());
        }
        
        int errorCode = ExecuteOSAScriptEvent(ptr, subroutine, args);
	
		if (errorCode < 0) {
			String msg = GetErrorMessage(ptr);
			int errorNum = GetErrorNumber(ptr);
			throw (new OSAException(msg, errorNum, errorCode));
		}
    }
    
	/** 
	* Returns the results of this script as byte-array. It is up to you 
	* to interpret the data (usually Strings)
	*/
	public AEDesc getResult() throws OSAException, IllegalStateException {
		
		if (ptr == 0) {
            throw (new IllegalStateException());
        }
		
		int size = GetResultDataSize(ptr);
		
		if (size > 0) {
			
			String type = GetResultType(ptr);
			
			byte[] data = new byte[size];
			GetResultData(ptr, data, 0, size);
			
			return (new AEDesc(type, data));
		} else {
			return null;
		}
	}
	
    /**
     * Releases the native resources
     */
    public void close() {
        if (ptr > 0) {
            ReleaseOSAScript(ptr);
            ptr = 0;
        }
    }

    protected void finalize() throws Throwable {
        if (ptr > 0) {
            ReleaseOSAScript(ptr);
        }
    }
	
    private static native synchronized int NewOSAScriptWithSrc(int[] ptr, String src);
    private static native synchronized int NewOSAScriptWithBin(int[] ptr, byte[] bin);
    
    private static native synchronized int CompileOSAScript(int ptr);
	private static native synchronized int ReleaseOSAScript(int ptr);
	
    private static native synchronized int ExecuteOSAScript(int ptr);
    private static native synchronized int ExecuteOSAScriptEvent(int ptr, String subroutine, String[] args);
    
	private static native synchronized int GetOSAScriptSize(int ptr);
    private static native synchronized int GetOSAScript(int ptr, byte[] buf, int pos, int length);
	
	private static native synchronized String GetResultType(int ptr);
	private static native synchronized int GetResultDataSize(int ptr);
	private static native synchronized int GetResultData(int ptr, byte[] buf, int pos, int length);
	
	private static native synchronized String GetErrorMessage(int ptr);
	private static native synchronized int GetErrorNumber(int ptr);
}
