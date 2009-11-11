package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Class that handles the displaying of video.
 */
class VideoDisplayDirector {
    
    private final JLayeredPane limeWireLayeredPane;   
    
    private final Integer videoLayer = new Integer(JLayeredPane.DEFAULT_LAYER + 1);
    
    private VideoPanel videoPanel;
    
    private JFrame fullScreenFrame;
    private NavigationListener closeVideoOnNavigation;
    
    @Resource(key="WireframeTop.preferredSize") private Dimension topPanelPreferredSize;

    private final VideoPanelFactory videoPanelFactory;

    private final Navigator navigator;
    
    private final Provider<VideoPlayerMediator> videoPlayerMediator;


    @Inject
    public VideoDisplayDirector(@GlobalLayeredPane JLayeredPane limeWireLayeredPane, Provider<VideoPlayerMediator> videoPlayerMediator,
            VideoPanelFactory videoPanelFactory, Navigator navigator){
        this.limeWireLayeredPane = limeWireLayeredPane;
        this.videoPlayerMediator = videoPlayerMediator;
        this.videoPanelFactory = videoPanelFactory;
        this.navigator = navigator;
        
        GuiUtils.assignResources(this);
        assert(topPanelPreferredSize != null);
        
        
        limeWireLayeredPane.addComponentListener(new VideoPanelResizer());
    }
    
    public void show(Component videoRenderer, boolean isFullScreen){
        
        if(this.videoPanel != null){
            close();
        }
        //Recycling the video panel causes problems with native painting.  We need a new one each time.
        videoPanel = videoPanelFactory.createVideoPanel(videoRenderer);
        
        // If the start screen is open, then open up the library view before showing the video.
        // The start screen has a heavy weight component that would cut off the video.
        NavItem selectedNavItem = navigator.getSelectedNavItem();
        if (selectedNavItem.getId().equals(HomeMediator.NAME)) {
            NavItem item = navigator.getNavItem(NavCategory.LIBRARY, LibraryMediator.NAME);
            item.select();
        }
        
        if(isFullScreen){
            showFullScreen();
        } else {
            showInClient();
        }           
    }
    
    private void showInClient(){
        limeWireLayeredPane.add(videoPanel.getComponent(), videoLayer);
        resizeVideoContainer();     
        //Make sure the flash of native video window doesn't steal focus
        GuiUtils.getMainFrame().toFront();   
        registerNavigationListener();
    }   
    
    private void registerNavigationListener() {
        if (closeVideoOnNavigation == null) {
            closeVideoOnNavigation = new CloseVideoOnNavigationListener();
        }

        navigator.addNavigationListener(closeVideoOnNavigation);
    }
    
    
    private void showFullScreen(){
        fullScreenFrame = new LimeJFrame();

        fullScreenFrame.setTitle(GuiUtils.getMainFrame().getTitle());
        // fullScreenFrame.setAlwaysOnTop(true) and
        // SystemUtils.setWindowTopMost(fullScreenFrame) don't play nicely with
        // dialog boxes so we aren't using them here.
       
        fullScreenFrame.setUndecorated(true);
        fullScreenFrame.add(videoPanel.getComponent(), BorderLayout.CENTER);            

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        fullScreenFrame.setBounds(0,0,screenSize.width, screenSize.height);
        
        GuiUtils.getMainFrame().setVisible(false);

        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        
        if (OSUtils.isMacOSX() && device.isFullScreenSupported()) {
            fullScreenFrame.setUndecorated(true);
            device.setFullScreenWindow(fullScreenFrame);
        } else {
            fullScreenFrame.setVisible(true);
            fullScreenFrame.toFront();
        }        
    }


    public boolean isFullScreen() {
        return fullScreenFrame != null;
    }
    
    public void close() {
        if (isFullScreen()) {
            closeFullScreen();
        } else {
            closeInClient();
        }
        videoPanel.dispose();
        videoPanel = null;
        //Force a repaint on close - gets rid of artifacts (especially noticable on Mac)
        GuiUtils.getMainFrame().repaint();
    }    
    
    private void closeFullScreen() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        if (OSUtils.isMacOSX() && device.isFullScreenSupported()) {
            device.setFullScreenWindow(null);
        }
        fullScreenFrame.setVisible(false);
        //manually dispose of the frame so that everything gets garbage collected.
        fullScreenFrame.dispose();
        fullScreenFrame = null;
        GuiUtils.getMainFrame().setVisible(true);
    }

    private void closeInClient(){    
        limeWireLayeredPane.remove(videoPanel.getComponent()); 
        removeNavigationListener();
    }
    
    private void removeNavigationListener(){
        navigator.removeNavigationListener(closeVideoOnNavigation);
    }

    
    private class VideoPanelResizer extends ComponentAdapter {        
        @Override
        public void componentResized(ComponentEvent e) {
            resizeVideoContainer();
        }
    }  
    

    private void resizeVideoContainer() {
        if (videoPanel != null) {
            Rectangle parentBounds = videoPanel.getComponent().getParent().getBounds();
            // TODO: this knows too much about the layered pane's layout
            videoPanel.getComponent().setBounds(0, (int)topPanelPreferredSize.getHeight(), (int)parentBounds.getWidth(), 
                    (int)parentBounds.getHeight() - (int)topPanelPreferredSize.getHeight());
            videoPanel.getComponent().revalidate();
        }
    }
    
    private class CloseVideoOnNavigationListener implements NavigationListener {

        @Override
        public void itemSelected(NavCategory category, NavItem navItem,
                NavSelectable selectable, NavMediator navMediator) {
            videoPlayerMediator.get().closeVideo();                    
        }

        @Override
        public void categoryAdded(NavCategory category) {
            // do nothing
        }

        @Override
        public void categoryRemoved(NavCategory category, boolean wasSelected) {
            // do nothing
        }

        @Override
        public void itemAdded(NavCategory category, NavItem navItem) {
            // do nothing
        }

        @Override
        public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected) {
            // do nothing
        }
    }
}
