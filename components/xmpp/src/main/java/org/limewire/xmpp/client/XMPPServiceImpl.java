package org.limewire.xmpp.client;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.lifecycle.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class XMPPServiceImpl implements Service, XMPPService {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);
    
    public static final String LW_SERVICE_NS = "http://www.limewire.org/";
    
    private CopyOnWriteArrayList<XMPPConnection> connections;
    private final Provider<List<XMPPConnectionConfiguration>> configurations;
    private final LibrarySource librarySource;
    private final IncomingFileAcceptor incomingFileAcceptor;
    private final FileTransferProgressListener progressListener;

    @Inject
    public XMPPServiceImpl(Provider<List<XMPPConnectionConfiguration>> configurations,
                       LibrarySource librarySource,
                       IncomingFileAcceptor incomingFileAcceptor,
                       FileTransferProgressListener progressListener) {
        this.configurations = configurations;
        this.librarySource = librarySource;
        this.incomingFileAcceptor = incomingFileAcceptor;
        this.progressListener = progressListener;
        this.connections = new CopyOnWriteArrayList<XMPPConnection>();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    public void initialize() {
        for(XMPPConnectionConfiguration configuration : configurations.get()) {
            addConnectionConfiguration(configuration);
        }
    }

    /**
     * Logs into the xmpp service specified in the <code>XMPPConfiguration</code>
     */
    public void start() {
        for(XMPPConnection connection : connections) {
            if(connection.getConfiguration().isAutoLogin()) {
                try {
                    connection.login();
                } catch (XMPPException e) {
                    LOG.error(e.getMessage(), e);
                    connection.getConfiguration().getErrorListener().error(e);
                }
            }
        }        
    }

    /**
     * Disconnects from the xmpp server
     */
    public void stop() {
        for(XMPPConnection connection : connections) {
            connection.logout();
        }
    }

    public String getServiceName() {
        return "XMPP";
    }

    public List<XMPPConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration) {
        XMPPConnectionImpl connection = new XMPPConnectionImpl(configuration, librarySource, incomingFileAcceptor, progressListener);
        connection.initialize();
        connections.add(connection);
    }
}
