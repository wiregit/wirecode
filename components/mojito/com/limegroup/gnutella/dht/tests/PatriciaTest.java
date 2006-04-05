package com.limegroup.gnutella.dht.tests;

import java.util.List;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class PatriciaTest {

    public void testRange() {
        PatriciaTrie trie = new PatriciaTrie();
        for(int i = 0; i < 50; i++) {
            KUID id = KUID.createRandomMessageID();
            trie.put(id, id);
        }
        
        List list = trie.range(KUID.createRandomMessageID(), -1);
        System.out.println(trie.size() == list.size()); // true
    }
    
    public static void main(String[] args) {
        new PatriciaTest().testRange();
    }
}
