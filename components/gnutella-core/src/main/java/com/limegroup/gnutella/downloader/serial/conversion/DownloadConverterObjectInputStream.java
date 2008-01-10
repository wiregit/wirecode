package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.InputStream;

import org.limewire.util.ConverterObjectInputStream;

class DownloadConverterObjectInputStream extends ConverterObjectInputStream {
    
    DownloadConverterObjectInputStream(InputStream in) throws IOException {
        super(in);
        
        addLookup("com.limegroup.gnutella.downloader.AbstractDownloader", SerialRoot.class.getName());
        addLookup("com.limegroup.gnutella.downloader.ManagedDownloader", SerialManagedDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.StoreDownloader", SerialStoreDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.ResumeDownloader", SerialResumeDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.RequeryDownloader", SerialRequeryDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.MagnetDownloader", SerialMagnetDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.InNetworkDownloader", SerialInNetworkDownloader.class.getName());
        addLookup("com.limegroup.bittorrent.BTDownloader", SerialBTDownloader.class.getName());
        addLookup("com.limegroup.gnutella.downloader.IncompleteFileManager", SerialIncompleteFileManager.class.getName());
        addLookup("com.limegroup.gnutella.RemoteFileDesc", SerialRemoteFileDesc.class.getName());
        addLookup("com.limegroup.gnutella.xml.LimeXMLDocument", SerialXml.class.getName());
    }
    

}
