package org.limewire.ui.swing.player;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.IncompatibleSourceException;
import javax.media.Player;
import javax.media.StopEvent;
import javax.media.Time;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.sf.fmj.concurrent.ExecutorServiceManager;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import foxtrot.Job;
import foxtrot.Worker;

@Singleton
class VideoPlayerMediator implements PlayerMediator {

    private final String playlistsNotSupported = I18n.tr("Playlists not supported in video");;
    private Player player;
    private File currentVideo;
    private final VideoDisplayDirector displayDirector;
    private final List<PlayerMediatorListener> listenerList;
    private volatile Timer updateTimer;
    private final CategoryManager categoryManager;
    private final Navigator navigator;
    private NavigationListener closeVideoOnNavigation;
    private boolean isSeeking;

    @Inject
    VideoPlayerMediator(VideoDisplayDirector displayDirector, Navigator navigator,
            CategoryManager categoryManager) {
        this.displayDirector = displayDirector;
        this.navigator = navigator;
        this.categoryManager = categoryManager;
        this.listenerList = new ArrayList<PlayerMediatorListener>();
        ExecutorServiceManager.setExecutorService(ExecutorsHelper.newFixedSizeThreadPool(1, "Video ThreadPool"));
    }

    private void registerNavigationListener() {
        if (closeVideoOnNavigation == null) {
            closeVideoOnNavigation = new CloseVideoOnNavigationListener();
        }

        navigator.addNavigationListener(closeVideoOnNavigation);
    }
    
    private void removeNavigationListener(){
        navigator.removeNavigationListener(closeVideoOnNavigation);
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
        if (initializePlayerOrNativeLaunch(file, null)) {
            showVideo(false, true);
            registerNavigationListener();     
        }
    }
    
    private void showVideo(boolean isFullScreen, boolean startVideo) {
        displayDirector.show(player.getVisualComponent(), isFullScreen);
        fireSongChanged(currentVideo.getName());
        if (startVideo) {
            // Must be SwingUtilities not SwingUtils.  We actually want this invoked later.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (player != null) {
                        player.start();
                    }
                }
            });
        }

        if (isPlaying(currentVideo)) {
            firePlayerStateChanged(PlayerState.PLAYING);
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
    private boolean initializePlayerOrNativeLaunch(final File file, Time time) {
        GuiUtils.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        currentVideo = file;
        player = (Player) Worker.post(new Job() {
            @Override
            public Object run() {
                try {
                    return VideoPlayerFactory.createVideoPlayer(file);
                } catch (IncompatibleSourceException e) {
                    nativeLaunch(file);
                    return null;
                } catch (MalformedURLException e) {
                    nativeLaunch(file);
                    return null;
                } catch (IOException e) {
                    // TODO: how should this be handled?
                    nativeLaunch(file);
                    return null;
                }
            }
        });
        
         GuiUtils.getMainFrame().setCursor(Cursor.getDefaultCursor());
        
        if(player == null){
            return false;
        }

        if (time != null) {
            player.setMediaTime(time);
        }

        player.addControllerListener(new VideoControllerListener());
        updateTimer = new Timer(1000, new TimerAction());
        updateTimer.start();

        return true;
    }

    private void nativeLaunch(File file) {
        NativeLaunchUtils.safeLaunchFile(file, categoryManager);
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
        player.start();

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
        return player.getGainControl() != null;
    }

    @Override
    public void skip(double percent) {
        if (!isDurationMeasurable()) {
            throw new IllegalStateException("Can not skip when duration is unmeasurable");
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
        
        removeNavigationListener();
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
        player.close();
        player.deallocate();
        player = null;
    }

    public void setFullScreen(boolean isFullScreen) {

        if (displayDirector.isFullScreen() == isFullScreen) {
            return;
        }
        
        boolean isPlaying = player.getState() == Controller.Started;

        reInitializePlayer();

        showVideo(isFullScreen, isPlaying);
    }

    private void reInitializePlayer() {

        if (player == null) {
            throw new IllegalStateException("Video player not initialized");
        }

        killTimer();

        Time time = isDurationMeasurable() ? player.getMediaTime() : null;

        killPlayer();

        boolean playerInitialized = initializePlayerOrNativeLaunch(currentVideo, time);

        if (!playerInitialized) {
            // TODO: how should we handle this?
            throw new IllegalStateException("Video player initialization failed");
        }

    }

    public boolean isFullScreen() {
        return displayDirector.isFullScreen();
    }

    private boolean isDurationMeasurable() {
        return player != null && player.getDuration() != Player.DURATION_UNBOUNDED
                && player.getDuration() != Player.DURATION_UNKNOWN;
    }

    private class VideoControllerListener implements ControllerListener {

        @Override
        public void controllerUpdate(final ControllerEvent controllerEvent) {
            SwingUtils.invokeNowOrLater(new Runnable() {

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
                // FMJ doesn't seem to fire EndOfMediaEvents so we need to do
                // this manually
                player.stop();
                updateTimer.stop();
                player.setMediaTime(new Time(0));
                firePlayerStateChanged(PlayerState.EOM);
            } else {
                fireProgressUpdated((float) (player.getMediaTime().getSeconds() / 
                        player.getDuration().getSeconds()));
            }
        }

    }
    
    private class CloseVideoOnNavigationListener implements NavigationListener {

        @Override
        public void itemSelected(NavCategory category, NavItem navItem,
                NavSelectable selectable, NavMediator navMediator) {
            closeVideoPanel();                    
        }

        @Override
        public void categoryAdded(NavCategory category) {
            // do nothing
        }

        @Override
        public void categoryRemoved(NavCategory category, boolean wasSelected) {
            // do nothing
        }

        @Override
        public void itemAdded(NavCategory category, NavItem navItem) {
            // do nothing
        }

        @Override
        public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected) {
            // do nothing
        }
    }

}
