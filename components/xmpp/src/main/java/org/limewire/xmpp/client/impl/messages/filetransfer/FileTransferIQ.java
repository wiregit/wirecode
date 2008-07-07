package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileTransferIQ extends IQ {
    
    private static final Log LOG = LogFactory.getLog(FileTransferIQ.class);
    
    public FileTransferIQ(XmlPullParser parser) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public String getChildElementXML() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    public static IQProvider getIQProvider() {
        return new FileTransferIQProvider();
    }

    private static class FileTransferIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {                     
            return new FileTransferIQ(parser);
        }
    }
}
