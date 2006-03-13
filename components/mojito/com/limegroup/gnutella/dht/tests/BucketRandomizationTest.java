package com.limegroup.gnutella.dht.tests;

import java.util.Iterator;
import java.util.List;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.util.KUIDKeyCreator;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class BucketRandomizationTest {
	
	public static void main(String[] args) {
		final String[] keys = new String[]{ 
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo", 
                "Amma"
        };
		
		PatriciaTrie trie = new PatriciaTrie(new KUIDKeyCreator());
		for(int i = 0; i < keys.length; i++) {
			trie.put(toKUID(keys[i]), keys[i]);
		}
		
		System.out.println(trie);
		
		List list = trie.select(toKUID("Am"), 6);
		for(Iterator it = list.iterator(); it.hasNext(); ) {
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
