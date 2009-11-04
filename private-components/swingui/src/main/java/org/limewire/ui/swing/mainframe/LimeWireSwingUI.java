package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.limewire.core.api.Application;
import org.limewire.core.api.updates.UpdateEvent;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeSplitPane;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.downloads.DownloadVisibilityListener;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.DownloadVisibilityEvent;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.mainframe.BottomPanel.TabId;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.pro.ProNagController;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.statusbar.SharedFileCountPopupPanel;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.update.UpdatePanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LimeWireSwingUI extends JPanel {
    
    private final JPanel centerPanel;
    private final TopPanel topPanel;
    private final JLayeredPane layeredPane;
    private final ProNagController proNagController;
    private final LimeSplitPane splitPane;
    private final Provider<SignOnMessageLayer> signOnMessageProvider;
    private final MainDownloadPanel mainDownloadPanel;
    private final BottomHeaderPanel bottomHeaderPanel;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator,
            SearchHandler searchHandler,
            ProNagController proNagController, 
            SharedFileCountPopupPanel sharedFileCountPopup,
            LoginPopupPanel loginPopup,
            Provider<SignOnMessageLayer> signOnMessageProvider,
            MainDownloadPanel mainDownloadPanel,
            @GlobalLayeredPane JLayeredPane limeWireLayeredPane,
            BottomPanel bottomPanel,
            BottomHeaderFactory bottomHeaderFactory) {
    	GuiUtils.assignResources(this);
    	
    	this.topPanel = topPanel;  	
    	this.layeredPane = limeWireLayeredPane;
    	this.proNagController = proNagController;
    	this.signOnMessageProvider = signOnMessageProvider;
        this.centerPanel = new JPanel(new GridBagLayout());   
        this.mainDownloadPanel = mainDownloadPanel;
        
        // Create bottom header panel.
        bottomHeaderPanel = bottomHeaderFactory.create(bottomPanel);
    	
        // Create split pane for bottom tray.
    	splitPane = createSplitPane(mainPanel, bottomPanel, 
    	        bottomHeaderPanel.getComponent(), bottomHeaderPanel.getDragComponent());

        setLayout(new BorderLayout());

        GridBagConstraints gbc = new GridBagConstraints();
                
        // The top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        centerPanel.add(topPanel, gbc);
        
        // The main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        centerPanel.add(splitPane, gbc);
        
        layeredPane.addComponentListener(new MainPanelResizer(centerPanel));
        layeredPane.add(centerPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(sharedFileCountPopup, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(sharedFileCountPopup));
        layeredPane.add(loginPopup, JLayeredPane.POPUP_LAYER);
        layeredPane.addComponentListener(new PanelResizer(loginPopup));
        add(layeredPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
    }
	
	@Inject
	public void registerListener(){
	    mainDownloadPanel.addDownloadVisibilityListener(new DownloadVisibilityHandler());
	    
	    // Add listener for Uploads setting.
	    UploadSettings.SHOW_UPLOADS_TRAY.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        handleUploadVisibilityChange(UploadSettings.SHOW_UPLOADS_TRAY.getValue());
                    }
                });
            }
	    });
	}
	
	private boolean isFirstPainting = true;
	@Override
    public void paint(Graphics g){
	    if(isFirstPainting && splitPane.getHeight() > 0){
	        isFirstPainting = false;
            if (UploadSettings.SHOW_UPLOADS_TRAY.getValue()) {
                handleUploadVisibilityChange(true);
            }
	        if (DownloadSettings.SHOW_DOWNLOADS_TRAY.getValue()) {
	            handleDownloadVisibilityChange(true);
	        }
	    }
	    super.paint(g);
	}
	
	void hideMainPanel() {
	    layeredPane.setVisible(false);
        centerPanel.setVisible(false);
    }
	
	void showMainPanel() {
        layeredPane.setVisible(true);
        centerPanel.setVisible(true);
    }
	
	void loadProNag() {
	    proNagController.allowProNag(layeredPane);
	}
	
	public void goHome() {
        topPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
   private LimeSplitPane createSplitPane(final JComponent top, final BottomPanel bottom, 
           JComponent divider, JComponent dragComponent) {
        final LimeSplitPane splitPane = new LimeSplitPane(JSplitPane.VERTICAL_SPLIT, true, top, bottom, divider);
        splitPane.setDividerSize(0);
        bottom.setVisible(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // Allow bottom panel to be minimized
        bottom.setMinimumSize(new Dimension(0, 0));
        
        // The bottom panel remains the same size when the splitpane is resized
        splitPane.setResizeWeight(1);
        
        // Move dragability from the entire divider to a single component
        splitPane.setDividerDraggable(false);
        splitPane.setDragComponent(dragComponent);
        
        //set top panel's minimum height to half of split pane height 
        //(this fires when the app is initialized)
        splitPane.addComponentListener(new ComponentAdapter(){            
            @Override
            public void componentResized(ComponentEvent e) {
                top.setMinimumSize(new Dimension(0, splitPane.getHeight()/2));
            }
        });
        
        // Add listener to save bottom tray size.
        bottom.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                int height = bottom.getHeight();
                int minHeight = bottom.getDefaultPreferredHeight(); 
                if (height > minHeight) {
                    SwingUiSettings.BOTTOM_TRAY_SIZE.setValue(height);
                } else {
                    SwingUiSettings.BOTTOM_TRAY_SIZE.setValue(minHeight);
                }
            }
        });

        return splitPane;
    }

    private class DownloadVisibilityHandler implements DownloadVisibilityListener {
        @Override
        public void updateVisibility(DownloadVisibilityEvent event) {
            handleDownloadVisibilityChange(event.getVisibility());
        }
    }
   
    /**
     * Handles change in visible state of Downloads tray.
     */
    private void handleDownloadVisibilityChange(boolean visible) {
        boolean uploadVisible = UploadSettings.SHOW_UPLOADS_TRAY.getValue();
        
        if (visible) {
            bottomHeaderPanel.selectTab(TabId.DOWNLOADS);
        } else if (uploadVisible) {
            bottomHeaderPanel.selectTab(TabId.UPLOADS);
        }
        
        setBottomTrayVisible(visible || uploadVisible);
    }
    
    /**
     * Handles change in visible state of Uploads tray.
     */
    private void handleUploadVisibilityChange(boolean visible) {
        boolean downloadVisible = DownloadSettings.SHOW_DOWNLOADS_TRAY.getValue();
        
        if (visible) {
            bottomHeaderPanel.selectTab(TabId.UPLOADS);
        } else if (downloadVisible) {
            bottomHeaderPanel.selectTab(TabId.DOWNLOADS);
        }
        
        setBottomTrayVisible(visible || downloadVisible);
    }
    
    /**
     * Sets the visibility of the downloads/uploads tray.
     */
    private void setBottomTrayVisible(boolean visible) {
        assert (SwingUtilities.isEventDispatchThread());
        
        // Set component visibility.
        boolean wasVisible = splitPane.getBottomComponent().isVisible();
        splitPane.getBottomComponent().setVisible(visible);
        
        if (visible) {
            // Restore divider size.
            splitPane.setDividerSize(bottomHeaderPanel.getComponent().getPreferredSize().height);
            
            // Restore divider location if newly visible.  If the last location
            // is not valid, compute perferred position and apply.
            if (!wasVisible) {
                int lastLocation = splitPane.getLastDividerLocation(); 
                if ((lastLocation <= 0) || (lastLocation > splitPane.getHeight())) {
                    int preferredDividerPosition = splitPane.getSize().height -
                        splitPane.getInsets().bottom - splitPane.getDividerSize() -
                        splitPane.getBottomComponent().getPreferredSize().height;
                    if (preferredDividerPosition < (splitPane.getHeight() / 2)) {
                        preferredDividerPosition = splitPane.getHeight() / 2;
                    }
                    splitPane.setDividerLocation(preferredDividerPosition);
                } else {
                    splitPane.setDividerLocation(lastLocation);
                }
            }
            
        } else {
            // Save divider location and reset divider size.
            splitPane.setLastDividerLocation(splitPane.getDividerLocation());
            splitPane.setDividerSize(0);
        }
    }
    
    private static class MainPanelResizer extends ComponentAdapter {
        private final JComponent target;

        public MainPanelResizer(JComponent target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            Rectangle parentBounds = e.getComponent().getBounds();
            target.setBounds(0, 0, (int)parentBounds.getWidth(), (int)parentBounds.getHeight());
        }
    }
    
    /**
     * Listens for Update events and display a dialog if a update exists.
     * @param updateEvent
     */
    @Inject void register(ListenerSupport<UpdateEvent> updateEvent,
            ListenerSupport<FriendConnectionEvent> connectionSupport,
            final Application application) {
        updateEvent.addListener(new EventListener<UpdateEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(UpdateEvent event) {
                UpdatePanel updatePanel = new UpdatePanel(event.getData(), application);
                JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("New Version Available!"), null, updatePanel);
                dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
                dialog.getRootPane().setDefaultButton(updatePanel.getDefaultButton());
                dialog.setModal(false);
                dialog.pack();
                dialog.setVisible(true);
            }
        });
        
        // Add listener to display sign-on message if enabled.
        if (SignOnMessageLayer.isSignOnMessageEnabled()) {
            connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(FriendConnectionEvent event) {
                    if (event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                        signOnMessageProvider.get().showMessage();
                    }
                }
            });
        }
    }
}