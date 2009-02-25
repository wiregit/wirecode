package org.limewire.core.impl.playlist;

import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Mock implementation of PlaylistManager. 
 */
@Singleton
class MockPlaylistManager implements PlaylistManager {

    private final List<Playlist> playlistList;
    
    @Inject
    public MockPlaylistManager() {
        // Create the only playlist currently supported.
        this.playlistList = Arrays.asList(new Playlist[] {
            new PlaylistImpl(I18n.tr("Quicklist"))
        });
    }

    @Override
    public List<Playlist> getPlaylists() {
        return playlistList;
    }

}
