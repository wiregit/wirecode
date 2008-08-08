package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.limewire.collection.BitField;
import org.limewire.collection.NECallable;
import org.limewire.http.entity.Piece;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmWriteJobControl;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.PieceReadListener;
import com.limegroup.bittorrent.disk.DiskManagerListener;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.bittorrent.handshaking.piecestrategy.PieceStrategy;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.util.LimeTestCase;

public class BTSwarmWriteJobTest extends LimeTestCase {

    public BTSwarmWriteJobTest(String name) {
        super(name);
    }

    public void testWrite() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        BTInterval piece1 = new BTInterval(0, 8, 0);
        pieces.add(piece1);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);
        final NECallable<BTPiece> factory = null;
        mockery.checking(new Expectations() {
            {
                allowing(tdm).writeBlock(factory);
                allowing(callback).resume();
                allowing(callback).pause();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(10);

        SwarmContent content = new SwarmContent() {

            public int read(ByteBuffer byteBuffer) throws IOException {
                int oldRemaining = byteBuffer.remaining();
                if (contentBuffer.remaining() == 0) {
                    return 0;
                }
                int oldLimit = contentBuffer.limit();
                int remainingOffset = contentBuffer.remaining() - byteBuffer.remaining();
                int newLimit = oldLimit - remainingOffset;
                if (newLimit < oldLimit) {
                    contentBuffer.limit(newLimit);
                }
                byteBuffer.put(contentBuffer);
                contentBuffer.limit(oldLimit);
                int read = oldRemaining - byteBuffer.remaining();
                return read;
            }

        };
        swarmWriteJob.write(content);
    }
}
