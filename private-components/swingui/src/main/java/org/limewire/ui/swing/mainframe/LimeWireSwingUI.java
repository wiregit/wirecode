package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LimeWireSwingUI extends JPanel {
    
    private final LeftPanel leftPanel;
    private final TopPanel topPanel;
    
    /**
	 * The color of the lines separating the GUI panels
	 */
	@Resource
    private Color lineColor;
    
	@Inject
    public LimeWireSwingUI(TopPanel topPanel, LeftPanel leftPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator, SearchHandler searchHandler,
            FriendsPanel friendsPanel) {
    	GuiUtils.assignResources(this);
    	        
    	this.topPanel = topPanel;
    	this.leftPanel = leftPanel;
        
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        
        // Line above the top area
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;        
        add(new Line(lineColor), gbc);
        
        // The left panel
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        add(leftPanel, gbc);
        
        // The top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        add(topPanel, gbc);
        
        // Line below the top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new Line(lineColor), gbc);
        
        // Line between left & main panel
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        add(new Line(lineColor), gbc);
        
        // The main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.addComponentListener(new MainPanelResizer(mainPanel));
        layeredPane.addComponentListener(new FriendsPanelResizer(friendsPanel));
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(friendsPanel, JLayeredPane.PALETTE_LAYER);
        add(layeredPane, gbc);
        
        // Line below the left & main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new Line(lineColor), gbc);
        
        // The statusbar
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(statusPanel, gbc);
    }
    
    public void goHome() {        
        leftPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
    private class MainPanelResizer extends ComponentAdapter {
        private final MainPanel target;

        public MainPanelResizer(MainPanel target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            Rectangle parentBounds = e.getComponent().getBounds();
            target.setBounds(0, 0, (int)parentBounds.getWidth(), (int)parentBounds.getHeight());
        }
    }
    
    private class FriendsPanelResizer extends ComponentAdapter {
        private final FriendsPanel target;
        
        public FriendsPanelResizer(FriendsPanel target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            target.resize();
        }
    }
}
