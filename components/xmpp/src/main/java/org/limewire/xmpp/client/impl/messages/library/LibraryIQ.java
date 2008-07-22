package org.limewire.xmpp.client.impl.messages.library;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.LibraryProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
    
    private LibraryProvider libraryProvider;
    private FileMetaData[] files;

    public LibraryIQ(XmlPullParser parser) throws IOException, XmlPullParserException {
        files = parseFiles(parser);
    }

    private FileMetaData[] parseFiles(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("file")) {
                    files.add(new FileMetaDataImpl(parser));
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("library")) {
                    return files.toArray(new FileMetaData[]{});
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        return files.toArray(new FileMetaData[]{});
    }

    public LibraryIQ(LibraryProvider libraryProvider) {
        this.libraryProvider = libraryProvider;
    }
    
    public FileMetaData[] getFiles() {
        return files;
    }
    
    public LibraryIQ() {
        
    }

    public String getChildElementXML() {
        if(libraryProvider != null) {
            return "<library xmlns=\"jabber:iq:lw-library\">" + toXML(libraryProvider) + "</library>";
        } else {
            return "<library xmlns=\"jabber:iq:lw-library\"/>";
        }
        
    }

    private String toXML(LibraryProvider libraryProvider) {
        StringBuilder builder = new StringBuilder();
        Iterator<FileMetaData> files = libraryProvider.getFiles();
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
