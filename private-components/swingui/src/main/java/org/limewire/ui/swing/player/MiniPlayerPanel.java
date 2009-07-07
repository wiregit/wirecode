package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

public class MiniPlayerPanel extends JPanel {
  
    @Resource
    private Icon pauseIcon;
    @Resource
    private Icon pauseIconPressed;
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon playIcon;
    @Resource
    private Icon playIconPressed;
    @Resource
    private Icon playIconRollover;
    @Resource 
    private Color foregroundColor;
    @Resource
    private Font font;

    private JButton playPauseButton;

    private MarqueeButton statusButton;
    
    private final AudioPlayer player;
    private final LibraryMediator libraryMediator;

    @Inject
    public MiniPlayerPanel(AudioPlayer player, LibraryMediator libraryMediator) {
        GuiUtils.assignResources(this);
        
        this.player = player;
        this.libraryMediator = libraryMediator;
        
        setLayout(new MigLayout("insets 0", "4[][]", "0[]0"));
        setOpaque(false);

        playPauseButton = new JXButton();
        playPauseButton.setMargin(new Insets(0, 0, 0, 0));
        playPauseButton.setBorderPainted(false);
        playPauseButton.setContentAreaFilled(false);
        playPauseButton.setFocusPainted(false);
        playPauseButton.setRolloverEnabled(true);
        playPauseButton.setIcon(playIcon);
        Dimension playPauseDimensions = new Dimension(playIcon.getIconWidth(), playIcon.getIconHeight());
        playPauseButton.setMaximumSize(playPauseDimensions);
        playPauseButton.setPreferredSize(playPauseDimensions);
        playPauseButton.setRolloverIcon(playIconRollover);
        playPauseButton.setPressedIcon(playIconPressed);
        playPauseButton.setHideActionText(true);
        playPauseButton.addActionListener(new PlayListener());

        statusButton = new MarqueeButton(I18n.tr("Nothing selected"), 16);
        Dimension statusButtonDimensions = new Dimension(Integer.MAX_VALUE, playIcon.getIconHeight());
        statusButton.setMaximumSize(statusButtonDimensions);
        statusButton.setFont(font);
        statusButton.setForeground(foregroundColor);    
        statusButton.addActionListener(new ShowPlayerListener());

        add(playPauseButton, "gapbottom 0, gaptop 0");
        add(statusButton, "gapbottom 0, gaptop 0");
     
        setMaximumSize(getPreferredSize());
        player.addAudioPlayerListener(new PlayerListener());
        
        //hide the player if setting is disabled
        SwingUiSettings.PLAYER_ENABLED.addSettingListener(new SettingListener(){
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        MiniPlayerPanel.this.setVisible(false);
                    }
                });
            }
        });
    }

    private class ShowPlayerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            AudioSource currentSource = player.getCurrentSong();
            
            if (currentSource != null) { 
                libraryMediator.selectInLibrary(currentSource.getFile());
            }
        }
    }
    
    private class PlayListener implements ActionListener {  
        @Override
        public void actionPerformed(ActionEvent e) {       
            setPlaying(!isPlaying());                        
        }

    }
    
    private boolean isPlaying(){
        return player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY ;
    }
    
    private void setPlaying(boolean playing){
        if (playing){
            player.unpause();
        } else {
            player.pause();
        }
    }
    
    private class PlayerListener implements AudioPlayerListener{
        //duration in seconds
        private int durationSecs;
        private int byteLength;
        private int currentSecs;
        private String title;
        private String artist;

        @Override
        public void progressChange(int bytesread) {
            if (byteLength != 0) {
               currentSecs = durationSecs * bytesread / byteLength;
               statusButton.setToolTipText(artist + " - " + title + 
                       " (" + CommonUtils.seconds2time(currentSecs) + 
                       "/" + CommonUtils.seconds2time(durationSecs) + ")");
            }
            if (statusButton.getToolTip().isVisible()) {
                statusButton.getToolTip().setTipText(statusButton.getToolTipText());
                statusButton.getToolTip().repaint();
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            //Show MiniPlayer when song is opened
            if(!isVisible())
                setVisible(true);
            
            title = (String) properties.get("title");
            artist = (String) properties.get("author");
            
            if (title != null && artist != null) {
                statusButton.setText(artist + " - " + title);
            } 
            else {
                String text = null;
                AudioSource currentSource = player.getCurrentSong();
                if (currentSource != null) {
                    File file = currentSource.getFile();
                    if (file !=null) {
                        text = file.getName();
                    }
                }
                if (text == null) {
                    text = I18n.tr("Unknown");
                }
                statusButton.setText(text);
            }
            
            statusButton.start();
            
            Object duration = properties.get("duration");
            if (duration != null) {
                durationSecs = (int)(((Long)duration).longValue()/1000/1000);
            } 
            else {
                durationSecs = 0;
            }
            
            if (properties.get("audio.length.bytes") != null) {
                byteLength = (Integer)properties.get("audio.length.bytes");
            } 
            else {
                byteLength = 0;
            }
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            if (player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY){
                playPauseButton.setIcon(pauseIcon);
                playPauseButton.setRolloverIcon(pauseIconRollover);
                playPauseButton.setPressedIcon(pauseIconPressed);
                statusButton.start();
                
            } else if (player.getStatus() == PlayerState.STOPPED) {
                setVisible(false);
                
            } else {
                playPauseButton.setIcon(playIcon);
                playPauseButton.setRolloverIcon(playIconRollover);
                playPauseButton.setPressedIcon(playIconPressed);
                statusButton.stop();
            }
        }
        
    }
    
    
}
