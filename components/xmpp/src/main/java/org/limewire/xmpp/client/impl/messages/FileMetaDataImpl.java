package org.limewire.xmpp.client.impl.messages;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.xmpp.client.service.FileMetaData;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FileMetaDataImpl implements FileMetaData {

    private enum Element {
        id, name, size, description, index, metadata, uris, createTime
    }

    private Map<Element, String> data = new HashMap<Element, String>();

    public FileMetaDataImpl(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.nextTag();
        do {            
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                data.put(Element.valueOf(parser.getName()), parser.nextText());
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("file")) {
                    return;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }
    
    public FileMetaDataImpl(FileMetaData metaData) throws URISyntaxException {
        setCreateTime(metaData.getCreateTime());
        setDescription(metaData.getDescription());
        setId(metaData.getId());
        setIndex(metaData.getIndex());
        setName(metaData.getName());
        setSize(metaData.getSize());
        setURIs(metaData.getURIs());
    }
    
    public FileMetaDataImpl() {
        
    }

    public String getId() {
        return data.get(Element.id);
    }
    
    public void setId(String id) {
        data.put(Element.id, id);
    }

    public String getName() {
        return data.get(Element.name);
    }
    
    public void setName(String name) {
        data.put(Element.name, name);
    }

    public long getSize() {
        return Long.valueOf(data.get(Element.size));
    }                                                     
    
    public void setSize(long size) {
        data.put(Element.size, Long.toString(size));
    }

    public String getDescription() {
        return data.get(Element.description);
    }
    
    public void setDescription(String description) {
        data.put(Element.description, description);
    }

    public long getIndex() {
        return Long.valueOf(data.get(Element.index));
    }
    
    public void setIndex(long index) {
        data.put(Element.index, Long.toString(index));
    }

    public Map<String, String> getMetaData() {
        // TODO
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<URI> getURIs() throws URISyntaxException {
        StringTokenizer st = new StringTokenizer(data.get(Element.uris), " ");
        HashSet<URI> set = new HashSet<URI>();
        while(st.hasMoreElements()) {
            set.add(new URI(st.nextToken()));
        }
        return set;
    }
    
    public void setURIs(Set<URI> uris) {
        String urisString = "";
        for(URI uri : uris) {
            urisString += uri  + " ";
        }
        data.put(Element.uris, urisString);
    }

    public Date getCreateTime() {
        return new Date(Long.valueOf(data.get(Element.createTime)));
    }
    
    public void setCreateTime(Date date) {
        data.put(Element.createTime, Long.toString(date.getTime()));
    }

    public String toXML() {
        String fileMetadata = "<file>";
        for(Element element : data.keySet()) {
            fileMetadata += "<" + element.toString() + ">";
            fileMetadata += data.get(element);
            fileMetadata += "</" + element.toString() + ">";
        }
        fileMetadata += "</file>";
        return fileMetadata;
    }
}
