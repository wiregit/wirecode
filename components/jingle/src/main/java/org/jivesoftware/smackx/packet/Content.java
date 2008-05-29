package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

import java.util.ArrayList;
import java.util.List;

public class Content implements PacketExtension {
   
    public enum Creator {initiator, responder}
    public enum Senders {both, initiator, responder}
    
    protected Creator creator;
    protected String name;
    protected Senders senders;
    
    protected List<Description> descriptions;
    protected List<JingleTransport> transports;

    public static final String CONTENT = "content";
    
    public Content(Creator creator, String name, Senders senders, Description description, JingleTransport transport) {
        this.creator = creator;
        this.name = name;
        this.senders = senders;
        this.descriptions = new ArrayList<Description>();
        if(description != null) {
            this.descriptions.add(description);
        }
        this.transports = new ArrayList<JingleTransport>();
        if(transport != null) {
            this.transports.add(transport);
        }
    }
    
    public Content(Description description) {
        this(null, null, null, description, null);
    }
    
    public Content(JingleTransport transport) {
        this(null, null, null, null, transport);
    }
    
    public Content(Creator creator, String name, Senders senders) {
        this(creator, name, senders, null, null);
    }
    
    public Content() {
        this(null, null, null, null, null);
    }
    
    public void addDescriptions(List<Description> descriptions) {
        this.descriptions.addAll(descriptions);
    }
    
    public void addDescription(Description description) {
        descriptions.add(description);
    }
    
    public void addTransports(List<JingleTransport> transports) {
        this.transports.addAll(transports);
    }
    
    public void addTransport(JingleTransport transport) {
        transports.add(transport);
    }

    public List<Description> getDescriptions() {
        return descriptions;
    }

    public List<JingleTransport> getTransports() {
        return transports;
    }

    public Creator getCreator() {
        return creator;
    }

    public void setCreator(Creator creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Senders getSenders() {
        return senders;
    }

    public void setSenders(Senders senders) {
        this.senders = senders;
    }

    public String getElementName() {
        return CONTENT;
    }

    public String getNamespace() {
        return Jingle.NAMESPACE;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(CONTENT);
        if (creator != null) {
            buf.append(" creator=\"").append(creator).append("\"");
        }
        if (name != null) {
            buf.append(" name=\"").append(name).append("\"");
        }
        if (senders != null) {
            buf.append(" senders=\"").append(senders).append("\"");
        }
        buf.append(">");
        
        synchronized (descriptions) {
            if(descriptions != null) {
                for(Description description : descriptions) {
                    buf.append(description.toXML());
                }
            }
        }

        synchronized (transports) {
            for (JingleTransport trans : transports) {
                buf.append(trans.toXML());
            }
        }

        buf.append("</").append(CONTENT).append(">");
        return buf.toString();
    }
}
