package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.HorizontalLayout;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.CommonUtils;

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

    private JButton playPauseButton;

    private MarqueeButton statusButton;
    
    private AudioPlayer player;

    public MiniPlayerPanel(AudioPlayer player) {
        super(new HorizontalLayout());
        GuiUtils.assignResources(this);
        this.player = player;
        
        setOpaque(false);

        playPauseButton = new JButton();
        playPauseButton.setMargin(new Insets(0, 0, 0, 0));
        playPauseButton.setBorderPainted(false);
        playPauseButton.setContentAreaFilled(false);
        playPauseButton.setFocusPainted(false);
        playPauseButton.setRolloverEnabled(true);
        playPauseButton.setIcon(playIcon);
        playPauseButton.setRolloverIcon(playIconRollover);
        playPauseButton.setPressedIcon(playIconPressed);
        playPauseButton.setHideActionText(true);
        playPauseButton.addActionListener(new PlayListener());

        statusButton = new MarqueeButton("nothing", 16);
        statusButton.setForeground(foregroundColor);    
        statusButton.addActionListener(new ShowPlayerListener());

        add(playPauseButton);
        add(statusButton);
     
        setMaximumSize(getPreferredSize());
        player.addAudioPlayerListener(new PlayerListener());
    }

    private class ShowPlayerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            new DisplayPlayerEvent().publish();
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
    
    private class MarqueeButton extends JButton {
        private JToolTip toolTip = new JToolTip();
        private int position;
        private int maxPosition;
        private Timer timer;

        private int repeatCount = 5;
        private int initialDelay = 30;
        private String marqueeString;
        //private int maxDelay = 50;

        public MarqueeButton(String text, int maxCharsShown) {
            super(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            toolTip.setComponent(this);
            setMargin(new Insets(0, 0, 0, 0));
            setBorderPainted(false);
            setRolloverEnabled(true);
            setContentAreaFilled(false);
            setOpaque(false);
            setFocusPainted(false);
            setMaxChars(maxCharsShown);
            setToolTipText(getText() + " (1:23/4:56)");
        }
        
        public void start() {
            if (timer != null) {
                timer.stop();
            }


            StringBuilder marqueeBuilder = new StringBuilder();
            for (int i = 0; i < repeatCount; i++) {
                marqueeBuilder.append(getText());
                if (i != repeatCount - 1) {
                    marqueeBuilder.append("          ");
                }
            }
            
            marqueeString = marqueeBuilder.toString();

            JLabel marqueeLabel = new JLabel(marqueeString);
            marqueeLabel.setFont(getFont());
            
            JLabel fullLabel = new JLabel(getText());
            fullLabel.setFont(getFont());
            
            maxPosition = marqueeLabel.getPreferredSize().width - fullLabel.getPreferredSize().width;
            
            timer = new Timer(initialDelay, new MarqueeAction());
            timer.start();
        }
        
        @Override
        public void paintComponent(Graphics g) {
            if (timer != null && timer.isRunning()) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                g2.setColor(getForeground());
                //TODO: do this properly, ie not - 1
                g2.drawString(marqueeString, -position, getHeight() / 2 + metrics.getAscent()/2 - 1);
                g2.dispose();
            } else {
                super.paintComponent(g);
            }
        }
        
        public void setMaxChars(int maxCharsShown){
            StringBuilder fillerBuilder = new StringBuilder();
            for(int i = 0; i < maxCharsShown; i++){
                fillerBuilder.append('X');
            }
            String oldText = getText();
            setText(fillerBuilder.toString());
            setMaximumSize(getPreferredSize());
            setPreferredSize(getMaximumSize());
            setText(oldText);
        }

        @Override
        public JToolTip createToolTip(){
            return toolTip;
        }
        
        public JToolTip getToolTip(){
            return toolTip;
        }
        
        
        private class MarqueeAction implements ActionListener {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                position += 1;
                
                repaint();
                
                if (position >= maxPosition){
                    timer.stop();
                }
            }       
        }
        
        //not using variable speed for now
//        private class MarqueeAction implements ActionListener {
//            
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (repeatCount <= 0) {
//                    timer.stop();
//                }
//                
//                if (position >= maxPosition) {
//                    position = -getWidth();
//                    repeatCount--;
//                    timer.setDelay(initialDelay);
//                }
//                position += 1;
//                repaint();
//                if (position % 5 == 0 && timer.getDelay() < maxDelay) {
//                    timer.setDelay(timer.getDelay() + 3);
//                }
//            }       
//        }
       
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
               statusButton.setToolTipText(title + " - " + artist + 
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
            setVisible(true);
            title = (String) properties.get("title");
            artist = (String) properties.get("author");
            statusButton.setText(title + " - " + artist);
            statusButton.start();
            // "duration" is in microseconds
            durationSecs = (int) (((Long) properties.get("duration")).longValue() / 1000 / 1000);
            byteLength = (Integer) properties.get("audio.length.bytes");
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            if (player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY){
                playPauseButton.setIcon(pauseIcon);
                playPauseButton.setRolloverIcon(pauseIconRollover);
                playPauseButton.setPressedIcon(pauseIconPressed);
            } else {
                playPauseButton.setIcon(playIcon);
                playPauseButton.setRolloverIcon(playIconRollover);
                playPauseButton.setPressedIcon(playIconPressed);
            }
        }
        
    }
    
    
}
