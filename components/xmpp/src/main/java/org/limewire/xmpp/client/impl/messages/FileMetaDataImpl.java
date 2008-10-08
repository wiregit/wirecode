package org.limewire.xmpp.client.impl.messages;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.net.address.FirewalledAddress;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FileMetaDataImpl implements FileMetaData {

    private enum Element {
        id, name, size, description, index, metadata, urns, createTime
    }

    private final Map<Element, String> data = new HashMap<Element, String>();

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

    public FileMetaDataImpl(Node fileNode) {
        Node firstNode = fileNode.getFirstChild();
        if (!firstNode.getNodeName().equals("file")) {
            throw new IllegalArgumentException("Invalid XML");
        }
        NodeList nodes = firstNode.getChildNodes();
        int numberOfNodes = nodes.getLength();
        for (int i=0; i<numberOfNodes; i++) {
            Node node = nodes.item(i);
            String key = node.getNodeName();
            String value = node.getTextContent();
            data.put(Element.valueOf(key), value);
        }

    }
    
    public FileMetaDataImpl(FileMetaData metaData) throws IOException {
        setCreateTime(metaData.getCreateTime());
        setDescription(metaData.getDescription());
        setId(metaData.getId());
        setIndex(metaData.getIndex());
        setName(metaData.getName());
        setSize(metaData.getSize());
        setURNs(metaData.getURNs());
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

    public Set<URN> getURNs() throws IOException {
        StringTokenizer st = new StringTokenizer(data.get(Element.urns), " ");
        HashSet<URN> set = new HashSet<URN>();
        while(st.hasMoreElements()) {
            set.add(URN.createUrnFromString((st.nextToken())));
        }
        return set;
    }
    
    public void setURNs(Set<URN> urns) {
        String urnsString = "";
        for(URN urn : urns) {
            urnsString += urn  + " ";
        }
        data.put(Element.urns, urnsString);
    }

    public Date getCreateTime() {
        return new Date(Long.valueOf(data.get(Element.createTime)));
    }
    
    public void setCreateTime(Date date) {
        data.put(Element.createTime, Long.toString(date.getTime()));
    }

    public String toXML() {
        // TODO StringBuilder instead of concats
        String fileMetadata = "<file>";
        for(Element element : data.keySet()) {
            fileMetadata += "<" + element.toString() + ">";
            fileMetadata += data.get(element);
            fileMetadata += "</" + element.toString() + ">";
        }
        fileMetadata += "</file>";
        return fileMetadata;
    }

    public RemoteFileDesc toRemoteFileDesc(LimePresence presence, 
                                           RemoteFileDescFactory rfdFactory) throws IOException {
        Connectable publicAddress;
        Address address = presence.getPresenceAddress();
        byte[] clientGuid = null;
        boolean firewalled;
        Set<Connectable> proxies = null;
        int fwtVersion = 0;
        Set<URN> urns = getURNs();

        if (address instanceof FirewalledAddress) {
            firewalled = true;
            FirewalledAddress fwAddress = (FirewalledAddress)address;
            publicAddress = fwAddress.getPublicAddress();
            clientGuid = fwAddress.getClientGuid().bytes();
            proxies = fwAddress.getPushProxies();
            fwtVersion = fwAddress.getFwtVersion();
        } else {
            assert address instanceof Connectable;
            firewalled = false;
            publicAddress = (Connectable)address;
        }

        return rfdFactory.createRemoteFileDesc(publicAddress.getAddress(), publicAddress.getPort(),
                getIndex(), getName(), getSize(), clientGuid, 0, false, 0, true, null, urns, false,
                firewalled, null, proxies, getCreateTime().getTime(), fwtVersion,
                publicAddress.isTLSCapable());

    }
}
