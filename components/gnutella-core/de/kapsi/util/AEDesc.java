
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

public class AEDesc {
    
	static {
        System.loadLibrary("OpenScripting");
    }
    
    private String type;
    private byte[] data;
    
    /* friendly */
    AEDesc(OSAScript script) {
	
        type = GetType(script.ptr);
		
		int size = GetDataSize(script.ptr);
        if (size > 0) {
			data = new byte[size];
			GetData(script.ptr, data, 0, size);
		}
    }
    
    /**
     * Returns a four-charcter code that indicates the type
     * of data in the byte-array.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Returns the data which came with the AppleEvent. It's
     * up to you to interpret the data. 
     */
    public byte[] getData() {
        return data;
    }

    public String toString() {
        return "AEDesc(type=" + type + ", data.length=" + 
					((data != null) ? data.length : 0) + ")";
    }
	
	private static native synchronized String GetType(int ptr);
	private static native synchronized int GetDataSize(int ptr);
	private static native synchronized int GetData(int ptr, byte[] buf, int pos, int length);
}
