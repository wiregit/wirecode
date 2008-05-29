package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.jingle.JingleContentHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class Description implements PacketExtension {

    public static String DESCRIPTION = "description";
    
    protected List<PacketExtension> children;
    
    protected JingleContentHandler contentHandler;
    
    public Description() {
        children = new ArrayList<PacketExtension>();
    }
    
    protected Attribute[] getAttributes() {
        return new Attribute[]{};
    }
    
    public String getElementName() {
        return DESCRIPTION;    
    }
    
    public JingleContentHandler getContentHandler() {
        if(contentHandler == null) {
            contentHandler = createContentHandler();
        }
        return contentHandler;
    }
    
    public abstract JingleContentHandler createContentHandler();
    
    public String toXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(DESCRIPTION);
        buf.append(" xmlns=\"" + getNamespace() + "\"");
        Attribute [] attrs = getAttributes();
        if(attrs != null && attrs.length > 0) {
            for(Attribute attribute : attrs) {
                buf.append(" " + attribute.name + "=\"").append(attribute.value).append("\"");
            }
        }
        buf.append(">");
        for(PacketExtension child : children) {
            buf.append(child.toXML());
        }

        buf.append("</").append(DESCRIPTION).append(">");
        return buf.toString();
    }
    
    public class Attribute {
        protected String name;
        protected String value;
        
        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }    
}
