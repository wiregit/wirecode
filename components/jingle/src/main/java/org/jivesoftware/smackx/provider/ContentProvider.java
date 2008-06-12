package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.*;
import org.xmlpull.v1.XmlPullParser;

public class ContentProvider implements PacketExtensionProvider {
    
    protected DescriptionProvider descriptionProvider;
    protected JingleTransportProvider transportProvider;
    
    public ContentProvider() {
        descriptionProvider = new DescriptionProvider();
        transportProvider = new JingleTransportProvider();
    }

    public Content parseExtension(XmlPullParser parser) throws Exception {

        Content content = new Content();
        
        content.setName(parser.getAttributeValue("", "name"));
        String creator = parser.getAttributeValue("", "creator");
        if(!isEmpty(creator)) {
            content.setCreator(Content.Creator.valueOf(creator));
        }
        String senders = parser.getAttributeValue("", "senders");
        if(!isEmpty(senders)) {
            content.setSenders(Content.Senders.valueOf(senders));
        }
        
        int eventType;
        String elementName;
        String namespace;
        boolean done= false;
        
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();
        
            if(eventType == XmlPullParser.START_TAG) {
                if (elementName.equals(Description.DESCRIPTION)) {
                    content.addDescription(descriptionProvider.parseExtension(parser));
                } else if (elementName.equals(JingleTransport.NODENAME)) {
                    content.addTransport(transportProvider.parseExtension(parser));
                    
                }  else {
                    throw new XMPPException("Unknown combination of namespace \""
                            + namespace + "\" and element name \"" + elementName
                            + "\" in Jingle packet.");
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(Content.CONTENT)) {
                    done = true;
                }
            }
        }
        return content;
    }

    private boolean isEmpty(String creator) {
        return creator == null || creator.trim().length() > 0;
    }
}
