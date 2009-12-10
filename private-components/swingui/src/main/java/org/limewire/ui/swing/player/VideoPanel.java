package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
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
import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.painter.factories.ButtonPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingHacks;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Panel that holds video and video controls.
 */
class VideoPanel implements Disposable{

    private final HeaderBar headerBar = new HeaderBar();
    
    private final JXCollapsiblePane headerBarCollapsiblePane = new JXCollapsiblePane();
    private ActionListener collapsingActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            headerBarCollapsiblePane.setCollapsed(true);
        }
    };
    private Timer collapsePlayerControlsTimer = new Timer(2000, collapsingActionListener);

    @Resource private Icon fullScreenSelected;
    @Resource private Icon fullScreenUnselected;

    @Resource private Icon close;

    private final VideoPlayerMediator videoMediator;
    
    private final Container videoRenderer;
    
    /**
     * the panel containing video and controls.
     */
    private final JPanel videoPanel = new JPanel(new BorderLayout());
    
    private final PlayerControlPanel controlPanel;
    

    private final MigLayout fitToScreenLayout = new MigLayout("align 50% 50%, novisualpadding, gap 0, ins 0");
    
    /**
     * Panel that holds the video panel. This is necessary to control whether or
     * not the video fits to screen.
     */
    private final JPanel fitToScreenContainer = new JPanel(fitToScreenLayout);    

    @Inject
    public VideoPanel(@Assisted Container videoRenderer, PlayerControlPanelFactory controlPanelFactory,
            HeaderBarDecorator headerBarDecorator, VideoPlayerMediator videoMediator, 
            ButtonPainterFactory buttonPainterFactory) {

        this.videoRenderer = videoRenderer;        
        this.videoMediator = videoMediator;
        
        GuiUtils.assignResources(this);

        controlPanel = controlPanelFactory.createVideoControlPanel();
        setUpHeaderBar(controlPanel, headerBarDecorator, buttonPainterFactory);

        setupActionMaps();
        
        videoPanel.setBackground(Color.BLACK);        
        this.videoRenderer.setBackground(Color.BLACK);
        fitToScreenContainer.setBackground(Color.BLACK);
        
        setUpMouseListener(videoPanel);
        setUpMouseListener(fitToScreenContainer);
        setUpMouseListener(videoRenderer);
        videoRenderer.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                setUpMouseListener(e.getChild());
                //ensure everything is sized properly and video is embedded
                setFitToScreen(SwingUiSettings.VIDEO_FIT_TO_SCREEN.getValue()); 
            }
        });

        fitToScreenContainer.add(this.videoRenderer);
        setFitToScreen(SwingUiSettings.VIDEO_FIT_TO_SCREEN.getValue());        

        collapsePlayerControlsTimer.setRepeats(false);

        headerBarCollapsiblePane.setContentPane(headerBar);
        videoPanel.add(headerBarCollapsiblePane, BorderLayout.NORTH);
        videoPanel.add(fitToScreenContainer, BorderLayout.CENTER);
    }
    
    public JComponent getComponent(){
        return videoPanel;
    }
    
    public void dispose() {
        collapsePlayerControlsTimer.removeActionListener(collapsingActionListener);
        collapsingActionListener = null;
        collapsePlayerControlsTimer = null;
        controlPanel.dispose();
    }

    public void requestFocus() {
        videoPanel.requestFocus();
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
        VideoPanelMouseListener listener = new VideoPanelMouseListener();
        videoComponent.addMouseListener(listener);
        videoComponent.addMouseMotionListener(listener);
    }

    private void setUpHeaderBar(JComponent controlPanel,
        HeaderBarDecorator headerBarDecorator, ButtonPainterFactory buttonPainterFactory) { 
        
        final JXButton fullScreenButton = new JXButton(fullScreenUnselected);
        fullScreenButton.setContentAreaFilled(false);
        fullScreenButton.setFocusPainted(false);
        fullScreenButton.setBackgroundPainter(buttonPainterFactory.createDarkFullButtonBackgroundPainter(DrawMode.FULLY_ROUNDED, AccentType.SHADOW));
        fullScreenButton.addActionListener(new FullScreenListener());
       
        final JXButton closeButton = new JXButton(close);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setBackgroundPainter(buttonPainterFactory.createDarkFullButtonBackgroundPainter(DrawMode.FULLY_ROUNDED, AccentType.SHADOW));
        closeButton.addActionListener(new CloseAction());

        headerBarDecorator.decorateBasic(headerBar);

        headerBar.setLayout(new MigLayout());
        headerBar.add(fullScreenButton, "right, push");
        headerBar.add(controlPanel, "pos 0.5al 0.5al");
        headerBar.add(closeButton);

        videoPanel.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                if (videoMediator.isFullScreen()) {
                    fullScreenButton.setIcon(fullScreenSelected);
                    headerBar.setFocusable(true);
                    headerBar.requestFocusInWindow();
                    setFitToScreen(true);
                    collapsePlayerControlsTimer.restart();
                } else {
                    fullScreenButton.setIcon(fullScreenUnselected);
                    setFitToScreen(SwingUiSettings.VIDEO_FIT_TO_SCREEN.getValue());
                }
                videoPanel.removeAncestorListener(this);
            }
            
            @Override
            public void ancestorRemoved(AncestorEvent event) {}
            
            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });
    }
    
    private JMenuItem createFullScreenMenuItem(){
        JMenuItem item = new JCheckBoxMenuItem(I18n.tr("Full Screen"));
        item.setSelected(videoMediator.isFullScreen());
        item.addActionListener(new FullScreenListener());
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
        //TODO: Remove isWin7 check.  At the moment win7 video only works with fit to screen selected.
        if(OSUtils.isWindows7() || isFitToScreen){
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
                videoMediator.closeVideo();
            }
        });
        return item;
    }
    
    private class CloseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            videoMediator.closeVideo();
        }
    }
    
    private class FullScreenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            videoMediator.setFullScreen(!videoMediator.isFullScreen());
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
            //save the Fit_To_Screen state in settings.
            SwingUiSettings.VIDEO_FIT_TO_SCREEN.setValue(e.getStateChange() == ItemEvent.SELECTED);
        }
    }
    
    private class EscAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (videoMediator.isFullScreen()) {
                videoMediator.setFullScreen(false);
            } else {
                videoMediator.closeVideo();
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

    private class VideoPanelMouseListener extends MouseAdapter {
        private int mouseX;
        private int mouseY;
        
        @Override
        public void mouseMoved(MouseEvent e) {
            /*
             * On Windows the resizing control panel is causing mouse moved events to occur even when the
             * mouse is not really moving. (It's only moving relative to the growing video panel.)
             * So, let's try to separate this sort of motion from real user input by checking whether
             * the mouse has actually moved with regard to its location on the screen.
             */
            if (videoMediator.isFullScreen() && (e.getLocationOnScreen().x != mouseX || e.getLocationOnScreen().y != mouseY)) {
                headerBarCollapsiblePane.setCollapsed(false);
                collapsePlayerControlsTimer.restart();
                mouseX = e.getLocationOnScreen().x;
                mouseY = e.getLocationOnScreen().y;
            }
        }
        
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
                if(!videoMediator.isFullScreen()) {
                    menu.add(createFitToScreenMenuItem());
                }
                menu.add(createFullScreenMenuItem());
                menu.addSeparator();
                menu.add(createCloseItem());
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
