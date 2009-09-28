package org.limewire.libtorrent;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LibTorrentInfo extends Structure {
    public String sha1;

    public int piece_length;

    public Pointer trackers;

    public int num_trackers;

    public Pointer seeds;

    public int num_seeds;

    public String created_by;

    public String comment;

    private List<LibTorrentAnnounceEntry> trackers_internal = new ArrayList<LibTorrentAnnounceEntry>();

    private List<LibTorrentAnnounceEntry> seeds_internal = new ArrayList<LibTorrentAnnounceEntry>();

    @Override
    public void read() {
        super.read();
        LibTorrentAnnounceEntry tracker = new LibTorrentAnnounceEntry(trackers);
        if (num_trackers > 0) {
            LibTorrentAnnounceEntry[] trackers = (LibTorrentAnnounceEntry[]) tracker
                    .toArray(num_trackers);
            for (int i = 0; i < trackers.length; i++) {
                trackers[i].read();
                trackers_internal.add(trackers[i]);
            }
        }

        LibTorrentAnnounceEntry seed = new LibTorrentAnnounceEntry(seeds);
        if (num_seeds > 0) {
            LibTorrentAnnounceEntry[] seeds = (LibTorrentAnnounceEntry[]) seed.toArray(num_seeds);
            for (int i = 0; i < seeds.length; i++) {
                seeds[i].read();
                seeds_internal.add(seeds[i]);
            }
        }
    }
}
