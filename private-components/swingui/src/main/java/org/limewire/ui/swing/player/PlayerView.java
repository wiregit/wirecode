package org.limewire.ui.swing.player;

import org.limewire.player.api.PlayerState;

/**
 * Defines a view of the audio player.
 */
public interface PlayerView {

    /**
     * Updates the progress display using the specified value from 0.0 to 1.0.
     */
    void updateProgress(float progress);
    
    /**
     * Updates the song display using the specified text.
     */
    void updateSong(String songText);
    
    /**
     * Updates the player display using the specified state.
     */
    void updateState(PlayerState playerState);
    
}
