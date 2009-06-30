package org.limewire.ui.swing.player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
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
    private final Provider<LibraryPanel> libraryPanelProvider;
    private final List<PlayerMediatorListener> listenerList;
    
    /** Audio player component. */
    private AudioPlayer audioPlayer;
    
    /** Current list of songs. */
    private List<LocalFileItem> playList;
    
    /** Identifier for current playlist. */
    private PlaylistId playlistId;
    
    /** File item for the last opened song. */
    private LocalFileItem fileItem = null;
    
    /** Map containing properties for the last opened song. */
    private Map audioProperties = null;
    
    /** Progress of current song from 0.0 to 1.0. */
    private float progress;
    
    /** Indicator for shuffle mode. */
    private boolean shuffle = false;
    
    /** Randomized list of songs to be played. */
    private List<LocalFileItem> shuffleList;
    
    /**
     * Constructs a PlayerMediator using the specified services.
     */
    @Inject
    public PlayerMediator(Provider<AudioPlayer> audioPlayerProvider,
            Provider<LibraryPanel> libraryPanelProvider) {
        this.audioPlayerProvider = audioPlayerProvider;
        this.libraryPanelProvider = libraryPanelProvider;
        this.listenerList = new ArrayList<PlayerMediatorListener>();
    }
    
    /**
     * Returns the audio player component.  When first called, this method
     * creates the component and registers this mediator as a listener.
     */
    private AudioPlayer getPlayer() {
        if (audioPlayer == null) {
            audioPlayer = audioPlayerProvider.get();
            audioPlayer.addAudioPlayerListener(new PlayerListener());
        }
        return audioPlayer;
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
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).progressUpdated(progress);
        }
    }
    
    /**
     * Notifies registered listeners that the song is changed to the specified
     * song name.
     */
    private void fireSongChanged(String name) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).songChanged(name);
        }
    }
    
    /**
     * Notifies registered listeners that the player state is changed to the
     * specified state.
     */
    private void fireStateChanged(PlayerState state) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).stateChanged(state);
        }
    }
    
    /**
     * Returns the current status of the audio player.
     */
    public PlayerState getStatus() {
        return getPlayer().getStatus();
    }
    
    /**
     * Returns true if the specified library item is the active playlist.
     */
    public boolean isActivePlaylist(LibraryNavItem navItem) {
        if (navItem != null) {
            PlaylistId newId = new PlaylistId(navItem);
            return newId.equals(playlistId);
        }
        return false;
    }
    
    /**
     * Sets the active playlist using the specified library item.
     */
    public void setActivePlaylist(LibraryNavItem navItem) {
        if (navItem != null) {
            playlistId = new PlaylistId(navItem);
        } else {
            playlistId = null;
            if (playList != null) playList.clear();
        }
    }
    
    /**
     * Returns the current playlist.
     */
    private List<LocalFileItem> getPlaylist() {
        // Compare selected list to playlist.
        PlaylistId selectedId = new PlaylistId(libraryPanelProvider.get().getSelectedNavItem());
        Category selectedCategory = libraryPanelProvider.get().getSelectedCategory();
        
        if (selectedId.equals(playlistId) && 
                libraryPanelProvider.get().isPlayable(selectedCategory)) {
            // Selected list is same so return list from library.
            return libraryPanelProvider.get().getPlayableList();
        } else {
            // Selected list is different so return internal playlist.
            return playList;
        }
    }
    
    /**
     * Sets the internal playlist using the specified list of file items.
     */
    public void setPlaylist(EventList<LocalFileItem> fileList) {
        // Clear current playlist.
        if (playList == null) {
            playList = new ArrayList<LocalFileItem>(); 
        } else {
            playList.clear();
        }
        
        if (fileList == null) return;
        
        // Copy files into playlist.
        for (int i = 0, size = fileList.size(); i < size; i++) {
            playList.add(fileList.get(i));
        }
    }
    
    /**
     * Returns true if shuffle mode is enabled.
     */
    public boolean isShuffle() {
        return shuffle;
    }
    
    /**
     * Sets an indicator to enable shuffle mode.
     */
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        
        // Update shuffle list.
        if (shuffle) {
            updateShuffleList();
        } else {
            clearShuffleList();
        }
    }
    
    /**
     * Sets the volume (gain) value on a linear scale from 0.0 to 1.0.
     */
    public void setVolume(double value) {
        getPlayer().setVolume(value);
    }
    
    /**
     * Pauses the current song in the audio player. 
     */
    public void pause() {
        getPlayer().pause();
    }
    
    /**
     * Resumes playing the current song in the audio player. 
     */
    public void resume() {
        getPlayer().unpause();
    }
    
    /**
     * Starts playing the specified file item in the audio player.
     */
    public void play(LocalFileItem localFileItem) {
        // Stop current song.
        getPlayer().stop();
        
        // Play new song.
        this.fileItem = localFileItem;
        getPlayer().loadSong(localFileItem.getFile());
        getPlayer().playSong();
        
        // Update shuffle list when enabled.
        if (shuffle) {
            updateShuffleList();
        }
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
                getPlayer().seekLocation(skipBytes);
            }
        }
    }
    
    /**
     * Stops playing the current song in the audio player.
     */
    public void stop() {
        getPlayer().stop();
    }
    
    /**
     * Plays the next song in the playlist.
     */
    public void nextSong() {
        if (fileItem != null) {
            // Stop current song.
            getPlayer().stop();
            
            // Get next file item.
            fileItem = getNextFileItem();
            
            // Play song.
            if (fileItem != null) {
                getPlayer().loadSong(fileItem.getFile());
                getPlayer().playSong();
            }
        }
    }
    
    /**
     * Plays the previous song in the playlist.
     */
    public void prevSong() {
        if (fileItem != null) {
            // Stop current song.
            getPlayer().stop();
            
            // If near beginning of current song, then get previous song.
            // Otherwise, restart current song.
            if (progress < 0.1f) {
                fileItem = getPrevFileItem();
            }
            
            // Play song.
            if (fileItem != null) {
                getPlayer().loadSong(fileItem.getFile());
                getPlayer().playSong();
            }
        }
    }
    
    /**
     * Returns true if this file is currently playing, false otherwise
     */
    public boolean isPlaying(File file) {
        return getPlayer().isPlaying(file);
    }
    
    /**
     * Returns true if the currently playing song is seekable.
     */
    public boolean isSeekable() {
        if (audioProperties != null) {
            return isSeekable((String) audioProperties.get(AUDIO_TYPE));
        }
        return false;
    }
    
    /**
     * Returns true if the specified song type is seekable, which means that 
     * the progress position can be set.  At present, only MP3 and Wave files 
     * are seekable.
     */
    private boolean isSeekable(String songType) {
        if (songType == null) {
            return false;
        }
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }
    
    /**
     * Clears the shuffle list of items to be played.
     */
    private void clearShuffleList() {
        if (shuffleList != null) {
            shuffleList.clear();
        }
    }
    
    /**
     * Updates the shuffle list of items to be played.
     */
    private void updateShuffleList() {
        // Create shuffle list.
        shuffleList = new ArrayList<LocalFileItem>();
        
        // Get current playlist.
        List<LocalFileItem> playlist = getPlaylist();
        if ((playlist == null) || (playlist.size() == 0)) return;
        
        // Set shuffle list elements and randomize.
        for (int i = 0; i < playlist.size(); i++) {
            shuffleList.add(playlist.get(i));
        }
        Collections.shuffle(shuffleList);
        
        // Move currently playing song to the beginning.
        int index = shuffleList.indexOf(fileItem);
        if (index >= 0) {
            shuffleList.remove(index);
            shuffleList.add(0, fileItem);
        }
    }
    
    /**
     * Returns the next file item in the specified file list.
     */
    private LocalFileItem getNextFileItem() {
        // Get file list.
        List<LocalFileItem> fileList = shuffle ? shuffleList : getPlaylist();
        
        if ((fileItem != null) && (fileList != null)) {
            int index = fileList.indexOf(fileItem);
            if (index < (fileList.size() - 1)) {
                return fileList.get(index + 1);
            }
        }
        return null;
    }
    
    /**
     * Returns the previous file item in the specified file list.
     */
    private LocalFileItem getPrevFileItem() {
        // Get file list.
        List<LocalFileItem> fileList = shuffle ? shuffleList : getPlaylist();
        
        if ((fileItem != null) && (fileList != null)) {
            int index = fileList.indexOf(fileItem);
            if (index > 0) {
                return fileList.get(index - 1);
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
    
    /**
     * An identifier for a playlist.
     */
    private static class PlaylistId {
        private final NavType type;
        private final int id;
        
        public PlaylistId(LibraryNavItem navItem) {
            this.type = navItem.getType();
            this.id = navItem.getId();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PlaylistId) {
                return (type == ((PlaylistId) obj).type) && (id == ((PlaylistId) obj).id);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + type.hashCode();
            result = 31 * result + id;
            return result;
        }
    }
}
