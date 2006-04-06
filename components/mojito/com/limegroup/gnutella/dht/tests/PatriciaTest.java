package com.limegroup.gnutella.dht.tests;

import java.util.Iterator;
import java.util.List;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.util.ArrayUtils;
import de.kapsi.net.kademlia.util.CollectionUtils;
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
    
    public void testMultipleSelect() {
        
        String localNode = "D6F8BAE43E4B1D6D31BC650D0A37EB30B2C7E3E8";
        String lookup = "E56242E5AC1F3E1819C2A791B68B9A68D8FF128F";
        
        String[] nodes = {
            "0FA8D0B4223DC824012090E3987AF46CBA53175B",
            "3A648FE27274539D6EB7E6FE6F4AD01B5F7FBCD7",
            "3DD530F8EF3D8BD47285A9B0D2130CC6DCF21868",
            "3DF376FD834BA256B0D90A2209970EA2C47FA046",
            "3E8C7B55A0F963018EB80F90DDFD683B3177D393",
            "62762D22271D6AF04003AB37559971FD6BA17836",
            "629B26E7DA93D2F72FF6B1F0DCA509021919E83D",
            "68270629639C90208FD63C2DD15543BD1E77C10C",
            "8D177936F0C6A3CC171020B3245C9CF106958603",
            "94850358CEFD33CD0BDE1F468AEA1E646B3F8724",
            "9B21E976DF82D5117ED1C14DE4E0D3A0586A8B35",
            "B325EA5FA6CD8C33BC700EAED2478311ED2D54CE",
            "CBEC6422A667A02B5C0A4477758DC548F935DED1",
            "D3AAF7CAFD733C2A0A97998E27C68123C74C09E9",
            "D6F8BAE43E4B1D6D31BC650D0A37EB30B2C7E3E8",
            "D74E1C48D7299B6935EE0DEC9C72A52205ED6098",
            "D93A72B3BD0F06D22A8549B0B236B5742D1F1235",
            "D9F92519043F613DE2D5FE8645CD543F8F0CEE0D",
            "DE84D7F096A619ECDC7716B6F70752F7563F4996",
            "F658FD1429FDACB169B50A08EEF28633E4EA7EDA",
            "F735922C5C8B54FFD9424EB46066236E7D5BBA8D"
        };
        
        PatriciaTrie trie = new PatriciaTrie();
        for(int i = 0; i < nodes.length; i++) {
            KUID kuid = KUID.createNodeID(ArrayUtils.parseHexString(nodes[i]));
            trie.put(kuid, kuid);
            
            System.out.println(kuid + " = " + kuid.toBinString());
        }
        
        //System.out.println(trie);
        
        KUID key = KUID.createNodeID(ArrayUtils.parseHexString(lookup));
        List items = trie.select(key, nodes.length);
        
        System.out.println();
        System.out.println("Key: \n" + key + " = " + key.toBinString() + "\n");
        
        for(Iterator it = items.iterator(); it.hasNext(); ) {
            KUID kuid = (KUID)it.next();
            System.out.println(kuid + " = " + kuid.toBinString());
        }
    }
    
    public void testSelect() {
        KUID lookup = KUID.createNodeID(ArrayUtils.parseHexString("C814CF8CF039760D1399720BDBBD10F5327DB5A9"));
        KUID inverted = lookup.invert();
        
        String[] nodes = {
            "D74E1C48D7299B6935EE0DEC9C72A52205ED6098",
            "F735922C5C8B54FFD9424EB46066236E7D5BBA8D",
            "8D177936F0C6A3CC171020B3245C9CF106958603",
            "9B21E976DF82D5117ED1C14DE4E0D3A0586A8B35",
            "94850358CEFD33CD0BDE1F468AEA1E646B3F8724"
        };
        
        PatriciaTrie trie = new PatriciaTrie();
        for(int i = 0; i < nodes.length; i++) {
            KUID kuid = KUID.createNodeID(ArrayUtils.parseHexString(nodes[i]));
            trie.put(kuid, kuid);
            
            System.out.println(kuid + " = " + kuid.toBinString());
        }
        
        System.out.println();
        System.out.println(trie);
        
        System.out.println();
        System.out.println("Lookup:");
        System.out.println(lookup + " = " + lookup.toBinString());
        
        System.out.println("Inverted:");
        System.out.println(inverted + " = " + inverted.toBinString());
        
        
        System.out.println();
        List items = trie.select(inverted, trie.size());
        for(Iterator it = items.iterator(); it.hasNext(); ) {
            KUID kuid = (KUID)it.next();
            System.out.println(kuid + " = " + kuid.toBinString());
        }
        
        System.out.println();
        KUID best = (KUID)trie.select(lookup);
        System.out.println("Selected for lookup: " + best);
        
        System.out.println();
        KUID selected = (KUID)trie.select(inverted);
        System.out.println("Selected for inverted: " + selected);
        
        System.out.println();
        System.out.println(trie.select(inverted, 1));
    }
    
    public static void main(String[] args) {
        //new PatriciaTest().testRange();
        new PatriciaTest().testSelect();
        //new PatriciaTest().testMultipleSelect();
    }
}
