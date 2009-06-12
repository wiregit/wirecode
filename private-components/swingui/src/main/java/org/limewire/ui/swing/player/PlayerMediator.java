package org.limewire.ui.swing.player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Mediator that controls the interaction between the player view, the current
 * playlist, and the audio player.
 */
@LazySingleton
public class PlayerMediator {

    public static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";
    public static final String AUDIO_TYPE = "audio.type";
    
    private static final String MP3 = "mp3";
    private static final String WAVE = "wave";
    
    private final Provider<AudioPlayer> audioPlayerProvider;
    private final List<PlayerMediatorListener> listenerList;
    
    /** Audio player component. */
    private AudioPlayer audioPlayer;
    /** Current list of songs. */
    private EventList<LocalFileItem> playList;
    /** File item for the last opened song. */
    private LocalFileItem fileItem = null;
    /** Map containing properties for the last opened song. */
    private Map audioProperties = null;
    /** Progress of current song from 0.0 to 1.0. */
    private float progress;
    
    /**
     * Constructs a PlayerMediator using the specified AudioPlayer provider.
     */
    @Inject
    public PlayerMediator(Provider<AudioPlayer> audioPlayerProvider) {
        this.audioPlayerProvider = audioPlayerProvider;
        this.listenerList = new ArrayList<PlayerMediatorListener>();
    }
    
    /**
     * Adds the specified listener to the list that is notified about 
     * mediator events.
     */
    public void addMediatorListener(PlayerMediatorListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified listener from the list that is notified about
     * mediator events.
     */
    public void removeMediatorListener(PlayerMediatorListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies registered listeners that the progress is updated to the 
     * specified value.
     */
    private void fireProgressUpdated(float progress) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.progressUpdated(progress);
        }
    }
    
    /**
     * Notifies registered listeners that the song is changed to the specified
     * song name.
     */
    private void fireSongChanged(String name) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.songChanged(name);
        }
    }
    
    /**
     * Notifies registered listeners that the player state is changed to the
     * specified state.
     */
    private void fireStateChanged(PlayerState state) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.stateChanged(state);
        }
    }
    
    /**
     * Returns the current status of the audio player.
     */
    public PlayerState getStatus() {
        return getAudioPlayer().getStatus();
    }
    
    /**
     * Sets the current play list.
     */
    public void setPlayList(EventList<LocalFileItem> playList) {
        this.playList = playList;
    }
    
    /**
     * Sets the volume (gain) value on a linear scale from 0.0 to 1.0.
     */
    public void setVolume(double value) {
        getAudioPlayer().setVolume(value);
    }
    
    /**
     * Pauses the audio player. 
     */
    public void pause() {
        getAudioPlayer().pause();
    }
    
    /**
     * Resumes playing the current song in the audio player. 
     */
    public void play() {
        getAudioPlayer().unpause();
    }
    
    /**
     * Skips the current song to a new position in the song. If the song's
     * length is unknown (streaming audio), then ignore the skip.
     * 
     * @param percent of the song frames to skip from begining of file
     */
    public void skip(double percent) {
        // need to know something about the audio type to be able to skip
        if (audioProperties != null && audioProperties.containsKey(AUDIO_TYPE)) {
            String songType = (String) audioProperties.get(AUDIO_TYPE);
            
            // currently, only mp3 and wav files can be seeked upon
            if (isSeekable(songType) && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                final long skipBytes = Math.round((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)* percent);
                getAudioPlayer().seekLocation(skipBytes);
            }
        }
    }
    
    /**
     * Stops playing the current song in the audio player.
     */
    public void stop() {
        getAudioPlayer().stop();
    }
    
    /**
     * Plays the next song in the playlist.
     */
    public void nextSong() {
        if (fileItem != null) {
            // Stop current song.
            getAudioPlayer().stop();
            
            // Get next song in playlist.
            fileItem = getNextFileItem();
            if (fileItem != null) {
                getAudioPlayer().loadSong(fileItem.getFile());
                getAudioPlayer().playSong();
            }
        }
    }
    
    /**
     * Plays the previous song in the playlist.
     */
    public void prevSong() {
        if (fileItem != null) {
            // Stop current song.
            getAudioPlayer().stop();
            
            if (progress >= 0.1f) {
                // Restart current song.
                getAudioPlayer().loadSong(fileItem.getFile());
                getAudioPlayer().playSong();
                
            } else {
                // Get previous song in playlist.
                fileItem = getPrevFileItem();
                if (fileItem != null) {
                    getAudioPlayer().loadSong(fileItem.getFile());
                    getAudioPlayer().playSong();
                }
            }
        }
    }
    
    /**
     * Returns true if this file is currently playing, false otherwise
     */
    public boolean isPlaying(File file) {
        //TODO: fix logic here
        return false;
    }
    
    /**
     * Returns the audio player component.
     */
    private AudioPlayer getAudioPlayer() {
        if (audioPlayer == null) {
            audioPlayer = audioPlayerProvider.get();
            audioPlayer.addAudioPlayerListener(new PlayerListener());
        }
        return audioPlayer;
    }
    
    /**
     * Returns the next file item in the playlist.
     */
    private LocalFileItem getNextFileItem() {
        if (fileItem != null) {
            int index = playList.indexOf(fileItem);
            if (index < (playList.size() - 1)) {
                return playList.get(index + 1);
            }
        }
        return null;
    }
    
    /**
     * Returns the previous file item in the playlist.
     */
    private LocalFileItem getPrevFileItem() {
        if (fileItem != null) {
            int index = playList.indexOf(fileItem);
            if (index > 0) {
                return playList.get(index - 1);
            }
        }
        return null;
    }
    
    /**
     * Returns the name of the current song.
     */
    private String getSongName() {
        // Use audio properties if available.
        if (audioProperties != null) {
            Object author = audioProperties.get("author");
            Object title = audioProperties.get("title");
            if ((author != null) && (title != null)) {
                return author + " - " + title;
            }
        }
        
        // Use file item if available.
        if (fileItem != null) {
            return fileItem.getFile().getName();
        } else {
            return I18n.tr("Unknown");
        }
    }
    
    /**
     * Returns true if the song type is seekable, which means that the progress
     * position can be set.  At present, only MP3 and Wave files are seekable.
     */
    private boolean isSeekable(String songType) {
        if (songType == null)
            return false;
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }
    
    /**
     * Listener to handle audio player events.
     */
    private class PlayerListener implements AudioPlayerListener {

        @Override
        public void progressChange(int bytesread) {
            // If we know the length of the song, update progress value.
            if ((audioProperties != null) && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                float byteslength = ((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)).floatValue();
                progress = bytesread / byteslength;
                
                // Notify UI about progress.
                fireProgressUpdated(progress);
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            // Save properties.
            audioProperties = properties;
            
            // Notify UI about new song.
            fireSongChanged(getSongName());
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            // Go to next song when finished.
            if (event.getState() == PlayerState.EOM) {
                nextSong();
            }
            
            // Notify UI about state change.
            fireStateChanged(event.getState());
        }
    }
}
