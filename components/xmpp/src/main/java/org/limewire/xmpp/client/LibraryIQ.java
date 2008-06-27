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

/**
 * Used to (de)serialize <code>library</code> IQ messages.  These messages look like this on the wire:<BR>
 *
 * <pre><iq id="8743kp" from="limebuddy1@gmail.com/limewire12345" to="limebuddy2@gmail.com/limewire678910" type="get">
 *      <library xmlns="jabber:iq:lw-library"/>
 * </iq>
 *
 * <iq id="8743kp" from="limebuddy2@gmail.com/limewire678910" to="limebuddy1@gmail.com/limewire12345" type="result">
 *      <library xmlns="jabber:iq:lw-library">
 *          <file name="foo.txt" id="yhd7w9whh773a0"/>
 *          <file name="bar.txt" id="dhcehfr940ekfj"/>
 *      </library>
 * </iq></pre>
 */
public class LibraryIQ extends IQ {

    private static final Log LOG = LogFactory.getLog(LibraryIQ.class);

    private XmlPullParser parser;
    private LibrarySource librarySource;
    private FileMetaData[] files;

    public LibraryIQ(XmlPullParser parser) {
        this.parser = parser;
        files = parseFiles();
    }

    FileMetaData[] parseFiles() {
        ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    if(parser.getName().equals("file")) {
                        String urn = parser.getAttributeValue(null, "id");
                        String name = parser.getAttributeValue(null, "name");
                        // TODO size, date, description
                        files.add(new FileMetaDataImpl(urn, name));
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("library")) {
                        return files.toArray(new FileMetaDataImpl[]{});
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        }
        return files.toArray(new FileMetaDataImpl[]{});
    }

    public LibraryIQ(LibrarySource librarySource) {
        this.librarySource = librarySource;
    }
    
    public FileMetaData[] getFiles() {
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
        Iterator<FileMetaData> files = librarySource.getFiles();
        while(files.hasNext()) {
            FileMetaData file = files.next();
            builder.append("<file name=\"" + file.getName() + "\" ");
            builder.append("id=\"" + file.getId() + "\" />\n");
            // TODO size, date, description
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
