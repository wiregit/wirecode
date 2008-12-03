package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.MediaSlider;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

public class PlayerPanel extends JXPanel {

    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 100;

    @Resource
    private Icon backIcon;
    @Resource
    private Icon backIconPressed;
    @Resource
    private Icon backIconRollover; 
    
    @Resource
    private Icon forwardIcon;
    @Resource
    private Icon forwardIconPressed;
    @Resource
    private Icon forwardIconRollover;
    
    @Resource
    private Icon playIcon;
    @Resource
    private Icon playIconPressed;
    @Resource
    private Icon playIconRollover;

    @Resource
    private Icon pauseIcon;
    @Resource
    private Icon pauseIconPressed;
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon closeIcon;
    @Resource
    private Icon closeIconPressed;
    @Resource
    private Icon closeIconRollover;

    @Resource
    private Icon volume0Icon;
    @Resource
    private Icon volume1Icon;
    @Resource
    private Icon volume2Icon;
    @Resource
    private Icon volume3Icon;
    

    @Resource
    private ImageIcon volumeTrackLeftIcon;
    @Resource
    private ImageIcon volumeTrackCenterIcon;
    @Resource
    private ImageIcon volumeTrackRightIcon;
    @Resource
    private ImageIcon volumeThumbUpIcon;
    @Resource
    private ImageIcon volumeThumbDownIcon;
    

    @Resource
    private ImageIcon progressTrackLeftIcon;
    @Resource
    private ImageIcon progressTrackCenterIcon;
    @Resource
    private ImageIcon progressTrackRightIcon;
    @Resource
    private ImageIcon progressThumbUpIcon;
    @Resource
    private ImageIcon progressThumbDownIcon;
    @Resource
    private ImageIcon progressIcon;
    
    @Resource
    private Color statusBackgroundColor;
    
    private JButton backButton;
    private JButton playButton;
    private JButton pauseButton;
    private JButton forwardButton;
    private JButton closeButton;
    private JSlider volumeSlider;
    private SongProgressBar progressSlider;
    private JPanel statusPanel;
    private JLabel volumeLabel;
    
    private JLabel titleLabel;
    private JLabel artistLabel;
    private JLabel albumLabel;
    
    private AudioPlayer player;
    
    /**
     * length of the current audio in seconds
     */
    private int durationSecs;

    /**
     * length of the current audio in bytes
     */
    private int byteLength;

    private static final String BACK = "BACK";
    private static final String PLAY = "PLAY";
    private static final String PAUSE = "PAUSE";
    private static final String CLOSE = "CLOSE";
    private static final String FORWARD = "FORWARD";

    public PlayerPanel(AudioPlayer player) {
        this.player = player;

        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout());
        setOpaque(false);
        
        ActionListener playerListener = new ButtonListener();

        backButton = new IconButton(backIcon, backIconRollover, backIconPressed);
        backButton.addActionListener(playerListener);
        backButton.setActionCommand(BACK);
        
        playButton = new IconButton(playIcon, playIconRollover, playIconPressed);
        playButton.addActionListener(playerListener);
        playButton.setActionCommand(PLAY);

        pauseButton = new IconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
        pauseButton.addActionListener(playerListener);
        pauseButton.setActionCommand(PAUSE);
        pauseButton.setVisible(false);

        forwardButton = new IconButton(forwardIcon, forwardIconRollover, forwardIconPressed);
        forwardButton.addActionListener(playerListener);
        forwardButton.setActionCommand(FORWARD);

        closeButton = new IconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.addActionListener(playerListener);
        closeButton.setActionCommand(CLOSE);

        volumeSlider = new MediaSlider(volumeTrackLeftIcon, volumeTrackCenterIcon,
                volumeTrackRightIcon, volumeThumbUpIcon, volumeThumbDownIcon);
        volumeSlider.setMinimum(MIN_VOLUME);
        volumeSlider.setMaximum(MAX_VOLUME);
        volumeSlider.addChangeListener(new VolumeListener());

        progressSlider = new SongProgressBar(progressTrackLeftIcon, progressTrackCenterIcon,
                progressTrackRightIcon, progressThumbUpIcon, progressThumbDownIcon, progressIcon);
        progressSlider.addChangeListener(new AudioProgressListener());
        progressSlider.setMaximum(500);
        
        volumeLabel = new JLabel(volume1Icon);    

        
        statusPanel = new JPanel(new MigLayout("wrap 1"));
        statusPanel.setBackground(statusBackgroundColor);
        
        titleLabel = new JLabel("Sample Audio Title");
        FontUtils.bold(titleLabel);
        artistLabel = new JLabel("Placeholder Arist");
        albumLabel = new JLabel("Collection");

        statusPanel.add(titleLabel);
        statusPanel.add(artistLabel);
        statusPanel.add(albumLabel);
        
        int buttonWidth = backButton.getPreferredSize().width + 
        playButton.getPreferredSize().width + forwardButton.getPreferredSize().width; 
        
        
        Dimension statusSize = new Dimension(buttonWidth, statusPanel.getPreferredSize().height);
        statusPanel.setPreferredSize(statusSize);

        Dimension volSize = new Dimension(buttonWidth - volumeLabel.getPreferredSize().width,
                volumeSlider.getPreferredSize().height);
        volumeSlider.setPreferredSize(volSize);
       
        add(backButton, "split 3, gapleft 2px,");
        add(pauseButton, "hidemode 3");
        add(playButton, "hidemode 3");
        add(forwardButton);
        add(statusPanel, "spany 2, spanx 3, grow, gapright 5px, wrap");
        add(volumeSlider, "split 2");
        add(volumeLabel, "wrap, gapleft 0px");
        add(progressSlider, "dock south, gaptop 5px, gapbottom 5px");
        EventAnnotationProcessor.subscribe(this);

        player.addAudioPlayerListener(new PlayerListener());      
    }

    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == PLAY){
                player.unpause();
            } else if (e.getActionCommand() == PAUSE){
                player.pause();
            } else if (e.getActionCommand() == FORWARD) {
            } else if (e.getActionCommand() == BACK) {
            }            
        }
    }
  
    private class AudioProgressListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {            
            if (progressSlider.getMaximum() != 0 && progressSlider.getValueIsAdjusting()){
                int position = byteLength * progressSlider.getValue() / progressSlider.getMaximum();
                player.seekLocation(position);
            }
        }
    }

    private class VolumeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (volumeSlider.getValue() > .66 * MAX_VOLUME){
                volumeLabel.setIcon(volume3Icon);
            } else if (volumeSlider.getValue() > .33 * MAX_VOLUME){
                volumeLabel.setIcon(volume2Icon);
            } else if (volumeSlider.getValue() > MIN_VOLUME){
                volumeLabel.setIcon(volume1Icon);                
            } else {//Volume is MIN_VOLUME
                volumeLabel.setIcon(volume0Icon);                
            }
            player.setVolume((double)volumeSlider.getValue() / MAX_VOLUME);
        }
    }
    
    private class PlayerListener implements AudioPlayerListener{
       
        @Override
        public void progressChange(int bytesread) {
            if (byteLength != 0 && !progressSlider.getValueIsAdjusting()) {
                progressSlider.setValue(durationSecs * bytesread / byteLength);
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
           titleLabel.setText((String)properties.get("title"));
           artistLabel.setText((String)properties.get("author"));
           albumLabel.setText((String)properties.get("album"));
           //"duration" is in microseconds
           durationSecs = (int)(((Long)properties.get("duration")).longValue()/1000/1000);
           progressSlider.setMaximum(durationSecs);
           
           byteLength = (Integer)properties.get("audio.length.bytes");
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            if (player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY){
                playButton.setVisible(false);
                pauseButton.setVisible(true);
            } else {
                playButton.setVisible(true);
                pauseButton.setVisible(false);
            }            
        }
        
    }
}
