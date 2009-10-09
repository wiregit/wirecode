package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.SystemUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

class VideoDisplayDirector {
    
    private final JLayeredPane limeWireLayeredPane;
    
    private final JPanel videoPanelContainer = new JPanel(new BorderLayout());    

    private final Provider<VideoPlayerMediator> videoPlayerMediator;
    
    private final PlayerControlPanelFactory controlPanelFactory;
    
    private final HeaderBarDecorator headerBarDecorator;
    
    private VideoPanel videoPanel;
    
    private JFrame fullScreenFrame;
    
    @Resource(key="WireframeTop.preferredSize") private Dimension topPanelPreferredSize;


    @Inject
    public VideoDisplayDirector(@GlobalLayeredPane JLayeredPane limeWireLayeredPane, Provider<VideoPlayerMediator> videoPlayerMediator,
            HeaderBarDecorator headerBarDecorator, PlayerControlPanelFactory controlPanelFactory){
        this.limeWireLayeredPane = limeWireLayeredPane;
        this.videoPlayerMediator = videoPlayerMediator;
        this.headerBarDecorator = headerBarDecorator;
        this.controlPanelFactory = controlPanelFactory;
        
        GuiUtils.assignResources(this);
        
        limeWireLayeredPane.add(videoPanelContainer, JLayeredPane.DEFAULT_LAYER);
        limeWireLayeredPane.moveToBack(videoPanelContainer);
        limeWireLayeredPane.addComponentListener(new VideoPanelResizer());
    }
    
    public void show(Component videoRenderer, boolean isFullScreen){
        videoPanel = new VideoPanel(controlPanelFactory.createVideoControlPanel(), videoRenderer,
                headerBarDecorator, videoPlayerMediator.get());
        if(isFullScreen){
            showFullScreen(videoPanel.getComponent());
        } else {
            showInClient(videoPanel.getComponent());
        }           
    }
    
    private void showInClient(Component videoPanel){
        if (isFullScreen()){
            closeFullScreen();
        }
        
        videoPanelContainer.add(videoPanel);  
        limeWireLayeredPane.moveToFront(videoPanelContainer);
        resizeVideoContainer();     
        //Make sure the flash of native video window doesn't steal focus
        GuiUtils.getMainFrame().toFront();   
    }
    
    private void showFullScreen(Component videoPanel){
        closeClientVideo();
        GuiUtils.getMainFrame().setVisible(false);
        
        fullScreenFrame = new LimeJFrame();
        fullScreenFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fullScreenFrame.setAlwaysOnTop(true);
        SystemUtils.setWindowTopMost(fullScreenFrame);
        fullScreenFrame.setUndecorated(true);
        fullScreenFrame.add(videoPanel, BorderLayout.CENTER);            

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        fullScreenFrame.setBounds(0,0,screenSize.width, screenSize.height);
        
        GuiUtils.getMainFrame().setVisible(false);
        
        fullScreenFrame.setVisible(true);
        fullScreenFrame.toFront();
    }


    public boolean isFullScreen() {
        return fullScreenFrame != null;
    }
    
    public void close() {
        if (isFullScreen()) {
            closeFullScreen();
        } else {
            closeClientVideo();
        }
    }
    
    private void closeFullScreen() {
        fullScreenFrame.setVisible(false);
        fullScreenFrame = null;
        GuiUtils.getMainFrame().setVisible(true);
    }

    private void closeClientVideo(){
        videoPanelContainer.removeAll(); 
        limeWireLayeredPane.moveToBack(videoPanelContainer); 
    }

    
    private class VideoPanelResizer extends ComponentAdapter {
        
        @Override
        public void componentResized(ComponentEvent e) {
            resizeVideoContainer();
        }
    }  
    

    private void resizeVideoContainer() {
        Rectangle parentBounds = videoPanelContainer.getParent().getBounds();
        //TODO: this knows too much about the layered pane's layout
        videoPanelContainer.setBounds(0, (int)topPanelPreferredSize.getHeight(), 
                (int)parentBounds.getWidth(), (int)parentBounds.getHeight()-(int)topPanelPreferredSize.getHeight());
        videoPanelContainer.revalidate();
    }
}
