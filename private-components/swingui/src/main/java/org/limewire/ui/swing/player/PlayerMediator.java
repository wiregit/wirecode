package org.limewire.ui.swing.player;

import java.io.File;
import java.net.URL;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;

import ca.odell.glazedlists.EventList;

public interface PlayerMediator {

    /**
     * Adds the specified listener to the list that is notified about 
     * mediator events.
     */
    public void addMediatorListener(PlayerMediatorListener listener);

    /**
     * Removes the specified listener from the list that is notified about
     * mediator events.
     */
    public void removeMediatorListener(PlayerMediatorListener listener);

    /**
     * Returns the current status of the audio player.
     */
    public PlayerState getStatus();
    
    /**
     * @return true if the mediator supports playlists
     */
    public boolean isPlaylistSupported();

    /**
     * Returns true if the specified library item is the active playlist.
     */
    public boolean isActivePlaylist(LibraryNavItem navItem);

    /**
     * Sets the active playlist using the specified library item.
     */
    public void setActivePlaylist(LibraryNavItem navItem);

    /**
     * Sets the internal playlist using the specified list of file items.  If
     * the specified list is empty or null, then the playlist is cleared.
     */
    public void setPlaylist(EventList<LocalFileItem> fileList);

    /**
     * Returns true if shuffle mode is enabled.
     */
    public boolean isShuffle();

    /**
     * Sets an indicator to enable shuffle mode.
     */
    public void setShuffle(boolean shuffle);

    /**
     * Sets the volume (gain) value on a linear scale from 0.0 to 1.0.
     */
    public void setVolume(double value);
    

    /**
     * @return true if volume can be set
     */
    boolean isVolumeSettable();

    /**
     * @return true if the name of the file playing should scroll above the control buttons
     */
    boolean hasScrollingTitle();

    /**
     * Pauses the current song in the audio player. 
     */
    public void pause();

    /**
     * Resumes playing the current song in the audio player.  If the player is
     * stopped, then attempt to play the first selected item in the library. 
     */
    public void resume();

    /**
     * Starts playing the specified file in the audio player.  The playlist
     * is automatically cleared so the player will stop when the song finishes.
     */
    public void play(File file);

    /**
     * Plays the specified URL
     * @param url; cannot be null
     */
    public void play(URL url);
    
    /**
     * Starts playing the specified file item in the audio player.
     */
    public void play(LocalFileItem localFileItem);

    /**
     * Skips the current song to a new position in the song. If the song's
     * length is unknown (streaming audio), then ignore the skip.
     * 
     * @param percent of the song frames to skip from begining of file
     */
    public void skip(double percent);

    /**
     * Stops playing the current song in the audio player.
     */
    public void stop();

    /**
     * Plays the next song in the playlist.
     */
    public void nextSong();

    /**
     * Plays the previous song in the playlist.
     */
    public void prevSong();

    /**
     * Returns the current playing file.
     */
    public File getCurrentMediaFile();

    /**
     * Returns true if this file is currently playing, false otherwise
     */
    public boolean isPlaying(File file);

    /**
     * Returns true if this file is currently loaded and paused, false otherwise.
     */
    public boolean isPaused(File file);

    /**
     * Returns true if the currently playing song is seekable.
     */
    public boolean isSeekable();


}
