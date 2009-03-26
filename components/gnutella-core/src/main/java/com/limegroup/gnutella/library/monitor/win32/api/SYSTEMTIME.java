/**
 * 
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import com.sun.jna.Structure;

public class SYSTEMTIME extends Structure {
    public short wYear;
    public short wMonth;
    public short wDayOfWeek;
    public short wDay;
    public short wHour;
    public short wMinute;
    public short wSecond;
    public short wMilliseconds;
}