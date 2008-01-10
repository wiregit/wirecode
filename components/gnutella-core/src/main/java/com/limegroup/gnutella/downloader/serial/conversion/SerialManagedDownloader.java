package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.limewire.util.GenericsUtils;

class SerialManagedDownloader extends SerialRoot {
    private static final long serialVersionUID = 2772570805975885257L;

    private transient SerialRemoteFileDesc defaultRFD;

    private transient Set<SerialRemoteFileDesc> remoteFileDescs;

    private transient SerialIncompleteFileManager incompleteFileManager;

    private transient Map<String, Serializable> properties;
    
    protected SerialManagedDownloader() {
    }
    
    private void writeObject(ObjectOutputStream output) throws IOException {}

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        Object next = stream.readObject();
        if (next instanceof SerialRemoteFileDesc[]) {
            SerialRemoteFileDesc[] rfds = (SerialRemoteFileDesc[]) next;
            if (rfds.length > 0)
                defaultRFD = rfds[0];
            remoteFileDescs = new HashSet<SerialRemoteFileDesc>(Arrays.asList(rfds));
        } else if (next instanceof Set) { // new format
            remoteFileDescs = GenericsUtils.scanForSet(next, SerialRemoteFileDesc.class,
                    GenericsUtils.ScanMode.REMOVE);
            if (remoteFileDescs.size() > 0) {
                defaultRFD = remoteFileDescs.iterator().next();
            }
        }

        incompleteFileManager = (SerialIncompleteFileManager) stream.readObject();

        Object map = stream.readObject();
        if (map instanceof Map)
            properties = GenericsUtils.scanForMap(map, String.class, Serializable.class,
                    GenericsUtils.ScanMode.REMOVE);
    }
    
    SerialRemoteFileDesc getDefaultRFD() {
        return defaultRFD;
    }
    
    Set<SerialRemoteFileDesc> getRemoteFileDescs() {
        return remoteFileDescs;
    }
    
    SerialIncompleteFileManager getIncompleteFileManager() {
        return incompleteFileManager;
    }
    
    Map<String, Serializable> getProperties() {
        return properties;
    }

}
