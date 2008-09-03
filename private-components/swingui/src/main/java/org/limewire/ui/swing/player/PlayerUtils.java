package org.limewire.ui.swing.player;

import java.io.File;

import org.limewire.player.api.AudioPlayer;

import com.google.inject.Inject;
//TODO:  this is not the best way to handle player access but it gets it working for now.
public class PlayerUtils {
    @Inject
    private static AudioPlayer player;

    public static void play(File audioFile){
        player.loadSong(audioFile);
        player.playSong();
    }
}
