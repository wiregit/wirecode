
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
 *
 * @author <a href="mailto:info@kapsi.de">Roger Kapsi</a>
 */
public class OSAException extends NativeException {
    
    /** Not really an OSA error but very common ;) */
    public static final int errAEEventNotHandled 		= -1708;
    
	public static final int errOSASystemError             = -1750;
	public static final int errOSAInvalidID               = -1751;
	public static final int errOSABadStorageType          = -1752;
	public static final int errOSAScriptError             = -1753;
	public static final int errOSABadSelector             = -1754;
	public static final int errOSASourceNotAvailable      = -1756;
	public static final int errOSANoSuchDialect           = -1757;
	public static final int errOSADataFormatObsolete      = -1758;
	public static final int errOSADataFormatTooNew        = -1759;
	public static final int errOSACorruptData             = -1702;
	public static final int errOSARecordingIsAlreadyOn    = -1732;
	public static final int errOSAComponentMismatch       = -1761; /* Parameters are from 2 different components */
	public static final int errOSACantOpenComponent       = -1762; /* Can't connect to scripting system with that ID */
    
    private AEDesc desc;
    
    public OSAException(int errorCode, AEDesc desc) {
        super((desc != null) ? (new String(desc.getData())) : createMessage(errorCode), errorCode);
        
        this.desc = desc;
    }

    public AEDesc getAEDesc() {
        return desc;
    }

    /**
     * Returns a more or less informative description for
     * the errorCode.
     */
    protected static String createMessage(int errorCode) {
        switch(errorCode) {
            case errAEEventNotHandled:
                return "AEEventNotHandled"; 
            case errOSASystemError:
                return "errOSASystemError";
            case errOSAInvalidID:
                return "errOSAInvalidID";
            case errOSABadStorageType:
                return "errOSABadStorageType";
            case errOSAScriptError:
                return "errOSAScriptError";
            case errOSABadSelector:
                return "errOSABadSelector";
            case errOSASourceNotAvailable:
                return "errOSASourceNotAvailable";
            case errOSANoSuchDialect:
                return "errOSANoSuchDialect";
            case errOSADataFormatObsolete:
                return "errOSADataFormatObsolete";
            case errOSADataFormatTooNew:
                return "errOSADataFormatTooNew";
            case errOSACorruptData:
                return "errOSACorruptData";
            case errOSARecordingIsAlreadyOn:
                return "errOSARecordingIsAlreadyOn";
            case errOSAComponentMismatch:
                return "errOSAComponentMismatch";
            case errOSACantOpenComponent:
                return "errOSACantOpenComponent";
            default:
                return NativeException.createMessage(errorCode);
        }
    }
}
