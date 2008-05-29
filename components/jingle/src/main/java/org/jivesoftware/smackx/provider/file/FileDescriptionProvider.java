package org.jivesoftware.smackx.provider.file;

import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.jivesoftware.smackx.packet.StreamInitiation;
import org.xmlpull.v1.XmlPullParser;

public class FileDescriptionProvider implements PacketExtensionProvider {
    static ProviderManager providerManager = ProviderManager.getInstance();
    PacketExtensionProvider fileExtensionProvider;
    
    public FileDescriptionProvider() {
        fileExtensionProvider = (PacketExtensionProvider)providerManager.getExtensionProvider("file", "http://jabber.org/protocol/si/profile/file-transfer");
    }
    

    public FileDescription parseExtension(XmlPullParser parser) throws Exception {
        boolean done = false;
        FileDescription desc = new FileDescription();

        while (!done) {
            int eventType = parser.next();
            String name = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals(FileDescription.Offer.NODENAME)) {
                    parser.nextTag();
                    desc.setFileContainer(new FileDescription.Offer((StreamInitiation.File)fileExtensionProvider.parseExtension(parser)));
                } else if (name.equals(FileDescription.Request.NODENAME)) {
                    parser.nextTag();
                    desc.setFileContainer(new FileDescription.Request((StreamInitiation.File)fileExtensionProvider.parseExtension(parser)));
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
