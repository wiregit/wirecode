package com.limegroup.gnutella.downloader;

import java.io.IOException;

import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.xml.sax.SAXException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

@Singleton
class RemoteFileDescFactoryImpl implements RemoteFileDescFactory {
    
    private final LimeXMLDocumentFactory limeXMLDocumentFactory;
    private final PushEndpointFactory pushEndpointFactory;
    
    @Inject
    public RemoteFileDescFactoryImpl(LimeXMLDocumentFactory limeXMLDocumentFactory, PushEndpointFactory pushEndpointFactory) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.pushEndpointFactory = pushEndpointFactory;
    }
    
    public RemoteFileDesc createFromMemento(RemoteHostMemento remoteHostMemento) throws InvalidDataException {
        try {
            return new RemoteFileDesc(
                    remoteHostMemento.getHost(),
                    remoteHostMemento.getPort(),
                    remoteHostMemento.getIndex(),
                    remoteHostMemento.getFileName(),
                    remoteHostMemento.getSize(),
                    remoteHostMemento.getClientGuid(),
                    remoteHostMemento.getSpeed(),
                    remoteHostMemento.isChat(),
                    remoteHostMemento.getQuality(),
                    remoteHostMemento.isBrowseHost(),
                    xml(remoteHostMemento.getXml()),
                    remoteHostMemento.getUrns(),
                    remoteHostMemento.isReplyToMulticast(),
                    remoteHostMemento.isFirewalled(),
                    remoteHostMemento.getVendor(),
                    IpPort.EMPTY_SET,
                    -1L,
                    -1,
                    pe(remoteHostMemento.getPushAddr()),
                    remoteHostMemento.isTls()
                    );
        } catch (SAXException e) {
            throw new InvalidDataException(e);
        } catch (SchemaNotFoundException e) {
            throw new InvalidDataException(e);
        } catch (IOException e) {
            throw new InvalidDataException(e);
        }
    }

    private PushEndpoint pe(String pushAddr) throws IOException {
        if(pushAddr != null)
            return pushEndpointFactory.createPushEndpoint(pushAddr);
        else
            return null;
    }

    private LimeXMLDocument xml(String xml) throws SAXException, SchemaNotFoundException, IOException {
        if(xml != null)
            return limeXMLDocumentFactory.createLimeXMLDocument(xml);
        else
            return null;
    }

}
