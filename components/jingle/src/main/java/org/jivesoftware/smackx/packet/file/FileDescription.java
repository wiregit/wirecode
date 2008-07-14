package org.jivesoftware.smackx.packet.file;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.file.FileLocator;
import org.jivesoftware.smackx.jingle.file.ReceiverFileContentHandler;
import org.jivesoftware.smackx.jingle.file.UserAcceptor;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileDescription extends Description {
    public static final String NAMESPACE = "urn:xmpp:tmp:jingle:apps:file-transfer";
    
    private static UserAcceptor userAcceptor;
    private static FileLocator fileLocator;
    
    public String getNamespace() {
        return NAMESPACE;
    }

    public FileContainer getFileContainer() {
        return ((FileContainer)children.get(0));
    }

    public void setFileContainer(FileContainer file) {
        children.add(file);
    }

    public JingleContentHandler createContentHandler() {
        return new ReceiverFileContentHandler(getFileContainer(),  userAcceptor, fileLocator);
    }
    
    public static void setUserAccptor(UserAcceptor userAccptor) {
        FileDescription.userAcceptor = userAccptor;
    }
    
    public static void setFileLocator(FileLocator fileLocator) {
        FileDescription.fileLocator = fileLocator;
    }

    public abstract static class FileContainer implements PacketExtension  {
        StreamInitiation.File file;
        
        public FileContainer(StreamInitiation.File file) {
            this.file = file;
        }

        public String getNamespace() {
            return "";
        }

        public StreamInitiation.File getFile() {
            return file;
        }

        public String toXML() {
           StringBuilder buf = new StringBuilder();

            buf.append("<").append(getElementName());
            buf.append(" xmlns=\"" + getNamespace() + "\"");
            buf.append(">");
            buf.append(file.toXML());    
            buf.append("</").append(getElementName()).append(">");
            return buf.toString();                          
        }
    }
    
    public static class Offer extends FileContainer{

        public static String NODENAME = "offer";
        
        public Offer(StreamInitiation.File file) {
            super(file);
        }

        public String getElementName() {
            return NODENAME;
        }
    }
    
    public static class Request extends FileContainer{
        
        public static String NODENAME = "request";

        public Request(StreamInitiation.File file) {
            super(file);
        }

        public String getElementName() {
            return NODENAME;
        }
    }
}
