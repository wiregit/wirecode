package org.limewire.xmpp.client.impl.messages.connectrequest;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class ConnectRequestIQProvider implements IQProvider {

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        return new ConnectRequestIQ(parser);
    }

}
