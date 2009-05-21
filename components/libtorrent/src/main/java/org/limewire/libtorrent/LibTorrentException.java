package org.limewire.libtorrent;

/**
 * LibTorrentException is used to wrap a caught exception from the native code.
 */
public class LibTorrentException extends RuntimeException {
    private final int type;

    public static final int LOAD_EXCEPTION = -100000;

    public LibTorrentException(String message, int type) {
        super(message);
        this.type = type;
    }

    public LibTorrentException(WrapperStatus status) {
        super(status.message);
        this.type = status.type;
    }

    public int getType() {
        return type;
    }
}
