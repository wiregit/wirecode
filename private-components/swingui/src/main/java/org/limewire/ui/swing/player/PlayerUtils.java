package org.limewire.ui.swing.player;

import java.io.File;
import java.util.Locale;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Utility methods to access the audio player.  PlayerUtils accesses the player
 * using an instance of PlayerMediator.  The UI may also work with the 
 * PlayerMediator directly, which maintains a playlist and various play modes
 * like shuffle.
 */
public class PlayerUtils {

    // TODO: This is terrible.  Never use static injection!
    @Inject private static @Audio Provider<PlayerMediator> audioPlayerProvider;
    // TODO: This is terrible.  Never use static injection!
    @Inject private static @Video Provider<PlayerMediator> videoPlayerProvider;

    private static void playAudio(File mediaFile) {
        audioPlayerProvider.get().play(mediaFile);
    }
    
    private static void playVideo(File mediaFile) {
        videoPlayerProvider.get().play(mediaFile);
    }
    
    public static boolean isPlaying(File mediaFile){
        return audioPlayerProvider.get().isPlaying(mediaFile) || videoPlayerProvider.get().isPlaying(mediaFile);
    }
    
    /**
     * @return true if the file is video.  It is possible that playback may fail anyway.
     */
    public static boolean isPlayableVideoFile(File file, CategoryManager categoryManager) {
        return (OSUtils.isWindows() || OSUtils.isMacOSX()) && categoryManager.getCategoryForFile(file) == Category.VIDEO;
    }
    
    public static boolean isFileAllowedInPlaylist(File file) {
        return isPlayableAudioFile(file);
    }
    
    public static boolean isPlayableAudioFile(File file){
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
    }

    public static void pause() {
        audioPlayerProvider.get().pause();
        videoPlayerProvider.get().pause();
    }
    
    public static void stop() {
        audioPlayerProvider.get().stop();
        videoPlayerProvider.get().stop();
    }
    
    /**Plays file internally if playable.  Launches native player otherwise.
     * 
     * @return true if file is played internally, false if played in native player
     */
    public static boolean playOrLaunch(File file, CategoryManager categoryManager) {
        if (SwingUiSettings.PLAYER_ENABLED.getValue() && isPlayableAudioFile(file)) {
            playAudio(file);
            return true;
        } if (SwingUiSettings.PLAYER_ENABLED.getValue() && isPlayableVideoFile(file, categoryManager)) {
            playVideo(file);
            return true;
        } 
  
        NativeLaunchUtils.safeLaunchFile(file, categoryManager);
        return false;
    }
    
    /**
     * @return The current file playing in the player. This may return null if
     *         nothing is playing or if the audio source is not a file.
     */
    public static File getCurrentSongFile(){
        return audioPlayerProvider.get().getCurrentMediaFile();
    }
}
