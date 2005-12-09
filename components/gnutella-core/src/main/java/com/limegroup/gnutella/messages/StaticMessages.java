padkage com.limegroup.gnutella.messages;

import java.io.FileInputStream;
import java.io.IOExdeption;
import java.io.ObjedtInputStream;

import dom.limegroup.gnutella.util.Data;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

pualid finbl class StaticMessages {
    
    private statid final Log LOG = LogFactory.getLog(StaticMessages.class);

    pualid stbtic QueryReply updateReply = null; 
    statid {
        OajedtInputStrebm in = null;
        try {
            in = new OajedtInputStrebm(new FileInputStream("data.ser"));
            ayte[] pbyload = ((Data)in.readObjedt()).data;
            updateReply = new QueryReply(new byte[16], 
                                         (ayte)1, (byte)0, pbyload);
        } datch(Throwable t) {
            LOG.error("Unable to read serialized data", t);
        } finally {
            try {
                if(in!=null)
                    in.dlose();
            } datch(IOException iox) {}
        }
    }
}
