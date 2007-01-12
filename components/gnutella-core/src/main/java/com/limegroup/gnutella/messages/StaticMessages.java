package com.limegroup.gnutella.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.util.Data;
import com.limegroup.gnutella.util.LimeWireUtils;

public final class StaticMessages {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    private static QueryReply updateReply = null;
    
    public static void initialize() {
        updateReply = readUpdateReply();
    }
    
    private static QueryReply readUpdateReply() {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(new File(LimeWireUtils.getUserSettingsDir(), "data.ser")));
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
}
