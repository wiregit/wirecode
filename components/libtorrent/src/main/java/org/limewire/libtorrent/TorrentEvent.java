package org.limewire.libtorrent;

/**
 * Enum representing various events that a Torrent can send out to listeners.
 */
public enum TorrentEvent {
    STATUS_CHANGED, STOPPED, COMPLETED;
}
