package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * A remote file descriptor augmented with information about past connection
 * attempts, e.g., whether it was reachable.
 */
public class RemoteFileDesc2 extends RemoteFileDesc {
    /** Set to false if a connection failed. */
    private boolean _isUnreachable;
    /** The time this was created, used for push authentication purposes. */
    private long _timeStamp;
    
    /** Pseudo copy constructor.  Initializes this to be exactly like rfd,
     *  but with the isUnreachable field set to false and the time field
     *  set to the current time. */
    public RemoteFileDesc2(RemoteFileDesc rfd, boolean isUnreachable) {
        super(rfd.getHost(),
              rfd.getPort(),
              rfd.getIndex(),
              rfd.getFileName(),
              rfd.getSize(),
              rfd.getClientGUID(),
              rfd.getSpeed(),
              rfd.chatEnabled());
        this._isUnreachable=isUnreachable;
        this._timeStamp=System.currentTimeMillis();
    }

    /** Returns true if this unreachable.  A return value of false
     *  does not necessarily mean this is reachable. */
    public boolean isUnreachable() {
        return _isUnreachable;
    }

    /** Returns the system time that this was created. */
    public long getCreationTime() {
        return _timeStamp;
    }
}
