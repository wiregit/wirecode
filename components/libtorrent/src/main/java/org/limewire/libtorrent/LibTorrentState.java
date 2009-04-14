package org.limewire.libtorrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum LibTorrentState {
    /**
     * These state align with the torrent_status.state_t enum. We need to
     * maintain in the indexes in order to properly map from the int jna type to
     * the enum.
     */
    queued_for_checking(0), checking_files(1), downloading_metadata(2), downloading(3), finished(4), seeding(
            5), allocating(6);

    private static final Map<Integer, LibTorrentState> map = new ConcurrentHashMap<Integer, LibTorrentState>();

    static {
        map.put(queued_for_checking.id, queued_for_checking);
        map.put(checking_files.id, checking_files);
        map.put(downloading_metadata.id, downloading_metadata);
        map.put(downloading.id, downloading);
        map.put(finished.id, finished);
        map.put(seeding.id, seeding);
        map.put(allocating.id, allocating);
    }

    private final int id;

    private LibTorrentState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LibTorrentState forId(int id) {
        return map.get(id);
    }

}
