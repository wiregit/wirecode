package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.http.nio.FileContentDecoder;
import org.limewire.io.IOUtils;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmDownload;

public class FileChannelSwarmFile implements SwarmDownload {

    // private static final Log LOG =
    // LogFactory.getLog(FileChannelSwarmFileWriter.class);

    private final File file;

    private RandomAccessFile raFile;

    private FileChannel fileChannel;

    private final Object LOCK = new Object();

    public FileChannelSwarmFile(File file) {
        this.file = file;
    }

    public long transferFrom(SwarmContent content, long start) throws IOException {
        synchronized (LOCK) {
            initialize();
            // if we have to grow the file, do so.
            // this is necessary because fileChannel.transferFrom doesn't
            // transfer
            // bytes if the length is smaller than the start position.
            if (fileChannel.size() < start)
                raFile.setLength(start);

            // Note: the two below calls use Integer.MAX_VALUE instead of
            // Long.MAX_VALUE
            // because Sun's FileChannelImpl is broken and casts the long to an
            // int,
            // causing problems.
            if (content instanceof FileContentDecoder) {
                return ((FileContentDecoder) content).transfer(fileChannel, start,
                        Integer.MAX_VALUE);
            } else {
                return transferTo(content, fileChannel, start);
            }
        }
    }

    public long transferTo(SwarmContent swarmContent, FileChannel fileChannel, long start)
            throws IOException {
        synchronized (LOCK) {
            initialize();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            long totalRead = 0;
            long numRead = 0;

            while ((numRead = swarmContent.read(buffer)) > 0) {
                totalRead += numRead;
                buffer.flip();
                fileChannel.write(buffer, (start + totalRead));
                buffer.clear();
            }

            return totalRead;
        }
    }

    public long transferTo(ByteBuffer buffer, long start) throws IOException {
        synchronized (LOCK) {
            initialize();
            if (fileChannel.size() <= start)
                throw new IOException("cannot read beyond size.");

            return fileChannel.read(buffer, start);
        }
    }

    public void finish() throws IOException {
        synchronized (LOCK) {
            if (raFile != null) {
                IOUtils.close(fileChannel);
                IOUtils.close(raFile);
            }
        }
    }

    public void initialize() throws IOException {
        synchronized (LOCK) {
            if (raFile == null) {
                raFile = new RandomAccessFile(file, "rw");
                fileChannel = raFile.getChannel();
            }
        }
    }

    public long transferFrom(ByteBuffer byteBuffer, long start) throws IOException {
        synchronized (LOCK) {
            initialize();
            long written = fileChannel.write(byteBuffer, start);
            return written;
        }
    }

}
