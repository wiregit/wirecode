package com.limegroup.gnutella.downloader;

import org.limewire.io.InvalidDataException;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;

public interface RemoteFileDescFactory {

    public RemoteFileDesc createFromMemento(RemoteHostMemento remoteHostMemento)
            throws InvalidDataException;

}
