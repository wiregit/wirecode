package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.media.CannotRealizeException;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StopEvent;
import javax.media.Time;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.mainframe.MainPanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.SystemUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class VideoPlayerMediator implements PlayerMediator {
    
    private Player player;
    private File currentVideo;
    private PlayerControlPanelFactory controlPanelFactory;
    private final MainPanel mainPanel;
    private final List<PlayerMediatorListener> listenerList;
    private volatile Timer updateTimer;
    private final HeaderBarDecorator headerBarDecorator;
    private final Navigator nav;
    private final CategoryManager categoryManager;
    private VideoPanel videoPanel;
    
    @Inject
    VideoPlayerMediator(PlayerControlPanelFactory controlPanelFactory, MainPanel mainPanel,
            HeaderBarDecorator headerBarDecorator, Navigator nav, CategoryManager categoryManager){
        this.controlPanelFactory = controlPanelFactory;
        this.mainPanel = mainPanel;
        this.headerBarDecorator = headerBarDecorator;
        this.listenerList = new ArrayList<PlayerMediatorListener>();
        this.nav = nav;
        this.categoryManager = categoryManager;
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
        return player != null && player.getState() != Controller.Started;
    }

    @Override
    public boolean isPlaying(File file) {
        if (file.equals(currentVideo) && player != null) {
        return player.getState() == Controller.Started;
        }
        return false;
    }

    @Override
    public boolean isPlaylistSupported() {
        return false;
    }

    @Override
    public boolean isSeekable() {
        return player != null && player.getDuration() != Player.DURATION_UNBOUNDED
                && player.getDuration() != Player.DURATION_UNKNOWN;
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
        //TODO: full screen?
        initializeVideoPanel();
        mainPanel.showTemporaryPanel(videoPanel);
        initializePlayer(file, null, true);
    }
    
    private void initializePlayer(File file, Time time, boolean autoPlay){
        new InitializationWorker(file, time, autoPlay).execute();
    }
    
    private void initializeVideoPanel(){
        videoPanel = new VideoPanel(controlPanelFactory.createVideoControlPanel(), 
                headerBarDecorator, this);
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
        // TODO: duration can be UNBOUNDED or UNKNOWN
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
        videoPanel = null;

        if (isFullScreen) {
            isFullScreen = false;
            fullScreenFrame.setVisible(false);
            fullScreenFrame = null;
            GuiUtils.getMainFrame().setVisible(true);
        }
        nav.goBack();
    }
    
    private void killTimer(){
        if(updateTimer != null && updateTimer.isRunning()){
            updateTimer.stop();
            updateTimer = null;
        }
    }
    
    private void killPlayer() {
        player.stop();
        try {
            player.deallocate();
        } catch (Throwable e) {
            // TODO
            // will this actually stop the crash? doubtful.
            e.printStackTrace();
        }
        player = null;
    }

    private JFrame fullScreenFrame;
    private boolean isFullScreen;
    
    public boolean isFullScreen(){
        return isFullScreen;
    }
    public void setFullScreen(boolean isFullScreen) {
        if(player == null){
            throw new IllegalStateException("Video player not initialized");
        }
        
        if (this.isFullScreen == isFullScreen){
            return;
        }
        
        this.isFullScreen = isFullScreen;
        killTimer();

        Time time = player.getMediaTime();

        boolean isPlaying = player.getState() == Controller.Started;
        if (isPlaying) {
            player.stop();
        }
        try {
            player.deallocate();
        } catch (Throwable e) {
            // TODO will this actually stop the crash? doubtful.
            e.printStackTrace();
        }
        
        initializeVideoPanel();

        if (isFullScreen) {
            GuiUtils.getMainFrame().setVisible(false);
            fullScreenFrame = new LimeJFrame();
            fullScreenFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            fullScreenFrame.setAlwaysOnTop(true);
            SystemUtils.setWindowTopMost(fullScreenFrame);
            fullScreenFrame.setUndecorated(true);
            fullScreenFrame.add(videoPanel, BorderLayout.CENTER);            

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            fullScreenFrame.setBounds(0,0,screenSize.width, screenSize.height);
            
            GuiUtils.getMainFrame().setVisible(false);
            fullScreenFrame.setVisible(true);
            GuiUtils.getMainFrame().toFront();

        } else {
            fullScreenFrame.setVisible(false);
            GuiUtils.getMainFrame().setVisible(true);
            mainPanel.showTemporaryPanel(videoPanel);
            //TODO: need to focus on the window
            GuiUtils.getMainFrame().toFront();
        }
        
        initializePlayer(currentVideo, time, isPlaying);
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

            if (player.getMediaTime().getSeconds() >= player.getDuration().getSeconds()) {
                player.stop();
                updateTimer.stop();
                player.setMediaTime(new Time(0));
                fireProgressUpdated(0);
                firePlayerStateChanged(PlayerState.EOM);
                System.out.println("EOM");
            } else {
                // TODO: duration can be UNBOUNDED or
                // UNKNOWN
                fireProgressUpdated((float) (player.getMediaTime().getSeconds() / player
                        .getDuration().getSeconds()));
            }
        }

    }

    private class InitializationWorker extends SwingWorker<Player, Void> {
        
        private final File file;
        private final Time time;
        private final boolean autoPlay;

        /**
         * 
         * @param file the video file to be played
         * @param time the starting time of the video.  null to start at the beginning.
         */
        public InitializationWorker(File file, Time time, boolean autoPlay){
            this.file = file;
            this.time = time;
            this.autoPlay = autoPlay;
        }

        @Override
        protected Player doInBackground() throws Exception {
            currentVideo = file;
            try {
                Player player = Manager.createRealizedPlayer(file.toURI().toURL());  
                if(time!=null){
                    player.setMediaTime(time);
                }
                return player;
            } catch (NoPlayerException e) {
               nativeLaunch(file);
            } catch (CannotRealizeException e) {
               nativeLaunch(file);
            } catch (MalformedURLException e) {
               nativeLaunch(file);
            } catch (IOException e) {
                //TODO: how should this be handled?
                throw e;
            }
            return null;
        }
        
        @Override
        public void done(){
            //TODO: handle exceptions & null player
            try {
                player = get();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            player.addControllerListener(new VideoControllerListener());
            videoPanel.add(player.getVisualComponent());
            videoPanel.revalidate();
            player.start();
            updateTimer = new Timer(1000, new TimerAction());
            updateTimer.start();
            
            fireSongChanged(file.getName());   

            if (!autoPlay) {
                //start and stop to get initial frame on screen
                pause();
            }

        }
        
    }
    
}
