package com.limegroup.gnutella.handshaking;

import junit.framework.*;
import java.text.ParseException;

public class UptimeHeadersTest extends TestCase {
    public UptimeHeadersTest(String name) {
        super(name);
    }

    public void testDecodeUptime() {
        try {
            assertEquals(UptimeHeaders.decodeUptime("1D 0H 0M"), 24*60*60);
            assertEquals(UptimeHeaders.decodeUptime("0D 1H 0M"), 60*60);
            assertEquals(UptimeHeaders.decodeUptime("0D 0H 1M"), 60);
            assertEquals(UptimeHeaders.decodeUptime(" 4D 2H  3M"), 
                         4*24*60*60+2*60*60+3*60);
        } catch (ParseException e) {
            fail("Unexpected parse problem: "+e);
        }
    }

    public void testDecodeUptimeExceptional() {
        try {
            UptimeHeaders.decodeUptime("1D");
            fail("No exception");
        } catch (ParseException e) { } 
        try {
            UptimeHeaders.decodeUptime("1D 3H 4M 4S");
            fail("No exception");
        } catch (ParseException e) { } 
        try {
            UptimeHeaders.decodeUptime("1 3H 4M");
            fail("No exception");
        } catch (ParseException e) { } 
        try {
            UptimeHeaders.decodeUptime("1D H 4M");
            fail("No exception");
        } catch (ParseException e) { } 
        try {
            UptimeHeaders.decodeUptime("1D 3H 4X");
            fail("No exception");
        } catch (ParseException e) { } 
        try {
            UptimeHeaders.decodeUptime("1D 3H XM");
            fail("No exception");
        } catch (ParseException e) { } 
    }
 }
