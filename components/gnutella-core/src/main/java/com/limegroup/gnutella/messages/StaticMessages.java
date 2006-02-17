package com.limegroup.gnutella.messages;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.limegroup.gnutella.util.Data;
import com.limegroup.gnutella.util.IOUtils;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public final class StaticMessages {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    public static QueryReply updateReply = null; 
    static {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream("data.ser"));
            byte[] payload = ((Data)in.readObject()).data;
            updateReply = new QueryReply(new byte[16], 
                                         (byte)1, (byte)0, payload);
        } catch(Throwable t) {
            LOG.error("Unable to read serialized data", t);
        } finally {
            IOUtils.close(in);
        }
    }
}
