package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.util.Base32;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.Data;

@Singleton
public final class StaticMessages implements Service {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    private volatile QueryReply limeReply;
    
    private final QueryReplyFactory queryReplyFactory;
    private final Provider<SimppManager> simppManager;
    
    @Inject
    public StaticMessages(QueryReplyFactory queryReplyFactory, Provider<SimppManager> simppManager) {
        this.queryReplyFactory = queryReplyFactory;
        this.simppManager = simppManager;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Static Messages");
    }
    
    public void initialize() {
    }
    public void stop() {
    }    
   
    public void start() {
        reloadMessages();
        simppManager.get().addListener(new SimppListener() {
            public void simppUpdated(int newVersion) {
                reloadMessages();
            }
        });
    }
    
    private void reloadMessages() {
        limeReply = createLimeReply();
    }
    
    private QueryReply createLimeReply() {
        byte [] reply = Base32.decode(SearchSettings.LIME_SIGNED_RESPONSE.get());
        return createReply(new ByteArrayInputStream(reply));
    }
    
    private QueryReply createReply(InputStream source) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(source);
            byte[] payload = ((Data) in.readObject()).data;
            return queryReplyFactory.createFromNetwork(new byte[16], (byte) 1,
                    (byte) 0, payload);
        } catch (Throwable t) {
            LOG.error("Unable to read serialized data", t);
            return null;
        } finally {
            IOUtils.close(in);
        }
    }
    
    public QueryReply getLimeReply() {
        return limeReply;
    }
}
