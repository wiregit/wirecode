package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.audiortp.AudioRTPDescription;
import org.jivesoftware.smackx.packet.Description;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.jivesoftware.smackx.provider.audiortp.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.provider.file.FileDescriptionProvider;
import org.xmlpull.v1.XmlPullParser;

public class DescriptionProvider implements PacketExtensionProvider {

    public Description parseExtension(XmlPullParser parser) throws Exception {               
        
        JingleContentDescriptionProvider jdpAudio = new JingleContentDescriptionProvider.Audio();
        FileDescriptionProvider fileDescriptionProvider = new FileDescriptionProvider();
        
        String namespace = parser.getNamespace();
        if ( namespace.equals(AudioRTPDescription.NAMESPACE)) {
            return jdpAudio.parseExtension(parser);
        } else if ( namespace.equals(FileDescription.NAMESPACE)) {
            return fileDescriptionProvider.parseExtension(parser);
        } else {
            throw new XMPPException("Unknown transport namespace \""
                    + namespace + "\" in Jingle packet.");
        }
    }
}
