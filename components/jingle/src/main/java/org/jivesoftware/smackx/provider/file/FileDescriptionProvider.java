package org.jivesoftware.smackx.provider.file;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.xmlpull.v1.XmlPullParser;

public class FileDescriptionProvider implements PacketExtensionProvider {
    static ProviderManager providerManager = ProviderManager.getInstance();
    IQProvider fileExtensionProvider;
    
    public FileDescriptionProvider() {
        //fileExtensionProvider = (PacketExtensionProvider)providerManager.getIQProvider("file", "http://jabber.org/protocol/si");
        fileExtensionProvider = new FileProvider();
    }
    

    public FileDescription parseExtension(XmlPullParser parser) throws Exception {
        boolean done = false;
        FileDescription desc = new FileDescription();

        while (!done) {
            int eventType = parser.next();
            String name = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals(FileDescription.Offer.NODENAME)) {
                    //parser.nextTag();
                    desc.setFileContainer(new FileDescription.Offer(((StreamInitiation)fileExtensionProvider.parseIQ(parser)).getFile()));
                } else if (name.equals(FileDescription.Request.NODENAME)) {
                    //parser.nextTag();
                    desc.setFileContainer(new FileDescription.Request(((StreamInitiation)fileExtensionProvider.parseIQ(parser)).getFile()));
                } else {
                    throw new Exception("Unknow element \"" + name + "\" in content.");
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (name.equals(Description.DESCRIPTION)) {
                    done = true;
                }
            }
        }
        return desc;
    }
}
