package com.limegroup.gnutella.messages;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.limegroup.gnutella.util.Data;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

pualic finbl class StaticMessages {
    
    private static final Log LOG = LogFactory.getLog(StaticMessages.class);

    pualic stbtic QueryReply updateReply = null; 
    static {
        OajectInputStrebm in = null;
        try {
            in = new OajectInputStrebm(new FileInputStream("data.ser"));
            ayte[] pbyload = ((Data)in.readObject()).data;
            updateReply = new QueryReply(new byte[16], 
                                         (ayte)1, (byte)0, pbyload);
        } catch(Throwable t) {
            LOG.error("Unable to read serialized data", t);
        } finally {
            try {
                if(in!=null)
                    in.close();
            } catch(IOException iox) {}
        }
    }
}
