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
 
package com.limegroup.mojito;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;

public class KUIDTest extends BaseTestCase {
    
    public KUIDTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(KUIDTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testIsNearer() {
        
        KUID lookup = KUID.create("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID worst = KUID.create("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868");
        KUID best = KUID.create("F2617265969422D11CFB73C75EE8B649132DFB37");
        
        assertTrue(worst.isNearerTo(lookup, best));
        assertFalse(best.isNearerTo(lookup, worst));
    }
    
    public void testCompareTo() {
        KUID a = KUID.create("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID b = KUID.create("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868");
        
        assertEquals(0, a.compareTo(a));
        assertEquals(-1, a.compareTo(b));
        
        assertEquals(0, b.compareTo(b));
        assertEquals(1, b.compareTo(a));
    }
    
    public void testSetBit() {
        KUID a = KUID.create("0000000000000000000000000000000000000000");
        KUID b = KUID.create("1000000000000000000000000000000000000000");
        KUID c = KUID.create("0800000000000000000000000000000000000000");
        
        assertFalse(a.isBitSet(3));
        
        assertTrue(b.isBitSet(3));
        assertFalse(b.isBitSet(4));
        
        assertFalse(c.isBitSet(3));
        assertTrue(c.isBitSet(4));
        
        c = c.set(4);
        assertTrue(c.isBitSet(4));
        
        c = c.unset(4);
        assertFalse(c.isBitSet(4));
        
        c = c.flip(4);
        assertTrue(c.isBitSet(4));
        
        c = c.flip(4);
        assertFalse(c.isBitSet(4));
    }
}
