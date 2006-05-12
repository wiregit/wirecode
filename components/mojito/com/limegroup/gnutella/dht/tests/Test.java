package com.limegroup.gnutella.dht.tests;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.ContextSettings;

public class Test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        KUID nodeId = KUID.createRandomNodeID();
        ContextSettings.setLocalNodeInstanceID(nodeId, 3);
        System.out.println(ContextSettings.getLocalNodeInstanceID(nodeId));
    }

}
