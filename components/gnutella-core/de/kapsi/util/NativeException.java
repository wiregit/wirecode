
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
 * For a complete list of error codes see:
 * CoreServices.framework -> CarbonCore.framework -> Headers -> MacError.h
 *
 * @author <a href="mailto:info@kapsi.de">Roger Kapsi</a>
 */
public class NativeException extends RuntimeException {
    
    public static final int FILE_NOT_FOUND = -43;
    public static final int ERROR_IN_PARAMETER_LIST = -50;
    
    public static final int NOT_ENOUGH_MEMORY = -108;
    
    private final int errorCode;
    
    public NativeException(int errorCode) {
        super(createMessage(errorCode));
        this.errorCode = errorCode;
    }
    
    public NativeException(String msg, int errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public String getMessage() {
        return (super.getMessage() + " (" + errorCode + ")");
    }
    
    protected static String createMessage(int errorCode) {
        switch(errorCode) {
            case FILE_NOT_FOUND:
                return "File not found";
            case ERROR_IN_PARAMETER_LIST:
                return "Error in parameter list";
            case NOT_ENOUGH_MEMORY:
                return "Not enough memory";
            default:
                return "Unknown";
        }
    }
}
