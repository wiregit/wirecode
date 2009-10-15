package org.limewire.ui.swing.player;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.SystemUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Class that handles the displaying of video.
 */
class VideoDisplayDirector {
    
    private final JLayeredPane limeWireLayeredPane;   
    
    private JComponent videoPanel;
    
    private JFrame fullScreenFrame;
    
    @Resource(key="WireframeTop.preferredSize") private Dimension topPanelPreferredSize;

    private final VideoPanelFactory videoPanelFactory;


    @Inject
    public VideoDisplayDirector(@GlobalLayeredPane JLayeredPane limeWireLayeredPane, Provider<VideoPlayerMediator> videoPlayerMediator,
            VideoPanelFactory videoPanelFactory){
        this.limeWireLayeredPane = limeWireLayeredPane;
        this.videoPanelFactory = videoPanelFactory;
        
        GuiUtils.assignResources(this);
        
        
        limeWireLayeredPane.addComponentListener(new VideoPanelResizer());
    }
    
    public void show(Component videoRenderer, boolean isFullScreen){
        
        if(this.videoPanel != null){
            close();
        }
        
        videoPanel = videoPanelFactory.createVideoPanel(videoRenderer).getComponent();
        
        if(isFullScreen){
            showFullScreen();
        } else {
            showInClient();
        }           
    }
    
    private void showInClient(){
        limeWireLayeredPane.add(videoPanel, JLayeredPane.DEFAULT_LAYER);
        limeWireLayeredPane.moveToFront(videoPanel);
        resizeVideoContainer();     
        //Make sure the flash of native video window doesn't steal focus
        GuiUtils.getMainFrame().toFront();   
    }
    
    private void showFullScreen(){
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
            closeInClient();
        }
        videoPanel = null;
    }
    
    private void closeFullScreen() {
        fullScreenFrame.setVisible(false);
        fullScreenFrame = null;
        GuiUtils.getMainFrame().setVisible(true);
    }

    private void closeInClient(){    
        //moving to back before removing ensures everything looks right
        limeWireLayeredPane.moveToBack(videoPanel); 
        limeWireLayeredPane.remove(videoPanel); 
    }

    
    private class VideoPanelResizer extends ComponentAdapter {        
        @Override
        public void componentResized(ComponentEvent e) {
            resizeVideoContainer();
        }
    }  
    

    private void resizeVideoContainer() {
        if (videoPanel != null) {
            Rectangle parentBounds = videoPanel.getParent().getBounds();
            // TODO: this knows too much about the layered pane's layout
            videoPanel.setBounds(0, (int)topPanelPreferredSize.getHeight(), (int)parentBounds.getWidth(), 
                    (int)parentBounds.getHeight() - (int)topPanelPreferredSize.getHeight());
            videoPanel.revalidate();
        }
    }
}
