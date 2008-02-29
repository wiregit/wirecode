package org.limewire.xmpp;

import java.io.File;

import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;

public class ServerPlugin implements Plugin {
    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        NetworkModeHandler modeHandler = new NetworkModeHandler("limewire module");
        IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
        iqRouter.addHandler(modeHandler);
        
        IQHandler pingHandler = new PingHandler("limewire module", modeHandler.getUltrapeers());
        iqRouter.addHandler(pingHandler);
        
        // TODO establish connections with other ultrapeers
    }

    public void destroyPlugin() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
