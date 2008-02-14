package com.limegroup.bittorrent;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.InvalidDataException;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public class BTMetaInfoFactoryImpl implements BTMetaInfoFactory {
        
    private static final Log LOG = LogFactory.getLog(BTMetaInfoFactory.class);
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfoFactory#create(com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento)
     */
    public BTMetaInfo create (BTMetaInfoMemento memento) throws InvalidDataException{
        return new BTMetaInfoImpl(memento);
    }
    
    public BTMetaInfo create(BTData data) throws IOException {
        return new BTMetaInfoImpl(data);
    }
    

    public BTMetaInfo createBTMetaInfoFromBytes(byte []torrent) throws IOException {
        try {
            Object metaInfo = Token.parse(torrent);
            if(!(metaInfo instanceof Map))
                throw new ValueException("metaInfo not a Map!");
            return new BTMetaInfoImpl(new BTDataImpl((Map)metaInfo));
        } catch (IOException bad) {
            LOG.error("read failed", bad);
            throw bad;
        }
    }    
}
