package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

public class VideoPanel {

    private final HeaderBar headerBar = new HeaderBar();

    private JCheckBox fullScreenButton;

    private JButton closeButton;

    private final VideoPlayerMediator videoMediator;
    
    private Component videoRenderer;
    
    /**
     * the video panel containing video and controls.
     */
    private JPanel videoPanel = new JPanel(new BorderLayout());
    

    private final MigLayout fitToScreenLayout = new MigLayout("debug, novisualpadding, gap 0, ins 0");
    
    /**
     * Panel that holds the video panel. This is necessary to control whether or
     * not the video fits to screen.
     */
    private final JPanel fitToScreenContainer = new JPanel(fitToScreenLayout);
    

    public VideoPanel(PlayerControlPanel controlPanel, Component videoRenderer,
            HeaderBarDecorator headerBarDecorator, VideoPlayerMediator videoMediator) {

        this.videoRenderer = new JButton("renderer");//videoRenderer;
        
        videoPanel.setBackground(Color.BLACK);
        videoPanel.setFocusable(true);
        
        this.videoMediator = videoMediator;

        setUpHeaderBar(controlPanel, headerBarDecorator);

        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "fullScreen");
        videoPanel.getActionMap().put("fullScreen", new ToggleFullScreen());
        
        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        videoPanel.getActionMap().put("esc", new EscAction());
        
        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "space");
        videoPanel.getActionMap().put("space", new PlayOrPauseAction());
        
        setUpMouseListener(videoPanel);
        setUpMouseListener(fitToScreenContainer);
        setUpMouseListener(this.videoRenderer);

        fitToScreenContainer.setOpaque(false);
        fitToScreenContainer.add(this.videoRenderer);
        setFitToScreen(SwingUiSettings.VIDEO_FIT_TO_SCREEN.getValue());        

        videoPanel.add(headerBar, BorderLayout.NORTH);
        videoPanel.add(fitToScreenContainer, BorderLayout.CENTER);

    }
    
    public Component getComponent(){
        return videoPanel;
    }
 


    private void setUpMouseListener(Component videoComponent) {
        videoComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu menu = new JPopupMenu();
                    if (videoMediator.getStatus() == PlayerState.PLAYING) {
                        menu.add(createPauseMenuItem());
                    } else {
                        menu.add(createPlayMenuItem());
                    }
                    menu.addSeparator();
                    menu.add(createFitToScreenMenuItem());
                    menu.add(createFullScreenMenuItem());
                    menu.addSeparator();
                    menu.add(createCloseItem());
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void setUpHeaderBar(PlayerControlPanel controlPanel,
            HeaderBarDecorator headerBarDecorator) {

        fullScreenButton = new JCheckBox(I18n.tr("Full Screen"));
        fullScreenButton.addItemListener(new FullScreenListener());
        
        videoPanel.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                //This is less than ideal.  We really want this to happen when the VideoPanel is first shown but
                //componentShown() only fires on setVisibility(true) so we are using
                //componentResized() instead since it will resize when VideoPanel is added to its parent.
                fullScreenButton.setSelected(VideoPanel.this.videoMediator.isFullScreen());
            }            
        });
        
        closeButton = new JButton("X");
        closeButton.addActionListener(new CloseAction());

        headerBarDecorator.decorateBasic(headerBar);

        headerBar.setLayout(new MigLayout());
        headerBar.add(fullScreenButton, "push");
        headerBar.add(controlPanel, "pos 0.5al 0.5al");
        headerBar.add(closeButton);

    }
    
    private JMenuItem createFullScreenMenuItem(){
        JMenuItem item = new JCheckBoxMenuItem(I18n.tr("Full Screen"));
        item.setSelected(videoMediator.isFullScreen());
        item.addItemListener(new FullScreenListener());
        return item;
    }

    private JMenuItem createPlayMenuItem() {
        JMenuItem item = new JMenuItem(I18n.tr("Play"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                videoMediator.resume();
            }
        });
        return item;
    }
    

    private JMenuItem createFitToScreenMenuItem() {
        JMenuItem item = new JCheckBoxMenuItem(I18n.tr("Fit to Screen"));
        item.setSelected(SwingUiSettings.VIDEO_FIT_TO_SCREEN.getValue());
        item.addItemListener(new FitToScreenListener());
        return item;
    }
    
    
    private JMenuItem createPauseMenuItem(){
        JMenuItem item = new JMenuItem(I18n.tr("Pause"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                videoMediator.pause();
            }
        });
        return item;
    }
    
    private JMenuItem createCloseItem(){
        JMenuItem item = new JMenuItem(I18n.tr("Close Video"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                videoMediator.closeVideoPanel();
            }
        });
        return item;
    }
    
    private void setFitToScreen(boolean isFitToScreen) {
        SwingUiSettings.VIDEO_FIT_TO_SCREEN.setValue(isFitToScreen);
        videoRenderer.setBackground(Color.BLUE);
        
        if(isFitToScreen){
            fitToScreenLayout.setComponentConstraints(videoRenderer, "grow, gap 0, push");   
            videoRenderer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));   
            videoRenderer.setMinimumSize(new Dimension(0, 0)); 
        } else {
            fitToScreenLayout.setComponentConstraints(videoRenderer, "pos 0.5al 0.5al");
            videoRenderer.setMaximumSize(videoRenderer.getPreferredSize());   
            videoRenderer.setMinimumSize(videoRenderer.getPreferredSize()); 
        }
        fitToScreenContainer.revalidate();
        fitToScreenContainer.setSize(new Dimension(videoPanel.getSize().width, videoPanel.getSize().height - headerBar.getHeight()));
        videoRenderer.setVisible(false);
        videoRenderer.setVisible(true);
    }
    

    private class CloseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            videoMediator.closeVideoPanel();
        }
    }
    
    private class FullScreenListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            videoMediator.setFullScreen(e.getStateChange() == ItemEvent.SELECTED);
        }
    }
    
    private class ToggleFullScreen extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            videoMediator.setFullScreen(!videoMediator.isFullScreen());
        }
    }
    
    private class FitToScreenListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            setFitToScreen(e.getStateChange() == ItemEvent.SELECTED);
        }
    }
    
    
    private class EscAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (videoMediator.isFullScreen()) {
                videoMediator.setFullScreen(false);
            } else {
                videoMediator.closeVideoPanel();
            }
        }
    }
    
    
    private class PlayOrPauseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (videoMediator.getStatus() == PlayerState.PLAYING) {
                videoMediator.pause();
            } else {
                videoMediator.resume();
            }
        }
    }

}
