
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

import java.io.Serializable;

/**
 * A simple Java Warpper for returned Apple Event Descriptors (AEDesc).
 *
 * <p><a href="http://developer.apple.com/documentation/Carbon/Reference/Apple_Event_Manager/apple_event_manager_ref/data_type_3.html#//apple_ref/c/tdef/AEDesc">AEDesc</a></p>
 */
public class AEDesc implements Serializable {
    
    private String type;
    private byte[] data;
    
    /* friendly */
    AEDesc(int type, byte[] data) {
        this.type = OSType2String(type);
        this.data = data;
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
        return "AEDesc(type=" + type + ", data.length=" + ((data != null) ? data.length : 0) + ")";
    }
    
    private static String OSType2String(int OSType) {
    
        byte[] temp = new byte[4];
        temp[3] = (byte)OSType;
        temp[2] = (byte)(OSType >> 8);
        temp[1] = (byte)(OSType >> 16);
        temp[0] = (byte)(OSType >> 24);
        
        String type = new String(temp);
        return type ;
    }
}
