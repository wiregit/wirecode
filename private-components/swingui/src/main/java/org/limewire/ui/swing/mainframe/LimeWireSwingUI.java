package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.BoxPanel;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.menu.LimeMenuBar;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LimeWireSwingUI extends JPanel {
    
    private final TopPanel topPanel;
    private final TrayNotifier trayNotifier;
    private final JMenuBar menuBar;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel, LeftPanel leftPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator,
            SearchHandler searchHandler, ChatFramePanel friendsPanel,
            TrayNotifier trayNotifier, AudioPlayer player,
            LimeMenuBar limeMenuBar, DownloadSummaryPanel downloadSummaryPanel) {
    	GuiUtils.assignResources(this);
    	        
    	this.trayNotifier = trayNotifier;
    	this.topPanel = topPanel;
    	this.menuBar = limeMenuBar;
        
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
                
        // The top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        add(topPanel, gbc);
                
        // The left panel
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        add(leftPanel, gbc);
        
        // The main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.addComponentListener(new MainPanelResizer(mainPanel));
        layeredPane.addComponentListener(new PanelResizer(friendsPanel));
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(friendsPanel, JLayeredPane.PALETTE_LAYER);
        PlayerPanel playerPanel = new PlayerPanel(player);
        layeredPane.add(playerPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(playerPanel));
        add(layeredPane, gbc);
                
        JPanel southPanel = new BoxPanel(BoxPanel.Y_AXIS);
        southPanel.add(downloadSummaryPanel);
        southPanel.add(statusPanel);
        
        // The download summary panel and statusbar
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(southPanel, gbc);
    }

    
    public void goHome() {
        topPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
    public void showTrayIcon() {
        trayNotifier.showTrayIcon();
    }
    
    public TrayNotifier getTrayNotifier() {
        return trayNotifier;
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }
    
    private static class MainPanelResizer extends ComponentAdapter {
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
    
    private static class PanelResizer extends ComponentAdapter {
        private final Resizable target;
        
        public PanelResizer(Resizable target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            target.resize();
        }
    }
}