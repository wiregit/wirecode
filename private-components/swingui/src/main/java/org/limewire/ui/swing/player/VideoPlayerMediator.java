package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.IncompatibleSourceException;
import javax.media.Player;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import net.sf.fmj.concurrent.ExecutorServiceManager;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lti.utils.OSUtils;

@Singleton
class VideoPlayerMediator implements PlayerMediator {

    private final String playlistsNotSupported = I18n.tr("Playlists not supported in video");;
    private Player player;
    private File currentVideo;
    private final VideoDisplayDirector displayDirector;
    private final List<PlayerMediatorListener> listenerList;
    private volatile Timer updateTimer;
    private final CategoryManager categoryManager;
    private boolean isSeeking;
    private final PlayerInitializer playerInitializer = new PlayerInitializer();
    
    private final ControllerListener controllerListener = new VideoControllerListener();

    @Inject
    VideoPlayerMediator(VideoDisplayDirector displayDirector, CategoryManager categoryManager) {
        this.displayDirector = displayDirector;
        this.categoryManager = categoryManager;
        this.listenerList = new ArrayList<PlayerMediatorListener>();

        ThreadPoolListeningExecutor tpe =  new ThreadPoolListeningExecutor(1, 1,
                5L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                ExecutorsHelper.daemonThreadFactory("Video ThreadPool"));
    
        ExecutorServiceManager.setExecutorService(ExecutorsHelper.unconfigurableExecutorService(tpe));//ExecutorsHelper.newFixedSizeThreadPool(1, "Video ThreadPool"));
    }

    @Inject
    void register(){
        SwingUiSettings.PLAYER_ENABLED.addSettingListener(new SettingListener(){
            @Override
            public void settingChanged(final SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        boolean enabled = SwingUiSettings.PLAYER_ENABLED.getValue();
                        if (!enabled && player != null) {
                            closeVideo();
                        }
                    }
                });
            }
        });
        
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
        if (player == null) {
            return PlayerState.UNKNOWN;
        }
        return convertControllerState(player.getState());
    }

    private PlayerState convertControllerState(int controllerState) {
        if(isSeeking){
            return PlayerState.SEEKING;
        }
        
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
        throw new UnsupportedOperationException(playlistsNotSupported);
    }

    @Override
    public boolean isPaused(File file) {
        // TODO: this isn't 100% correct but it's close enough to start
        return file.equals(currentVideo) && player != null
                && player.getState() != Controller.Started;
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
    public boolean hasScrollingTitle() {
        return false;
    }

    @Override
    public void nextSong() {
        throw new UnsupportedOperationException(playlistsNotSupported);
    }

    @Override
    public void pause() {
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public void play(File file) {
        if(file.equals(currentVideo)){
            return;
        }
        initializePlayerOrNativeLaunch(file, null, false, true);
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
    private void initializePlayerOrNativeLaunch(final File file, Time time, boolean isFullScreen, boolean autoStart) {
        GuiUtils.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        currentVideo = file;        
        
        playerInitializer.initialize(file, time, isFullScreen, autoStart);
    }


    @Override
    public void play(LocalFileItem localFileItem) {
        play(localFileItem.getFile());
    }

    @Override
    public void prevSong() {
        throw new UnsupportedOperationException(playlistsNotSupported);
    }

    @Override
    public void resume() {
        if (player != null && player.getState() != Controller.Started) {
            player.start();
        }
    }

    @Override
    public void setActivePlaylist(LibraryNavItem navItem) {
        throw new UnsupportedOperationException(playlistsNotSupported);
    }

    @Override
    public void setPlaylist(EventList<LocalFileItem> fileList) {
        throw new UnsupportedOperationException(playlistsNotSupported);
    }

    @Override
    public void setShuffle(boolean shuffle) {
        throw new UnsupportedOperationException(playlistsNotSupported);
    }

    @Override
    public void setVolume(double value) {
        if (player.getGainControl() != null) {
            player.getGainControl().setLevel((float) value);
        }
    }

    @Override
    public boolean isVolumeSettable() {
        // On Mac OS X we're using true full screen mode, and you can't use popups in true full screen mode.
        // So, since the volume popup won't work, let's disable it.
        if (OSUtils.isMacOSX() && isFullScreen())
            return false;

        return player != null && player.getGainControl() != null;
    }

    @Override
    public void skip(double percent) {
        if (!isDurationMeasurable()) {
            //Hmm... this shouldn't be happening.  We don't want to disturb the user by throwing an exception so we'll just
            //fireSongChanged to update everything (disabling the progress bar) and return.
            if (currentVideo != null) {
                fireSongChanged(currentVideo.getName());
            }
            return;
        }
        isSeeking = true;
        player.setMediaTime(new Time(percent * player.getDuration().getSeconds()));
        isSeeking = false;
    }

    @Override
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    private void firePlayerStateChanged(PlayerState state) {
        for (PlayerMediatorListener listener : listenerList) {
            listener.stateChanged(state);
        }
    }
    
    private void fireProgressUpdated() {
        fireProgressUpdated((float) (player.getMediaTime().getSeconds() / player.getDuration().getSeconds()));
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

    public void closeVideo() {

        if(playerInitializer.isInitializing()){
            playerInitializer.cancel();
        }
      
        displayDirector.close();
        
        killTimer();
        killPlayer();

        currentVideo = null;        
    }

    private void killTimer() {
        if (updateTimer != null) {
            if (updateTimer.isRunning()) {
                updateTimer.stop();
            }
            updateTimer = null;
        }
    }

    private void killPlayer() {
        if (player != null) {
            player.stop();
            player.removeControllerListener(controllerListener);
            player.close();
            player.deallocate();
            player = null;
        }
    }

    public void setFullScreen(boolean isFullScreen) {
        if (player == null) {
            return;
        }
        
        if (displayDirector.isFullScreen() == isFullScreen) {
            return;
        }
        
        //task is already running.  user probably hit crtl-f twice quickly.
        if (playerInitializer.isInitializing()){
            return;
        }
        
        boolean isPlaying = player.getState() == Controller.Started;

        reInitializePlayer(isFullScreen, isPlaying);

    }

    private void reInitializePlayer(boolean isFullScreen, boolean isPlaying) {

        if (player == null) {
            throw new IllegalStateException("Video player not initialized");
        }

        killTimer();

        Time time = isDurationMeasurable() ? player.getMediaTime() : null;

        killPlayer();

        initializePlayerOrNativeLaunch(currentVideo, time, isFullScreen, isPlaying);
    }

    public boolean isFullScreen() {
        return displayDirector.isFullScreen();
    }

    private boolean isDurationMeasurable() {
        if (player == null){
            return false;
        }
        long time = player.getDuration().getNanoseconds();        
        return Player.DURATION_UNBOUNDED.getNanoseconds() != time && Player.DURATION_UNKNOWN.getNanoseconds() != time && Time.TIME_UNKNOWN.getNanoseconds() != time;
    }

    private class VideoControllerListener implements ControllerListener {

        @Override
        public void controllerUpdate(final ControllerEvent controllerEvent) {
            SwingUtils.invokeNowOrLater(new Runnable() {

                @Override
                public void run() {

                    if (controllerEvent instanceof EndOfMediaEvent) {
                        setEndOfMedia();
                    } else if (controllerEvent instanceof StartEvent || controllerEvent.getSourceController().getState() == Controller.Started) {
                        firePlayerStateChanged(PlayerState.OPENED);
                        firePlayerStateChanged(PlayerState.PLAYING);
                        if (updateTimer == null) {
                            updateTimer = new Timer(100, new TimerAction());
                        }

                        if (!updateTimer.isRunning()) {
                            updateTimer.start();
                        }

                    } else if (controllerEvent instanceof StopEvent) {
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
            if (!isDurationMeasurable()) {
                updateTimer.stop();
                return;
            }

            if (player.getMediaTime().getSeconds() >= player.getDuration().getSeconds()) {
                // some FMJ players don't seem to fire EndOfMediaEvents so we need to do
                // this manually
                setEndOfMedia();
            } else {
                fireProgressUpdated();
            }
        }

    }    
    
    private void setEndOfMedia() {
        player.stop();
        player.setMediaTime(new Time(0));
        updateTimer.stop();
        firePlayerStateChanged(PlayerState.EOM);
        fireProgressUpdated(100f);

    }
    
    
    /**
     * Asynchronously initializes new video players.
     *
     */
    private class PlayerInitializer {
        
        private PlayerInitalizationWorker initializationWorker;
        
        public boolean isInitializing() {
            return initializationWorker != null;
        }

        public void initialize(final File file, Time time, boolean isFullScreen, boolean autoStart) {
            if (isInitializing()) {
                cancel();
            }

            initializationWorker = new PlayerInitalizationWorker(currentVideo, time, isFullScreen, autoStart);
            initializationWorker.execute();
        }

        public void cancel() {
            initializationWorker.cancelInitialization();
            initializationWorker = null;
        }
                

        private void finish(Player newPlayer, Time time, boolean isFullScreen, boolean autoStart) {
            initializationWorker = null;
            
            if(newPlayer == null){
                //New player creation failed.  The video was launched natively.
                displayDirector.close();
                return;
            }

            if (time != null) {
                newPlayer.setMediaTime(time);
            }

            newPlayer.addControllerListener(controllerListener);           

            player = newPlayer;
       
            displayDirector.show();   
            
            fireSongChanged(currentVideo.getName()); 
            
            if (updateTimer == null) {
                updateTimer = new Timer(100, new TimerAction());
            }
            updateTimer.start();
              
            startVideo(autoStart); 

        }
        
        private void startVideo(final boolean startVideo){
          
            ActionListener listener = new ActionListener() {                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (player != null) {
                        // start the player regardless of startVideo so that
                        // control panel is correctly updated
                        player.start();
                        // force fire these immediately to make sure
                        // everything updates
                        firePlayerStateChanged(PlayerState.PLAYING);
                        if (isDurationMeasurable()) {
                            fireProgressUpdated();
                        }

                        if (!startVideo) {
                            player.stop();
                        }
                    }
                }
            };
            
            // Prevents the flash of native video window by delaying 
            // video playback so that the video can be fully embedded first.
            Timer timer = new Timer(100, listener);
            timer.setRepeats(false);
            timer.start();

        }

        /**
         * SwingWorker that initializes the new player off of the EDT.
         */
        private class PlayerInitalizationWorker extends SwingWorker<Player, Void> {
            private final File mediaFile;

            private final Time time;

            private final boolean autoStart;

            private final boolean isFullScreen;

            private boolean canceled = false;   
            
            private final Container renderPanel = new JPanel(new BorderLayout());   

            public PlayerInitalizationWorker(File mediaFile, Time time, boolean isFullScreen,
                    boolean autoStart) {
                this.mediaFile = mediaFile;
                this.time = time;
                this.autoStart = autoStart;
                this.isFullScreen = isFullScreen;
                displayDirector.initialize(renderPanel, isFullScreen);                
            }

            /**
             * Cancels the player initialization. We don't want to interrupt the
             * thread by using cancel(boolean) because we need to properly
             * dispose of the player.
             */
            public void cancelInitialization() {
                canceled = true;
            }
            
            @Override
            protected Player doInBackground() throws Exception {
                try {
                    return new VideoPlayerFactory().createVideoPlayer(mediaFile, renderPanel);
                } catch (IncompatibleSourceException e) {
                    nativeLaunch(mediaFile);
                    return null;
                }
            }            

            private void nativeLaunch(File file) {
                NativeLaunchUtils.safeLaunchFile(file, categoryManager);
            }

            @Override
            protected void done() {
                GuiUtils.getMainFrame().setCursor(Cursor.getDefaultCursor());

                Player newPlayer = null;
                try {
                    newPlayer = get();
                } catch (InterruptedException e) {
                    // we're already finished so this can't happen
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

                if (canceled) {
                    if (newPlayer != null) {
                        newPlayer.close();
                        newPlayer.deallocate();
                    }
                    return;
                }

                finish(newPlayer, time, isFullScreen, autoStart);

            }
        }
    }

}
