package org.limewire.ui.swing.player;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.player.api.AudioPlayer;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class PlayerMediator {

    private final Provider<AudioPlayer> audioPlayerProvider;
    private AudioPlayer audioPlayer;
    private EventList<LocalFileItem> playList;
    
    @Inject
    public PlayerMediator(Provider<AudioPlayer> audioPlayerProvider) {
        this.audioPlayerProvider = audioPlayerProvider;
    }
    
    public void setPlayList(EventList<LocalFileItem> playList) {
        this.playList = playList;
    }
    
    public void pause() {
        
    }
    
    public void play() {
        
    }
    
    public void stop() {
        
    }
    
    public void nextSong() {
        
    }
    
    public void prevSong() {
        
    }
}
