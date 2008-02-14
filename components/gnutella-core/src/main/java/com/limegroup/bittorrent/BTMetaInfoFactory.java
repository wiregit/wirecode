package com.limegroup.bittorrent;

import java.io.IOException;

import org.limewire.io.InvalidDataException;

import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public interface BTMetaInfoFactory {

    BTMetaInfo create(BTMetaInfoMemento memento) throws InvalidDataException;
    
    /**
     * Reads a BTMetaInfo from byte []
     * 
     * @param torrent byte array with the contents of .torrent
     * @return new instance of BTMetaInfo if all went well
     * @throws IOException if parsing or reading failed.
     */
    BTMetaInfo createBTMetaInfoFromBytes(byte []torrent) throws IOException;
}