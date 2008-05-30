package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Library extends IQ {
    private RemoteFile[] allSharedFileDescriptors;
    
    public Library(XmlPullParser parser) {
        ArrayList<RemoteFile> results = new ArrayList<RemoteFile>();
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("file")) {
                        String urn = parser.getAttributeValue(null, "id");
                        String name = parser.getAttributeValue(null, "name");
                        results.add(new RemoteFile(urn, name));
                        //System.out.println(results.size() - 1 + ": " + results.get(results.size() - 1));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("library")) {
                        allSharedFileDescriptors = results.toArray(new RemoteFile[]{});
                        return;
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XmlPullParserException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        
    }

    public Library(RemoteFile[] allSharedFileDescriptors) {
        this.allSharedFileDescriptors = allSharedFileDescriptors;
    }
    
    public Library() {
        
    }

    public RemoteFile[] getAllSharedFileDescriptors() {
        return allSharedFileDescriptors;
    }

    public String getChildElementXML() {
        if(allSharedFileDescriptors != null) {
            return "<library xmlns=\"jabber:iq:lw-library\">" + toXML(allSharedFileDescriptors) + "</library>";
        } else {
            return "<library xmlns=\"jabber:iq:lw-library\"/>";
        }
        
    }

    private String toXML(RemoteFile[] allSharedFileDescriptors) {
        StringBuilder builder = new StringBuilder();
        for(RemoteFile file : allSharedFileDescriptors) {
            builder.append("<file name=\"" + file.getName() + "\" ");
            builder.append("id=\"" + file.getId() + "\" />\n");
        }
        System.out.println(builder.toString());
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
