package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.Data;

public final class StaticMessages {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    private static volatile QueryReply updateReply, limeReply;
    
    public static void initialize() {
        reloadMessages();
        SimppManager.instance().addListener(new SimppListener() {
            public void simppUpdated(int newVersion) {
                reloadMessages();
            }
        });
    }
    
    private static void reloadMessages() {
        updateReply = readUpdateReply();
        limeReply = createLimeReply();
    }
    
    private static QueryReply readUpdateReply() {
        try {
            return createReply(new FileInputStream(new File(CommonUtils.getUserSettingsDir(), "data.ser")));
        } catch (FileNotFoundException bad) {
            return null;
        }
    }
    
    private static QueryReply createLimeReply() {
        byte [] reply = Base32.decode(SearchSettings.LIME_SIGNED_RESPONSE.getValue());
        return createReply(new ByteArrayInputStream(reply));
    }
    
    private static QueryReply createReply(InputStream source) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(source);
            byte[] payload = ((Data) in.readObject()).data;
            return new QueryReply(new byte[16], (byte) 1, (byte) 0, payload);
        } catch (Throwable t) {
            LOG.error("Unable to read serialized data", t);
            return null;
        } finally {
            IOUtils.close(in);
        }
    }
    
    public static QueryReply getUpdateReply() {
        return updateReply;
    }
    
    public static QueryReply getLimeReply() {
        return limeReply;
    }
}
