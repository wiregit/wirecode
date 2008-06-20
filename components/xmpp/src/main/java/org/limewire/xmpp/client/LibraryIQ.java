package org.limewire.xmpp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LibraryIQ extends IQ {

    private static final Log LOG = LogFactory.getLog(LibraryIQ.class);

    private XmlPullParser parser;
    private LibrarySource librarySource;
    private File [] files;

    public LibraryIQ(XmlPullParser parser) {
        this.parser = parser;
        files = parseFiles();
    }

    File [] parseFiles() {
        ArrayList<File> files = new ArrayList<File>();
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("file")) {
                        String urn = parser.getAttributeValue(null, "id");
                        String name = parser.getAttributeValue(null, "name");
                        files.add(new File(urn, name));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("library")) {
                        return files.toArray(new File[]{});
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        }
        return files.toArray(new File[]{});
    }

    public LibraryIQ(LibrarySource librarySource) {
        this.librarySource = librarySource;
    }
    
    public File [] getFiles() {
        return files;
    }
    
    public LibraryIQ() {
        
    }

    public String getChildElementXML() {
        if(librarySource != null) {
            return "<library xmlns=\"jabber:iq:lw-library\">" + toXML(librarySource) + "</library>";
        } else {
            return "<library xmlns=\"jabber:iq:lw-library\"/>";
        }
        
    }

    private String toXML(LibrarySource librarySource) {
        StringBuilder builder = new StringBuilder();
        Iterator<File> files = librarySource.getFiles();
        while(files.hasNext()) {
            File file = files.next();
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
