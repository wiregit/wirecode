package org.limewire.mojito.util;

import org.limewire.mojito.DefaultDHT;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.BootstrapManager.State;

public class UnitTestUtils {
    
    private UnitTestUtils() {
    }
    
    public static void setBooting(MojitoDHT dht, boolean booting) {
        setState(dht, booting ? State.BOOTING : State.UNDEFINED);
    }
    
    public static void setReady(MojitoDHT dht, boolean ready) {
        setState(dht, ready ? State.READY : State.UNDEFINED);
    }
    
    public static void setState(MojitoDHT dht, State state) {
        ((DefaultDHT)dht).getBootstrapManager().setCustomState(state);
    }
}
