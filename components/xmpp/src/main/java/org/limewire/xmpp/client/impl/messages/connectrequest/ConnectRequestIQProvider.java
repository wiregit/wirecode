package org.limewire.xmpp.client.impl.messages.connectrequest;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ConnectRequestIQProvider implements IQProvider {

    private static Log LOG = LogFactory.getLog(ConnectRequestIQProvider.class);
    
    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        try {
            return new ConnectRequestIQ(parser);
        } catch (RuntimeException re) {
            LOG.debug("runtime", re);
            throw re;
        } catch (IOException ie) {
            LOG.debug("io", ie);
            throw ie;
        } catch (XmlPullParserException e) {
            LOG.debug("xml", e);
            throw e;
        }
    }

}
