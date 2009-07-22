package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

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
    
    private final PlayerMediator playerMediator;
    private final LibraryMediator libraryMediator;

    @Inject
    public MiniPlayerPanel(PlayerMediator playerMediator, LibraryMediator libraryMediator) {
        GuiUtils.assignResources(this);
        
        this.playerMediator = playerMediator;
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
        playerMediator.addMediatorListener(new PlayerListener());
        
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
            File currentFile = playerMediator.getCurrentSongFile();
            
            if (currentFile != null) { 
                libraryMediator.selectInLibrary(currentFile);
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
        return playerMediator.getStatus() == PlayerState.PLAYING || 
                playerMediator.getStatus() == PlayerState.SEEKING_PLAY ;
    }
    
    private void setPlaying(boolean playing){
        if (playing){
            playerMediator.resume();
        } else {
            playerMediator.pause();
        }
    }
    
    private class PlayerListener implements PlayerMediatorListener {
         @Override
        public void progressUpdated(float progress) {
        }

        @Override
        public void songChanged(String name) {
            //Show MiniPlayer when song is opened
            statusButton.setText(name);
            statusButton.getToolTip().setTipText(name);
            if(!isVisible())
                setVisible(true);
            statusButton.start();
        }

        @Override
        public void stateChanged(PlayerState state) {
            if (state == PlayerState.PLAYING || state == PlayerState.RESUMED){
                playPauseButton.setIcon(pauseIcon);
                playPauseButton.setRolloverIcon(pauseIconRollover);
                playPauseButton.setPressedIcon(pauseIconPressed);
                statusButton.start(); 
            } else if (state == PlayerState.STOPPED || state == PlayerState.EOM || state == PlayerState.UNKNOWN) {
                setVisible(false);
                statusButton.stop();
            } else if(state == PlayerState.PAUSED){
                playPauseButton.setIcon(playIcon);
                playPauseButton.setRolloverIcon(playIconRollover);
                playPauseButton.setPressedIcon(playIconPressed);
                statusButton.stop();
            }
        }
        
    }
}
