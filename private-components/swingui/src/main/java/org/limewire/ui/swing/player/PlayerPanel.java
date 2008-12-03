package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;

public class PlayerPanel extends JXPanel {

    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color innerBorder;
    @Resource private Color innerBackground;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    @Resource private Icon backIcon;
    @Resource private Icon backIconPressed;
    @Resource private Icon backIconRollover; 
    
    @Resource private Icon forwardIcon;
    @Resource private Icon forwardIconPressed;
    @Resource private Icon forwardIconRollover;
    
    @Resource private Icon playIcon;
    @Resource private Icon playIconPressed;
    @Resource private Icon playIconRollover;

    @Resource private Icon pauseIcon;
    @Resource private Icon pauseIconPressed;
    @Resource private Icon pauseIconRollover;
    @Resource private ImageIcon progressTrackLeftIcon;
    @Resource private ImageIcon progressTrackCenterIcon;
    @Resource private ImageIcon progressTrackRightIcon;
    @Resource private ImageIcon progressThumbUpIcon;
    @Resource private ImageIcon progressThumbDownIcon;
    @Resource private ImageIcon progressIcon;
        
    private final JButton backButton;
    private final JButton playButton;
    private final JButton pauseButton;
    private final JButton forwardButton;
    private final SongProgressBar progressSlider;
    private final JPanel statusPanel;
    private final JButton volumeButton;
    
    private final JPopupMenu volumeControlPopup;
    private final JSlider volumeSlider; 
    
    private final JLabel titleLabel;
    
    private final AudioPlayer player;
    
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
    private static final String FORWARD = "FORWARD";
    private static final String VOLUME = "VOLUME";

    public PlayerPanel(AudioPlayer player) {
        this.player = player;

        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, filly, alignx center"));
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
        
        volumeButton = new IconButton(forwardIcon, forwardIconRollover, forwardIconPressed);
        volumeButton.addActionListener(playerListener);
        volumeButton.setActionCommand(VOLUME);

        progressSlider = new SongProgressBar(progressTrackLeftIcon, progressTrackCenterIcon,
                progressTrackRightIcon, progressThumbUpIcon, progressThumbDownIcon, progressIcon);
        progressSlider.addChangeListener(new AudioProgressListener());
        progressSlider.setMaximum(500);
        progressSlider.setMaximumSize(new Dimension(206, 4));
        progressSlider.setMinimumSize(new Dimension(206, 4));
        progressSlider.setPreferredSize(new Dimension(206, 4));
        progressSlider.setSize(new Dimension(206, 4));
        
        statusPanel = new JPanel(new MigLayout());
        
        titleLabel = new JLabel("Sample Media Author - Sample Audio Title");
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        titleLabel.setMaximumSize(new Dimension(206, (int)titleLabel.getMaximumSize().getHeight()));
        titleLabel.setMinimumSize(new Dimension(206, (int)titleLabel.getMinimumSize().getHeight()));
        titleLabel.setPreferredSize(new Dimension(206, (int)titleLabel.getPreferredSize().getHeight()));
        titleLabel.setSize(new Dimension(206, (int)titleLabel.getSize().getHeight()));
        
        statusPanel.add(titleLabel);
        statusPanel.add(progressSlider, "dock south");
        statusPanel.setOpaque(false);
        
        int buttonWidth = backButton.getPreferredSize().width + 
        playButton.getPreferredSize().width + forwardButton.getPreferredSize().width; 
                
        Dimension statusSize = new Dimension(buttonWidth, statusPanel.getPreferredSize().height);
        statusPanel.setPreferredSize(statusSize);

        JXPanel innerPanel = new JXPanel(new MigLayout("insets 0 8 0 8, filly, gap 4, alignx center"));
        innerPanel.setOpaque(false);
        innerPanel.setBackgroundPainter(createStatusBackgroundPainter());
        
        innerPanel.add(backButton);
        innerPanel.add(pauseButton, "hidemode 3");
        innerPanel.add(playButton, "hidemode 3");
        innerPanel.add(forwardButton);
        innerPanel.add(statusPanel, "gapbottom 2");
        innerPanel.add(volumeButton);
                
        add(innerPanel, "gaptop 2, gapbottom 2");
        
        EventAnnotationProcessor.subscribe(this);

        player.addAudioPlayerListener(new PlayerListener());      
        
        volumeControlPopup = new JPopupMenu();
        volumeSlider = new JSlider(0,100);
        initVolumeControl();        
    }
    
    private void initVolumeControl() {
        volumeSlider.addChangeListener(new VolumeListener());
        volumeSlider.setOrientation(JSlider.VERTICAL);
        volumeControlPopup.add(volumeSlider);
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
            } else if (e.getActionCommand() == VOLUME) {
                volumeControlPopup.show(volumeButton, 0, 0);
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
            player.setVolume((double)volumeSlider.getValue() / 100);
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
           titleLabel.setText((String)properties.get("author") + " - " + (String)properties.get("title"));
           durationSecs = (int)(((Long)properties.get("duration")).longValue()/1000*1000);
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
    
    
    private Painter<JTextField> createStatusBackgroundPainter() {
        
        CompoundPainter<JTextField> compoundPainter = new CompoundPainter<JTextField>();
        
        RectanglePainter<JTextField> painter = new RectanglePainter<JTextField>();
        
        painter.setRounded(true);
        painter.setFillPaint(innerBackground);
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setInsets(new Insets(2,2,2,2));
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        compoundPainter.setPainters(painter, new BorderPainter<JTextField>(this.arcWidth, this.arcHeight,
                this.innerBorder, this.bevelLeft, this.bevelTop1, this.bevelTop2, 
                this.bevelRight,  this.bevelBottom, AccentType.SHADOW));
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
}
