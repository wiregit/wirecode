package org.limewire.xmpp.client.impl.messages.authtoken;

import java.io.IOException;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AuthTokenIQProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        try {
            return new AuthTokenIQ(parser);
        } catch (IOException ie) {
            return new ExceptionalAuthTokenIQ(ie); 
        } catch (XmlPullParserException xmpe) {
            return new ExceptionalAuthTokenIQ(xmpe);
        }
    }
    
    public static class ExceptionalAuthTokenIQ extends AuthTokenIQ {

        private final Exception exception;

        public ExceptionalAuthTokenIQ(Exception exception) {
            this.exception = exception;
        }
        
        public Exception getException() {
            return exception;
        }
        
    }
}
