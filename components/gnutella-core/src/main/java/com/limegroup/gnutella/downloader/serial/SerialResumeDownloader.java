package com.limegroup.gnutella.downloader.serial;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;

import com.limegroup.gnutella.URN;

class SerialResumeDownloader extends SerialManagedDownloader {
    private static final long serialVersionUID = -4535935715006098724L;

    private File _incompleteFile;

    private String _name;

    @SuppressWarnings("unused")
    private int _size; // this is required for deserialization to read the property.

    private long _size64;

    private URN _hash;

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        GetField gets = stream.readFields();
        _hash = (URN) gets.get("_hash", null);
        _name = (String) gets.get("_name", null);
        _incompleteFile = (File) gets.get("_incompleteFile", null);

        // try to read the long size first, if not there read the int
        _size64 = gets.get("_size64", -1L);
        if (_size64 == -1L)
            _size64 = gets.get("_size", 0);
    }
    
    File getIncompleteFile() {
        return _incompleteFile;
    }
    
    String getName() {
        return _name;
    }
    
    long getSize() {
        return _size64;
    }
    
    URN getUrn() {
        return _hash;
    }

}
