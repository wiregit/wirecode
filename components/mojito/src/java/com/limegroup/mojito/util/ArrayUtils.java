/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
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
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.mojito.util;

/**
 * Miscellaneous utilities for Arrays
 */
public final class ArrayUtils {
    
    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', 
        '6', '7', '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    };
    
    private static final String[] BIN = {
        "0000", "0001", "0010", "0011", "0100", "0101", 
        "0110", "0111", "1000", "1001", "1010", "1011",
        "1100", "1101", "1110", "1111"
    };
    
    private ArrayUtils() {}
    
    public static boolean isNull(byte[] data) {
        if (data == null 
                || data.length == 0) {
            return true;
        }
        
        int sum = 0;
        for(int i = data.length; i >= 0; --i) {
            sum += (int)(data[i] & 0xFF);
        }
        return sum == 0;
    }
    
    public static String toHexString(byte[] data) {
        StringBuilder buffer = new StringBuilder(data.length * 2);
        for(int i = 0; i < data.length; i++) {
            buffer.append(HEX[(data[i] >> 4) & 0xF]).append(HEX[data[i] & 0xF]);
        }
        return buffer.toString();
    }
    
    public static String toBinString(byte[] data) {
        StringBuilder buffer = new StringBuilder(data.length * 8);
        for(int i = 0; i < data.length; i++) {
            buffer.append(BIN[(data[i] >> 4) & 0xF]).append(BIN[data[i] & 0xF]).append(" ");
        }
        
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length()-1);
        }
        return buffer.toString();
    }
    
    public static byte[] parseHexString(String data) {
        byte[] buffer = new byte[data.length()/2];
        for(int i = 0, j = 0; i < buffer.length; i++) {
            int hi = parseHexChar(data.charAt(j++));
            int lo = parseHexChar(data.charAt(j++));
            buffer[i] = (byte)(((hi & 0xF) << 4) | (lo & 0xF));
        }
        return buffer;
    }
    
    private static int parseHexChar(char c) {
        switch(c) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'a': return 10;
            case 'A': return 10;
            case 'b': return 11;
            case 'B': return 11;
            case 'c': return 12;
            case 'C': return 12;
            case 'd': return 13;
            case 'D': return 13;
            case 'e': return 14;
            case 'E': return 14;
            case 'f': return 15;
            case 'F': return 15;
            default: throw new NumberFormatException("Unknown digit: " + c);
        }
    }
}
