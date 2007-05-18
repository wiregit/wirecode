package com.limegroup.server;

import java.util.Map;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limewire.store.server.main.AbstractDispatchee;
import com.limewire.store.server.main.Dispatchee;
import com.limewire.store.server.main.LocalServer;

/**
 * Default {@link Dispatchee} class that handles commands from the local server.
 * 
 * @author jpalm
 */
public class DispatcheeImpl extends AbstractDispatchee {

    public DispatcheeImpl(LocalServer server) {
        super(server);
    }

    public String dispatch(String cmd, Map<String, String> args) {
        return null;
    }

    protected void connectionChanged(boolean isConnected) {
        System.out.println("noteConnected:" + isConnected);
        GUIMediator.instance().getStatusLine().updateStoreLabel(!isConnected);
    }
}
