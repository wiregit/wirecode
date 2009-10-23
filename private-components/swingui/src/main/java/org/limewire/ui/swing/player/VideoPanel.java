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
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.JToggleButton.ToggleButtonModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.painter.factories.ButtonPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingHacks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Panel that holds video and video controls.
 */
class VideoPanel {

    private final HeaderBar headerBar = new HeaderBar();

    private JXButton fullScreenButton;
    @Resource private Icon fullScreenSelected;
    @Resource private Icon fullScreenUnselected;

    private JXButton closeButton;
    @Resource private Icon close;

    private final VideoPlayerMediator videoMediator;
    
    private Component videoRenderer;
    
    /**
     * the panel containing video and controls.
     */
    private JPanel videoPanel = new JPanel(new BorderLayout());
    

    private final MigLayout fitToScreenLayout = new MigLayout("align 50% 50%, novisualpadding, gap 0, ins 0");
    
    /**
     * Panel that holds the video panel. This is necessary to control whether or
     * not the video fits to screen.
     */
    private final JPanel fitToScreenContainer = new JPanel(fitToScreenLayout);    

    @Inject
    public VideoPanel(@Assisted Component videoRenderer, PlayerControlPanelFactory controlPanelFactory,
            HeaderBarDecorator headerBarDecorator, VideoPlayerMediator videoMediator, 
            ButtonPainterFactory buttonPainterFactory) {

        this.videoRenderer = videoRenderer;        
        this.videoMediator = videoMediator;
        
        GuiUtils.assignResources(this);

        setUpHeaderBar(controlPanelFactory.createVideoControlPanel(), headerBarDecorator, buttonPainterFactory);

        setupActionMaps();
        
        videoPanel.setBackground(Color.BLACK);        
        this.videoRenderer.setBackground(Color.BLACK);
        fitToScreenContainer.setBackground(Color.BLACK);
        
        setUpMouseListener(videoPanel);
        setUpMouseListener(fitToScreenContainer);
        setUpMouseListener(this.videoRenderer);

        fitToScreenContainer.add(this.videoRenderer);
        setFitToScreen(SwingUiSettings.VIDEO_FIT_TO_SCREEN.getValue());        

        videoPanel.add(headerBar, BorderLayout.NORTH);
        videoPanel.add(fitToScreenContainer, BorderLayout.CENTER);

    }
    
    public JComponent getComponent(){
        return videoPanel;
    }
 

    private void setupActionMaps(){
        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "fullScreen");
        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullScreen");
        videoPanel.getActionMap().put("fullScreen", new ToggleFullScreen());
        
        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        videoPanel.getActionMap().put("esc", new EscAction());
        
        videoPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "space");
        videoPanel.getActionMap().put("space", new PlayOrPauseAction());
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
                    SwingHacks.fixPopupMenuForWindows(menu);
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

    private void setUpHeaderBar(JComponent controlPanel,
            HeaderBarDecorator headerBarDecorator, ButtonPainterFactory buttonPainterFactory) { 
        
        fullScreenButton = new JXButton(fullScreenUnselected);
        fullScreenButton.setSelectedIcon(fullScreenSelected);
        fullScreenButton.setBackgroundPainter(buttonPainterFactory.createDarkFullButtonBackgroundPainter(DrawMode.FULLY_ROUNDED, AccentType.SHADOW));
        fullScreenButton.setModel(new ToggleButtonModel());
        fullScreenButton.addItemListener(new FullScreenListener());
        
        videoPanel.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                //This is less than ideal.  We really want this to happen when the VideoPanel is first shown but
                //componentShown() only fires on setVisibility(true) so we are using
                //componentResized() instead since it will resize when it is first shown.
                fullScreenButton.setSelected(VideoPanel.this.videoMediator.isFullScreen());
            }            
        });
        
        closeButton = new JXButton(close);
        closeButton.setBackgroundPainter(buttonPainterFactory.createDarkFullButtonBackgroundPainter(DrawMode.FULLY_ROUNDED, AccentType.SHADOW));
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
    
    private void setFitToScreen(boolean isFitToScreen) {
        SwingUiSettings.VIDEO_FIT_TO_SCREEN.setValue(isFitToScreen);
        
        if(isFitToScreen){
            fitToScreenLayout.setComponentConstraints(videoRenderer, "grow, push");   
            videoRenderer.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));   
            videoRenderer.setMinimumSize(new Dimension(0, 0)); 
        } else {
            fitToScreenLayout.setComponentConstraints(videoRenderer, "");
            videoRenderer.setMaximumSize(videoRenderer.getPreferredSize());   
            videoRenderer.setMinimumSize(new Dimension(0, 0)); 
        }
        fitToScreenContainer.revalidate();
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
