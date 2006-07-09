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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.util.ArrayUtils;
import com.limegroup.mojito.util.PatriciaTrie;
import com.limegroup.mojito.util.Trie;
import com.limegroup.mojito.util.TrieUtils;


public class PatriciaTest extends BaseTestCase {
    
    public PatriciaTest(String name) {
        super(name);
    }

    public static FutureChainTest suite() {
        return buildTestSuite(PatriciaTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testNames() {
        String[] keys = { "Albert", "Xavier", "XyZ", "Anna",
                "Alien", "Alberto", "Alberts", "Allie", "Alliese", "Alabama",
                "Banane", "Blabla", "Amber", "Ammun", "Akka", "Akko",
                "Albertoo", "Amma" };

        Trie<KUID, String> trie = new PatriciaTrie<KUID, String>();
        for (int i = 0; i < keys.length; i++) {
            trie.put(toKUID(keys[i]), keys[i]);
        }

        int k = 6;
        List<String> list = TrieUtils.select(trie, toKUID("Albert"), k);
        assertEquals(k, list.size());
        
        /*for (Iterator it = list.iterator(); it.hasNext(); ) {
            String name = (String)it.next();
            System.out.println(name);
        }
        System.out.println();*/
        
        list = trie.range(toKUID("Brasil"), 8);
        assertEquals(2, list.size());
        
        /*for (Iterator it = list.iterator(); it.hasNext(); ) {
            String name = (String)it.next();
            System.out.println(name);
        }*/
    }
    
    private static KUID toKUID(String key) {
        byte[] b = key.getBytes();
        
        byte[] id = new byte[20];
        System.arraycopy(b, 0, id, 0, Math.min(b.length, id.length));
        
        return KUID.createNodeID(id);
    }
    
    public void testRange() {
        PatriciaTrie trie = new PatriciaTrie();
        for(int i = 0; i < 50; i++) {
            KUID id = KUID.createRandomMessageID();
            trie.put(id, id);
        }
        
        List list = trie.range(KUID.createRandomMessageID(), -1);
        
        assertFalse(trie.isEmpty());
        assertFalse(list.isEmpty());
        assertEquals(trie.size(), list.size());
    }
    
    // Not an unit test!
    public void /*test*/MultipleSelect() {
        
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
        
        Trie<KUID, KUID> trie = new PatriciaTrie<KUID, KUID>();
        for(int i = 0; i < nodes.length; i++) {
            KUID kuid = KUID.createNodeID(ArrayUtils.parseHexString(nodes[i]));
            trie.put(kuid, kuid);
            
            System.out.println(kuid + " = " + kuid.toBinString());
        }
        
        //System.out.println(trie);
        
        KUID key = KUID.createNodeID(ArrayUtils.parseHexString(lookup));
        List<KUID> items = TrieUtils.select(trie, key, nodes.length);
        
        System.out.println();
        System.out.println("Key: \n" + key + " = " + key.toBinString() + "\n");
        
        for(Iterator it = items.iterator(); it.hasNext(); ) {
            KUID kuid = (KUID)it.next();
            System.out.println(kuid + " = " + kuid.toBinString());
        }
    }
    
    // Not an unit test!
    private void /*test*/Select() {
        KUID lookup = KUID.createNodeID(ArrayUtils.parseHexString("C814CF8CF039760D1399720BDBBD10F5327DB5A9"));
        KUID inverted = lookup.invert();
        
        String[] nodes = {
            "D74E1C48D7299B6935EE0DEC9C72A52205ED6098",
            "F735922C5C8B54FFD9424EB46066236E7D5BBA8D",
            "8D177936F0C6A3CC171020B3245C9CF106958603",
            "9B21E976DF82D5117ED1C14DE4E0D3A0586A8B35",
            "94850358CEFD33CD0BDE1F468AEA1E646B3F8724"
        };
        
        Trie<KUID, KUID> trie = new PatriciaTrie<KUID, KUID>();
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
        List<KUID> items = TrieUtils.select(trie, inverted, trie.size());
        for(Iterator it = items.iterator(); it.hasNext(); ) {
            KUID kuid = (KUID)it.next();
            System.out.println(kuid + " = " + kuid.toBinString());
        }
        
        System.out.println();
        KUID best = (KUID)trie.select(lookup);
        System.out.println("Selected for lookup: " + best);
        
        System.out.println();
        KUID selected = (KUID)trie.select(inverted);
        System.out.println(trie.size());
        System.out.println("Selected for inverted: " + selected);
        
        System.out.println();
        System.out.println(TrieUtils.select(trie, inverted, 1));
    }
    
    // Not an unit test!
    private void /*test*/RandomRemove() {
        PatriciaTrie trie = new PatriciaTrie();
        
        for(int i = 0; i < 100000; i++) {
            Set keys = new HashSet();
            
            for(int j = 0; j < 1000; j++) {
                KUID key = KUID.createRandomNodeID();
                keys.add(key);
                trie.put(key, key);
            }
            
            if (keys.size() != trie.size()) {
                throw new IllegalStateException(keys.size() + " != " + trie.size());
            }
            
            for(Iterator it = keys.iterator(); it.hasNext(); ) {
                KUID key = (KUID)it.next();
                it.remove();
                
                KUID rem = (KUID)trie.remove(key);
                
                if (rem == null) {
                    throw new IllegalStateException("null");
                }
                
                if (!key.equals(rem)) {
                    throw new IllegalStateException("Expected: " + key + " but got: " + rem);
                }
                
                for(Iterator jt = keys.iterator(); jt.hasNext(); ) {
                    key = (KUID)jt.next();
                    
                    if (!trie.containsKey(key)) {
                        throw new IllegalStateException(key + " not found in Trie");
                    }
                }
            }
            
            if (trie.size() != 0) {
                throw new IllegalStateException("Trie is not empty");
            }
            
            if (i % 1000 == 0) {
                System.out.println(100f * i / 100000 + "%");
            }
        }
    }
}
