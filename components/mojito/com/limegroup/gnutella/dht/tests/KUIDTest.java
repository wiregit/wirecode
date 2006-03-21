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
    
    public static void main(String[] args) {
        new KUIDTest().testIsCloser();
    }
}
