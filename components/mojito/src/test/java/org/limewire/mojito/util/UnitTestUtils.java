package org.limewire.mojito.util;

import org.limewire.mojito2.DefaultDHT;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.DefaultDHT.State;

public class UnitTestUtils {
    
    private UnitTestUtils() {
    }
    
    public static void setBooting(MojitoDHT dht, boolean booting) {
        setState(dht, booting ? State.BOOTING : State.INIT);
    }
    
    public static void setReady(MojitoDHT dht, boolean ready) {
        setState(dht, ready ? State.READY : State.INIT);
    }
    
    public static void setState(MojitoDHT dht, State state) {
        ((DefaultDHT)dht).setState(state);
    }
}
