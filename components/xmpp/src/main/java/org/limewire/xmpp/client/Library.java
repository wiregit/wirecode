package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Library extends IQ {
    private File[] allSharedFileDescriptors;
    
    public Library(XmlPullParser parser) {
        ArrayList<File> results = new ArrayList<File>();
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("file")) {
                        String urn = parser.getAttributeValue(null, "id");
                        String name = parser.getAttributeValue(null, "name");
                        results.add(new File(urn, name));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("library")) {
                        allSharedFileDescriptors = results.toArray(new File[]{});
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

    public Library(File[] allSharedFileDescriptors) {
        this.allSharedFileDescriptors = allSharedFileDescriptors;
    }
    
    public Library() {
        
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
            return new Library(parser);
        }
    }
}
