
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

/**
 * Thrown by OpenScripting components e.g. OSAScript. 
 */
public class OSAException extends Exception {
    
	static {
        System.loadLibrary("OpenScripting");
    }
	
	private String msg;
	private int errorCode;
	private int errorNum;
	
	/* friendly */
	OSAException(OSAScript script, int errorCode) {
		
		msg = GetErrorMessage(script.ptr);
		errorNum = GetErrorNumber(script.ptr);
		
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public int getErrorNum() {
		return errorNum;
	}
	
	public String getMessage() {
		if (msg == null) {
			return errorCode + ", " + errorNum;
		} else {
			return msg + " (" + errorCode + ", " + errorNum + ")";
		}
	}
	
	private static native synchronized String GetErrorMessage(int ptr);
	private static native synchronized int GetErrorNumber(int ptr);
}
