package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A remote file descriptor augmented with information about past connection
 * attempts, e.g., whether it was reachable.  RemoteFileDesc2 extends
 * URLRemoteFileDesc because it may needs to store faked-up URLs for MAGNET
 * default locations.
 */
public class RemoteFileDesc2 extends URLRemoteFileDesc {
    /** Set to false if a connection failed. */
    private boolean _isUnreachable;
    /** The time this was created, used for push authentication purposes. */
    private long _timeStamp;
    
    /** Pseudo copy constructor.  Initializes this to be exactly like rfd,
     *  but with the isUnreachable field set to false and the time field
     *  set to the current time. */
    public RemoteFileDesc2(RemoteFileDesc rfd) {
        //Note that the runtime type of rfd may be either RemoteFileDesc or
        //URLRemoteFileDesc.  In the former case, the URL stored in this is the
        //value calculated on the fly by RemoteFileDesc.getUrl().  In the latter
        //case, it's a copy of the value stored in URLRemoteFileDesc.
        super(rfd.getHost(),
              rfd.getPort(),
              rfd.getIndex(),
              rfd.getFileName(),
              rfd.getSize(),
              rfd.getClientGUID(),
              rfd.getSpeed(),
              rfd.chatEnabled(),
              rfd.getQuality(),
              rfd.browseHostEnabled(),
			  rfd.getXMLDoc(),
			  rfd.getUrns(),
			  rfd.isReplyToMulticast(),
              rfd.getUrl());
    }
}

