package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.limewire.io.IOUtils;

public class FileChannelSwarmFileWriter implements SwarmFileWriter {
    
    private final File file;
    private RandomAccessFile raFile;
    private FileChannel fileChannel;
    
    public FileChannelSwarmFileWriter(File file) {
        this.file = file;
    }

    public long transferFrom(ContentDecoder decoder, long start) throws IOException {
        initialize();
        
        // if we have to grow the file, do so.
        // this is necessary because fileChannel.transferFrom doesn't transfer
        // bytes if the length is smaller than the start position.
        if(fileChannel.size() < start)
            raFile.setLength(start);
        
        // Note: the two below calls use Integer.MAX_VALUE instead of Long.MAX_VALUE
        // because Sun's FileChannelImpl is broken and casts the long to an int,
        // causing problems.        
        if(decoder instanceof FileContentDecoder) {
            return ((FileContentDecoder)decoder).transfer(fileChannel, start, Integer.MAX_VALUE);
        } else {
            return fileChannel.transferFrom(new ContentDecoderChannel(decoder), start, Integer.MAX_VALUE);
        }
    }
    
    public void finish() {
        if(raFile != null) {
            IOUtils.close(fileChannel);
            IOUtils.close(raFile);
        }
    }
    
    public void initialize() throws IOException {
        if(raFile == null) {
            raFile = new RandomAccessFile(file, "rw");
            fileChannel = raFile.getChannel();
        }  
    }
    

}
