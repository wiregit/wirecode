package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.ObjectInputStream.GetField;
import java.util.List;
import java.util.Map;

import org.limewire.collection.Range;
import org.limewire.io.URNImpl;


public class SerialIncompleteFileManager implements Serializable {
    private static final long serialVersionUID = -7658285233614679878L;

    private Map<File, List<Range>> blocks;

    private Map<URNImpl, File> hashes;

    Map<File, List<Range>> getBlocks() {
        return blocks;
    }

    Map<URNImpl, File> getHashes() {
        return hashes;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {}
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        GetField gets = stream.readFields();
        blocks = (Map<File, List<Range>>) gets.get("blocks", null);
        hashes = (Map<URNImpl, File>) gets.get("hashes", null);
    }

}
