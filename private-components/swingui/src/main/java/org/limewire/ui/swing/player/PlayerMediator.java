package org.limewire.ui.swing.player;

import java.util.Map;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Mediator to control the interaction between the audio player and the 
 * current playlist.
 */
public class PlayerMediator {

    public static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";
    public static final String AUDIO_TYPE = "audio.type";
    
    private static final String MP3 = "mp3";
    private static final String WAVE = "wave";
    
    private final Provider<AudioPlayer> audioPlayerProvider;
    
    /** Audio player component. */
    private AudioPlayer audioPlayer;
    /** Display view for the player. */
    private PlayerView playerView;
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
    }
    
    /**
     * Returns the current status of the audio player.
     */
    public PlayerState getStatus() {
        return getAudioPlayer().getStatus();
    }
    
    /**
     * Sets the display view for the player.
     */
    public void setPlayerView(PlayerView playerView) {
        this.playerView = playerView;
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
                playerView.updateProgress(progress);
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            // Save properties.
            audioProperties = properties;
            
            // Notify UI about new song.
            playerView.updateSong(getSongName());
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            // Go to next song when finished.
            if (event.getState() == PlayerState.EOM) {
                nextSong();
            }
            
            // Notify UI about state change.
            playerView.updateState(event.getState());
        }
    }
}
