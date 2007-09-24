package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
public class LocalFileDetailsFactoryImpl implements LocalFileDetailsFactory {
    
    private final NetworkManager networkManager;
    
    @Inject
    public LocalFileDetailsFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LocalFileDetailsFactory#create(com.limegroup.gnutella.FileDesc)
     */
    public FileDetails create(final FileDesc fd) {
        return new FileDetails() {
            public String getFileName() {
                return fd.getFileName();
            }

            public long getFileSize() {
                return fd.getFileSize();
            }
            
            public InetSocketAddress getInetSocketAddress() {
                // TODO maybe cache this, even statically
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(networkManager.getAddress()), networkManager.getPort());
                } catch (UnknownHostException e) {
                }
                return null;
            }
            
            public boolean isFirewalled() {
                return !networkManager.acceptedIncomingConnection();
            }

            public URN getSHA1Urn() {
                return fd.getSHA1Urn();
            }

            public Set<URN> getUrns() {
                return fd.getUrns();
            }

            public LimeXMLDocument getXMLDocument() {
                return fd.getXMLDocument();
            }            
        };
    }

}
