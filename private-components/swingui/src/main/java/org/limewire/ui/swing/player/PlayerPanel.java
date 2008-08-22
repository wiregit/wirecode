package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.ui.swing.components.MediaSlider;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

public class PlayerPanel extends JXCollapsiblePane {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new PlayerPanel(), BorderLayout.NORTH);
        frame.pack();
        frame.setVisible(true);

    }
    
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
    private Icon volumeIcon;
    

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
    
    private JButton backButton;
    private JButton playButton;
    private JButton pauseButton;
    private JButton forwardButton;
    private JButton closeButton;
    private JSlider volumeSlider;
    private SongProgressBar progressSlider;
    private JPanel statusPanel;
    //necessary to show component on top of heavyweight browser
    private Panel heavyPanel;
    
    private JLabel titleLabel;
    private JLabel artistLabel;
    private JLabel albumLabel;

    private static final String BACK = "BACK";
    private static final String PLAY = "PLAY";
    private static final String PAUSE = "PAUSE";
    private static final String CLOSE = "CLOSE";
    private static final String FORWARD = "FORWARD";

    public PlayerPanel() {
        super(Direction.UP);
        setCollapsed(true);
        setLayout(new BorderLayout());
        
        heavyPanel = new Panel(new MigLayout());
        add(heavyPanel);
        GuiUtils.assignResources(this);
        
        heavyPanel.setBackground(Color.LIGHT_GRAY);
        
        ActionListener playerListener = new PlayerListener();

        backButton = GuiUtils.createIconButton(backIcon, backIconRollover, backIconPressed);
        backButton.addActionListener(playerListener);
        backButton.setActionCommand(BACK);
        
        playButton = GuiUtils.createIconButton(playIcon, playIconRollover, playIconPressed);
        playButton.addActionListener(playerListener);
        playButton.setActionCommand(PLAY);

        pauseButton = GuiUtils.createIconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
        pauseButton.addActionListener(playerListener);
        pauseButton.setActionCommand(PAUSE);
        pauseButton.setVisible(false);

        forwardButton = GuiUtils.createIconButton(forwardIcon, forwardIconRollover, forwardIconPressed);
        forwardButton.addActionListener(playerListener);
        forwardButton.setActionCommand(FORWARD);

        closeButton = GuiUtils.createIconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.addActionListener(playerListener);
        closeButton.setActionCommand(CLOSE);

        volumeSlider = new MediaSlider(volumeTrackLeftIcon, volumeTrackCenterIcon,
                volumeTrackRightIcon, volumeThumbUpIcon, volumeThumbDownIcon);
        volumeSlider.addChangeListener(new VolumeListener());

        progressSlider = new SongProgressBar(progressTrackLeftIcon, progressTrackCenterIcon,
                progressTrackRightIcon, progressThumbUpIcon, progressThumbDownIcon, progressIcon);
        progressSlider.addChangeListener(new AudioProgressListener());
        progressSlider.setMaximum(500);
        
        JLabel volumeLabel = new JLabel(volumeIcon);    

        
        statusPanel = new JPanel(new VerticalLayout());
        //TODO: resources
        statusPanel.setBackground(Color.WHITE);
        
        titleLabel = new JLabel("Clean Your Room");
        FontUtils.bold(titleLabel);
        artistLabel = new JLabel("The Your Moms");
        albumLabel = new JLabel("The New Jersey Album");

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
       
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBackground(Color.DARK_GRAY);
        closePanel.add(closeButton);
        heavyPanel.add(closePanel, "dock north, gapbottom 5px");
        heavyPanel.add(backButton, "split 3");
        heavyPanel.add(pauseButton, "hidemode 3");
        heavyPanel.add(playButton, "hidemode 3");
        heavyPanel.add(forwardButton);
        heavyPanel.add(statusPanel, "spany 2, spanx 3, grow, gapright 5px, wrap");
        heavyPanel.add(volumeSlider, "split 2");
        heavyPanel.add(volumeLabel, "wrap");
        heavyPanel.add(progressSlider, "dock south, gaptop 5px, gapbottom 5px");
        EventAnnotationProcessor.subscribe(this);
    }
    
    @EventSubscriber
    public void handleDisplayEvent(DisplayPlayerEvent event) {
        if (isCollapsed()) {
            resetBounds();
        }
        // toggle
        setCollapsed(!isCollapsed());
        heavyPanel.setVisible(!isCollapsed());        
    }
    
    private class PlayerListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == PLAY){
                playButton.setVisible(false);
                pauseButton.setVisible(true);
            } else if (e.getActionCommand() == PAUSE){
                playButton.setVisible(true);
                pauseButton.setVisible(false);
            } else if (e.getActionCommand() == FORWARD) {
            } else if (e.getActionCommand() == BACK) {
            } else if (e.getActionCommand() == CLOSE) {
                System.out.println("close");
                setCollapsed(true); 
            }
            
        }
        
    }
    
    private void resetBounds() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = heavyPanel.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    public void resize() {
        if (!isCollapsed()) {
            resetBounds();
        }
    }
    
    private class AudioProgressListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            //TODO: change audio progress
        }
    }

    private class VolumeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            //TODO: change volume
        }
    }
}
