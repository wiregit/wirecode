package org.limewire.ui.swing.player;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.media.CannotRealizeException;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StopEvent;
import javax.media.Time;
import javax.swing.Timer;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class VideoPlayerMediator implements PlayerMediator {
    
    private Player player;
    private File currentVideo;
    private final VideoDisplayDirector displayDirector;
    private final List<PlayerMediatorListener> listenerList;
    private volatile Timer updateTimer;
    private final CategoryManager categoryManager;
    
    @Inject
    VideoPlayerMediator(VideoDisplayDirector displayDirector,
            CategoryManager categoryManager){
        this.displayDirector = displayDirector;
        this.categoryManager = categoryManager;
        this.listenerList = new ArrayList<PlayerMediatorListener>();
    }

    @Override
    public void addMediatorListener(PlayerMediatorListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void removeMediatorListener(PlayerMediatorListener listener) {
        listenerList.remove(listener);
    }

    @Override
    public File getCurrentMediaFile() {
        return currentVideo;
    }

    @Override
    public PlayerState getStatus() {
        if (player == null){
            return PlayerState.UNKNOWN;
        }
        return convertControllerState(player.getState());
    }

    private PlayerState convertControllerState(int controllerState) {
        // TODO: there a lot of states missing but this is enough to get it
        // working
        switch (controllerState) {
        case Controller.Started:
            return PlayerState.PLAYING;
        case Controller.Realized:
            return PlayerState.PAUSED;
        default:
            return PlayerState.UNKNOWN;
        }
    }

    @Override
    public boolean isActivePlaylist(LibraryNavItem navItem) {
        throw new UnsupportedOperationException(getPlaylistsNotSupportedMessage());
    }

    @Override
    public boolean isPaused(File file) {
        // TODO: this isn't 100% correct but it's close enough to start
        return file.equals(currentVideo) && player != null && player.getState() != Controller.Started;
    }

    @Override
    public boolean isPlaying(File file) {
        return player.getState() == Controller.Started && file.equals(currentVideo)
                && player != null;
    }

    @Override
    public boolean isPlaylistSupported() {
        return false;
    }

    @Override
    public boolean isSeekable() {
        return isDurationMeasurable();
    }

    @Override
    public boolean isShuffle() {
        return false;
    }

    @Override
    public void nextSong() {
        throw new UnsupportedOperationException(getPlaylistsNotSupportedMessage());
    }

    @Override
    public void pause() {
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public void play(File file) {
        if(initializePlayerOrNativeLaunch(file, null, true)){
            displayDirector.show(player.getVisualComponent(), false);
        }
    }
    
  
     /**
     * Initializes an FMJ player for the video if possible, launches natively if
     * not.
     * 
     * @param file the video file to be played
     * @param time the starting time of the video. null to start at the
     *        beginning.
     * @param autoPlay whether or not to start playback
     * @return true if the player is successfully initialized, false if it is
     *         not initialized and the file is natively launched
     */
    private boolean initializePlayerOrNativeLaunch(File file, Time time, boolean autoPlay){
        GuiUtils.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        currentVideo = file;

        try {
            player = Manager.createRealizedPlayer(file.toURI().toURL());
        } catch (CannotRealizeException e) {
            nativeLaunch(file);
            return false;
        } catch (NoPlayerException e) {
           nativeLaunch(file);
           return false;
        } catch (MalformedURLException e) {
           nativeLaunch(file);
           return false;
        } catch (IOException e) {
            //TODO: how should this be handled?
            nativeLaunch(file);
            return false;
        }
        
        
        
        if(time != null){
            player.setMediaTime(time);
        }
        player.start();
        
        player.addControllerListener(new VideoControllerListener());        
        updateTimer = new Timer(1000, new TimerAction());
        updateTimer.start();
        
        fireSongChanged(file.getName());   

        if (!autoPlay) {
            //start and stop to get initial frame on screen
            pause();
        }
        GuiUtils.getMainFrame().setCursor(Cursor.getDefaultCursor());
        
        return true;    
    }
 
    
    private void nativeLaunch(File file){
        NativeLaunchUtils.safeLaunchFile(file, categoryManager);        
    }

    @Override
    public void play(LocalFileItem localFileItem) {
        play(localFileItem.getFile());
    }

    @Override
    public void prevSong() {
        throw new UnsupportedOperationException(getPlaylistsNotSupportedMessage());
    }


    @Override
    public void resume() {
        player.start();

    }

    @Override
    public void setActivePlaylist(LibraryNavItem navItem) {
        throw new UnsupportedOperationException(getPlaylistsNotSupportedMessage());
    }

    @Override
    public void setPlaylist(EventList<LocalFileItem> fileList) {
        throw new UnsupportedOperationException(getPlaylistsNotSupportedMessage());
    }

    @Override
    public void setShuffle(boolean shuffle) {
        throw new UnsupportedOperationException(getPlaylistsNotSupportedMessage());
    }

    @Override
    public void setVolume(double value) {
        if (player.getGainControl() != null) {
            player.getGainControl().setLevel((float) value);
        }
    }
    
    @Override
    public boolean isVolumeSettable(){
        return player.getGainControl() != null;
    }

    @Override
    public void skip(double percent) {
        if(!isDurationMeasurable()){
            throw new IllegalStateException("Can not skip when duration is unmeasurable");
        }
        
        player.setMediaTime(new Time(percent * player.getDuration().getSeconds()));
    }

    @Override
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }
    
    private String getPlaylistsNotSupportedMessage(){
        return I18n.tr("Playlists not supported in video");
    }
    
    private void firePlayerStateChanged(PlayerState state) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.stateChanged(state);
        }
    }
    
    private void fireProgressUpdated(float progress) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.progressUpdated(progress);
        }
    }
    
    private void fireSongChanged(String name) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.songChanged(name);
        }
    }

    public void closeVideoPanel() {
        killTimer();
        killPlayer();        

        currentVideo = null;

        displayDirector.close();
    }
    
    private void killTimer(){
        if(updateTimer != null && updateTimer.isRunning()){
            updateTimer.stop();
            updateTimer = null;
        }
    }
    
    private void killPlayer() {
        player.close();
        player.deallocate();
        player = null;
    }

    public void setFullScreen(boolean isFullScreen) {

        if (displayDirector.isFullScreen() == isFullScreen) {
            return;
        }
        
        reInitializePlayer();
        
        displayDirector.show(player.getVisualComponent(), isFullScreen);

    }
    
    private void reInitializePlayer(){

        if (player == null) {
            throw new IllegalStateException("Video player not initialized");
        }

        killTimer();

        Time time = isDurationMeasurable() ? player.getMediaTime() : null;

        boolean isPlaying = player.getState() == Controller.Started;

        killPlayer();

        boolean playerInitialized = initializePlayerOrNativeLaunch(currentVideo, time, isPlaying);

        if (!playerInitialized) {
            // TODO: how should we handle this?
            throw new IllegalStateException("Video player initialization failed");
        }         
            
    }
    
    public boolean isFullScreen(){
        return displayDirector.isFullScreen();
    }

    
    private boolean isDurationMeasurable(){
        return player != null && player.getDuration() != Player.DURATION_UNBOUNDED
        && player.getDuration() != Player.DURATION_UNKNOWN;
    }
    


    private class VideoControllerListener implements ControllerListener {

        @Override
        public void controllerUpdate(final ControllerEvent controllerEvent) {
            SwingUtils.invokeLater(new Runnable() {

                @Override
                public void run() {

                    if (controllerEvent.getSourceController().getState() == Controller.Started) {
                        firePlayerStateChanged(PlayerState.PLAYING);
                        if (updateTimer == null) {
                            updateTimer = new Timer(500, new TimerAction());
                        }
                        
                        if (!updateTimer.isRunning()) {
                            updateTimer.start();
                        }

                    } else if (controllerEvent instanceof StopEvent){
                        firePlayerStateChanged(PlayerState.STOPPED);
                        if (updateTimer != null) {
                            updateTimer.stop();
                        }
                    }
                }
            });
        }
    }
    
    private class TimerAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!isDurationMeasurable()){
                return;
            }
            
            if (player.getMediaTime().getSeconds() >= player.getDuration().getSeconds()) {
                //FMJ doesn't seem to fire EndOfMediaEvents so we need to do this manually
                player.stop();
                updateTimer.stop();
                player.setMediaTime(new Time(0));
                fireProgressUpdated(0);
                firePlayerStateChanged(PlayerState.EOM);
            } else {
                fireProgressUpdated((float) (player.getMediaTime().getSeconds() / player.getDuration().getSeconds()));
            }
        }

    }
  
}
