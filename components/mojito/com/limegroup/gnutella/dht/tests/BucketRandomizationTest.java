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

import java.util.Iterator;
import java.util.List;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.util.KUIDKeyCreator;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class BucketRandomizationTest {
	
	public static void main(String[] args) {
        final String[] keys = new String[] { "Albert", "Xavier", "XyZ", "Anna",
                "Alien", "Alberto", "Alberts", "Allie", "Alliese", "Alabama",
                "Banane", "Blabla", "Amber", "Ammun", "Akka", "Akko",
                "Albertoo", "Amma" };

        PatriciaTrie trie = new PatriciaTrie(new KUIDKeyCreator());
        for (int i = 0; i < keys.length; i++) {
            trie.put(toKUID(keys[i]), keys[i]);
        }

        System.out.println(trie);

        List list = trie.select(toKUID("Albert"), 6);
        for (Iterator it = list.iterator(); it.hasNext();) {
            System.out.println(it.next());
        }
    }
	
	public static KUID toKUID(String key) {
		byte[] b = key.getBytes();
		
		byte[] id = new byte[20];
		System.arraycopy(b, 0, id, 0, Math.min(b.length, id.length));
		
		return KUID.createNodeID(id);
	}
}
