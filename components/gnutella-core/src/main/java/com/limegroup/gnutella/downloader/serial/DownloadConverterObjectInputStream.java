package com.limegroup.gnutella.downloader.serial;

import java.io.IOException;
import java.io.InputStream;

import org.limewire.util.ConverterObjectInputStream;

class DownloadConverterObjectInputStream extends ConverterObjectInputStream {
    
    DownloadConverterObjectInputStream(InputStream in) throws IOException {
        super(in);
        
        addLookup("com.limegroup.gnutella.downloader.AbstractDownloader", "com.limegroup.gnutella.downloader.serial.SerialRoot");
        addLookup("com.limegroup.gnutella.downloader.ManagedDownloader", "com.limegroup.gnutella.downloader.serial.SerialManagedDownloader");
        addLookup("com.limegroup.gnutella.downloader.StoreDownloader", "com.limegroup.gnutella.downloader.serial.SerialStoreDownloader");
        addLookup("com.limegroup.gnutella.downloader.ResumeDownloader", "com.limegroup.gnutella.downloader.serial.SerialResumeDownloader");
        addLookup("com.limegroup.gnutella.downloader.RequeryDownloader", "com.limegroup.gnutella.downloader.serial.SerialRequeryDownloader");
        addLookup("com.limegroup.gnutella.downloader.MagnetDownloader", "com.limegroup.gnutella.downloader.serial.SerialMagnetDownloader");
        addLookup("com.limegroup.gnutella.downloader.InNetworkDownloader", "com.limegroup.gnutella.downloader.serial.SerialInNetworkDownloader");
        addLookup("com.limegroup.bittorrent.BTDownloader", "com.limegroup.gnutella.downloader.serial.SerialBTDownloader");
        addLookup("com.limegroup.gnutella.downloader.IncompleteFileManager", "com.limegroup.gnutella.downloader.serial.SerialIncompleteFileManager");
        addLookup("com.limegroup.gnutella.RemoteFileDesc", "com.limegroup.gnutella.downloader.serial.SerialRemoteFileDesc");
    }
    

}
