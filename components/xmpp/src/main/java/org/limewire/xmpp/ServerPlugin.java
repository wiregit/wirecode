package org.limewire.xmpp;

import java.io.File;
import java.util.ArrayList;

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


        ArrayList<File> shared = new ArrayList<File>();
        // TODO lookup shared dirs in config
        shared.add(new File("C:/shared/"));
        QueryHandler queryHandler = new QueryHandler("limewire module", modeHandler.getUltrapeers(), modeHandler.getLeaves(), XMPPServer.getInstance().getRoutingTable(), true, shared);
        iqRouter.addHandler(queryHandler);
        
        // TODO establish connections with other ultrapeers
    }

    public void destroyPlugin() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
