package org.limewire.core.impl.playlist;

import java.io.File;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistListener;

import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Mock implementation of Playlist.
 */
class MockPlaylist implements Playlist {

    @Override
    public void addPlaylistListener(PlaylistListener listener) {
    }

    @Override
    public void removePlaylistListener(PlaylistListener listener) {
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void addFile(File file) {
    }

    @Override
    public void addFile(int index, File file) {
    }

    @Override
    public void removeFile(File file) {
    }

    @Override
    public void reorderFiles(File[] files) {
    }

    @Override
    public void clear() {
    }
    
    @Override
    public boolean canAdd(File file) {
        return false;
    }

    @Override
    public boolean contains(File file) {
        return false;
    }

    @Override
    public int getIndex(File file) {
        return 0;
    }

    @Override
    public MatcherEditor<LocalFileItem> getFilter() {
        return null;
    }
}
