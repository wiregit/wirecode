package org.limewire.xmpp.client;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LibraryIQ extends IQ {
    private File[] allSharedFileDescriptors;
    private XmlPullParser parser;
    
    public LibraryIQ(XmlPullParser parser) {
        this.parser = parser;
    }

    void parseFiles(LibraryListener listener) {
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("file")) {
                        String urn = parser.getAttributeValue(null, "id");
                        String name = parser.getAttributeValue(null, "name");
                        listener.fileAdded(new File(urn, name));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("library")) {
                        return;
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            e.printStackTrace(); // TODO log, throw?
        } catch (XmlPullParserException e) {
            e.printStackTrace(); // TODO log, throw?
        }
    }

    public LibraryIQ(File[] allSharedFileDescriptors) {
        this.allSharedFileDescriptors = allSharedFileDescriptors;
    }
    
    public LibraryIQ() {
        
    }

    public File[] getAllSharedFileDescriptors() {
        return allSharedFileDescriptors;
    }

    public String getChildElementXML() {
        if(allSharedFileDescriptors != null) {
            return "<library xmlns=\"jabber:iq:lw-library\">" + toXML(allSharedFileDescriptors) + "</library>";
        } else {
            return "<library xmlns=\"jabber:iq:lw-library\"/>";
        }
        
    }

    private String toXML(File[] allSharedFileDescriptors) {
        StringBuilder builder = new StringBuilder();
        for(File file : allSharedFileDescriptors) {
            builder.append("<file name=\"" + file.getName() + "\" ");
            builder.append("id=\"" + file.getId() + "\" />\n");
        }
        return builder.toString();
    }

    public static IQProvider getIQProvider() {
        return new LibraryIQProvider();
    }

    private static class LibraryIQProvider implements IQProvider {
        public IQ parseIQ(XmlPullParser parser) throws Exception {                     
            return new LibraryIQ(parser);
        }
    }
}
