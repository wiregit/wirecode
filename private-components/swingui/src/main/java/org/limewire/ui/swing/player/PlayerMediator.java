package org.limewire.ui.swing.player;

import java.io.File;

import org.limewire.core.api.library.LocalFileItem;

public interface PlayerMediator {

    /**
     * Starts playing the specified file item in the media player.
     */
    public void play(LocalFileItem localFileItem);
    
    /**
     * Starts playing the specified file within LW if it can, otherwise it
     * launches natively on failure.
     */
    public void playOrLaunchNatively(File file);
}