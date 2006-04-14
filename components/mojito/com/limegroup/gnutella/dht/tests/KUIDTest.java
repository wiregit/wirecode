/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package com.limegroup.gnutella.dht.tests;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.util.ArrayUtils;

public class KUIDTest {
    
    public void testIsCloser() {
        
        KUID lookup = KUID.createNodeID(ArrayUtils.parseHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB"));
        KUID worst = KUID.createNodeID(ArrayUtils.parseHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868"));
        KUID best = KUID.createNodeID(ArrayUtils.parseHexString("F2617265969422D11CFB73C75EE8B649132DFB37"));
        
        System.out.println(worst.isCloser(best, lookup));
    }
    
    public void testCompareTo() {
        KUID a = KUID.createNodeID(ArrayUtils.parseHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB"));
        KUID b = KUID.createNodeID(ArrayUtils.parseHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868"));
        
        System.out.println(a.compareTo(a)); // 0
        System.out.println(a.compareTo(b)); // -1
        
        System.out.println(b.compareTo(b)); // 0
        System.out.println(b.compareTo(a)); // 1
    }
    
    public static void main(String[] args) {
        new KUIDTest().testIsCloser();
        new KUIDTest().testCompareTo();
    }
}
