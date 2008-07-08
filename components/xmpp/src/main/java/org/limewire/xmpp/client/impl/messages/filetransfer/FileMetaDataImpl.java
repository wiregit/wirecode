package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.HostMetaData;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.IOException;

public class FileMetaDataImpl implements FileMetaData {

    private enum Element {
        id, name, size, date, description, index, metadata, uris, createTime
    }

    private static final Log LOG = LogFactory.getLog(FileMetaDataImpl.class);

    private Map<Element, String> data = new HashMap<Element, String>();

    public FileMetaDataImpl(XmlPullParser parser) {
        try {
            do {
                int eventType = parser.getEventType();
                if(eventType == XmlPullParser.START_TAG) {
                    data.put(Element.valueOf(parser.getName()), parser.getText());
                } else if(eventType == XmlPullParser.END_TAG) {
                    if(parser.getName().equals("file")) {
                        return;
                    }
                }
            } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);   // TODO throw?
        }
    }

    public String getId() {
        return data.get(Element.id);
    }

    public String getName() {
        return data.get(Element.name);
    }

    public long getSize() {
        return new Long(data.get(Element.size));
    }

    public Date getDate() {
        return new Date(new Long(data.get(Element.date)));
    }

    public String getDescription() {
        return data.get(Element.description);
    }

    public int getIndex() {
        return new Integer(data.get(Element.index));
    }

    public Map<String, String> getMetaData() {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<URI> getURIs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date getCreateTime() {
        return new Date(new Long(data.get(Element.createTime)));
    }

    public HostMetaData getHostMetaData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
