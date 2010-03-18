package org.limewire.core.impl;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

class TorrentFactoryImpl implements TorrentFactory {

    private final TorrentManager torrentManager;
    
    @Inject
    public TorrentFactoryImpl(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }
    
    @Override
    public Torrent createTorrentFromXML(LimeXMLDocument xmlDocument) {
        // if this isn't a torrent xml file then return null
        if(xmlDocument == null || !xmlDocument.getSchemaURI().equals(LimeXMLNames.TORRENT_SCHEMA)) {
            return null;
        }

        String hash = xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH);
        if(!StringUtils.isEmpty(hash)) {
            byte[] bytes = Base32.decode(hash);
            String sha1 = StringUtils.toHexString(bytes);
            // try getting a real Torrent based on the sha1 in the xml
            Torrent torrent = torrentManager.getTorrent(sha1);
            if(torrent != null) {
                return torrent;
            }
        }
        
        // if there are torrent files listed, return an XMLTorrent
        if(xmlDocument.getValue(LimeXMLNames.TORRENT_FILE_PATHS) != null)
            return new XMLTorrent(xmlDocument); 
        else
            return null;
    }
}
