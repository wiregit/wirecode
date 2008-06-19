package org.jivesoftware.smackx.packet.file;

import java.io.File;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.file.FileMediaNegotiator;
import org.jivesoftware.smackx.jingle.file.ReceiverFileContentHandler;
import org.jivesoftware.smackx.jingle.file.UserAcceptor;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileDescription extends Description {
    public static final String NAMESPACE = "urn:xmpp:tmp:jingle:apps:file-transfer";
    
    private static UserAcceptor userAcceptor;
    private File saveDir;
    
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
        return new ReceiverFileContentHandler(getFileContainer(),  userAcceptor, saveDir);
    }
    
    public static void setUserAccptor(UserAcceptor userAccptor) {
        FileDescription.userAcceptor = userAccptor;
    }
    
    public void setSaveDir(File saveDir) {
        this.saveDir = saveDir;
    }

    public abstract static class FileContainer implements PacketExtension  {
        FileMediaNegotiator.JingleFile file;
        
        public FileContainer(StreamInitiation.File file) {
            this.file = new FileMediaNegotiator.JingleFile(file);
        }
        
        public FileContainer(FileMediaNegotiator.JingleFile file) {
            this.file = file;
        }

        public String getNamespace() {
            return "";
        }

        public FileMediaNegotiator.JingleFile getFile() {
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
        
        public Offer(FileMediaNegotiator.JingleFile file) {
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
        
        public Request(FileMediaNegotiator.JingleFile file) {
            super(file);
        }

        public String getElementName() {
            return NODENAME;
        }
    }
}
