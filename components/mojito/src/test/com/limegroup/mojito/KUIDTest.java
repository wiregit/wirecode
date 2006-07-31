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

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.util.ArrayUtils;

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
        
        KUID lookup = KUID.createNodeID(ArrayUtils.parseHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB"));
        KUID worst = KUID.createNodeID(ArrayUtils.parseHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868"));
        KUID best = KUID.createNodeID(ArrayUtils.parseHexString("F2617265969422D11CFB73C75EE8B649132DFB37"));
        
        assertTrue(worst.isNearer(best, lookup));
    }
    
    public void testCompareTo() {
        KUID a = KUID.createNodeID(ArrayUtils.parseHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB"));
        KUID b = KUID.createNodeID(ArrayUtils.parseHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868"));
        
        assertEquals(0, a.compareTo(a));
        assertEquals(-1, a.compareTo(b));
        
        assertEquals(0, b.compareTo(b));
        assertEquals(1, b.compareTo(a));
    }
    
    public void testQueryKey() throws Exception {
        InetSocketAddress addr1 = new InetSocketAddress("localhost", 1234);
        QueryKey key1 = QueryKey.getQueryKey(addr1);
        
        MessageID messageId1 = MessageID.create(addr1);
        Method m = MessageID.class.getDeclaredMethod("getQueryKey", new Class[0]);
        m.setAccessible(true);
        QueryKey key2 = (QueryKey)m.invoke(messageId1, new Object[0]);
        
        assertTrue(key1.equals(key2));
        assertTrue(messageId1.verifyQueryKey(addr1));
        
        InetSocketAddress addr2 = new InetSocketAddress("www.google.com", 1234);
        assertFalse(messageId1.verifyQueryKey(addr2));
    }
}
