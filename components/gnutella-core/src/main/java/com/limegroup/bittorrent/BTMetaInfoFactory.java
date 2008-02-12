package com.limegroup.bittorrent;

import java.io.IOException;

import org.limewire.io.InvalidDataException;

import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public interface BTMetaInfoFactory {

    BTMetaInfo create(BTMetaInfoMemento memento) throws InvalidDataException;
    
    BTMetaInfo createBTMetaInfoFromBytes(byte []torrent) throws IOException;
}