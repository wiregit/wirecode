package org.limewire.ui.swing.library.nav;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TooManyListenersException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendRequestEvent;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.impl.PresenceEvent;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryNavTransferHandler;
import org.limewire.ui.swing.friends.FriendRequestPanel;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.Catalog;
import org.limewire.ui.swing.library.FriendLibraryPanel;
import org.limewire.ui.swing.library.ListSourceChanger;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ca.odell.glazedlists.EventList;
import net.miginfocom.swing.MigLayout;

@Singleton
class LibraryNavigatorImpl extends JXPanel implements LibraryNavigator {
    
    private static final Log LOG = LogFactory.getLog(LibraryNavigatorImpl.class);
    
    private final NavPanel myLibrary;
    private final NavPanel p2pNetwork;
    private final NavPanel allFriends;
    private final NavList limewireList;
    private final NavList onlineList;
    private final NavList offlineList;
    private final NavList[] allLists;
    
    private final MyLibraryPanel myLibraryPanel;
    private final FriendLibraryPanel friendLibraryPanel;
    
    private final ShareListManager shareListManager;
    private final NavPanelFactory navPanelFactory;
    private final JScrollPane friendsScrollArea;
    private final FriendRequestPanel friendRequestPanel;
    private final GhostDragGlassPane ghostPane;

    private Catalog activeCatalog;
    
    private Friend selectedFriend = null;
    
    /** NavItem for the FriendLibraryPanel */
    private NavItem friendNavItem;
    
    /** map lookup key for All Friend Action */
    private static final String ALL_FRIEND_ACTION = "ALL_FRIEND_ACTION";
    
    /** Map of friends to their action */
    private Map<String, Action> friendActionMap = new HashMap<String, Action>();

    @Inject
    LibraryNavigatorImpl(Navigator navigator,
            RemoteLibraryManager remoteLibraryManager,
            DownloadListManager downloadListManager,
            ShareListManager shareListManager,
            MyLibraryPanel myLibraryPanel,
            FriendLibraryPanel friendLibraryPanel,
            NavPanelFactory navPanelFactory,
            final FriendsSignInPanel friendsPanel,
            SaveLocationExceptionHandler saveLocationExceptionHandler,
            FriendRequestPanel friendRequestPanel,
            GhostDragGlassPane ghostPane) {
        
        this.friendRequestPanel = friendRequestPanel;
        this.myLibraryPanel = myLibraryPanel;
        this.friendLibraryPanel = friendLibraryPanel;
        this.shareListManager = shareListManager;
        this.limewireList = new NavList("LibraryNavigator.limewireList", null);
        this.onlineList = new NavList("LibraryNavigator.onlineList", SwingUiSettings.ONLINE_COLLAPSED);
        this.offlineList = new OfflineNavList("LibraryNavigator.offlineList", SwingUiSettings.OFFLINE_COLLAPSED);
        this.allLists = new NavList[] { limewireList, onlineList, offlineList };
        this.navPanelFactory = navPanelFactory;
        this.ghostPane = ghostPane;
        
        limewireList.setTitleText(I18n.tr("On LimeWire"));
        onlineList.setTitleText(I18n.tr("Online"));
        offlineList.setTitleText(I18n.tr("Offline"));
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        
        String libraryTitle = I18n.tr("My Library");
        NavItem libraryNavItem = navigator.createNavItem(NavCategory.LIBRARY, libraryTitle, myLibraryPanel);
        myLibrary = initializePanel(libraryTitle,  createMyLibraryAction(libraryNavItem), "LibraryNavigator.myLibrary");
        myLibrary.updateLibraryState(myLibraryPanel.getLibrary().getState());
        myLibrary.setTransferHandler(new MyLibraryNavTransferHandler(downloadListManager, myLibraryPanel.getLibrary(), saveLocationExceptionHandler));
        try {
            myLibrary.getDropTarget().addDropTargetListener(new GhostDropTargetListener(myLibrary,ghostPane));
        } catch (TooManyListenersException ignoreException) {            
        }   
        
        
        myLibraryPanel.getLibrary().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("state")) {
                    myLibrary.updateLibraryState((LibraryState)evt.getNewValue());
                }
            }
        });
        
        p2pNetwork = initializePanel(I18n.tr("P2P Network"), createP2PAction(libraryNavItem), "LibraryNavigator.p2pNetwork"); 
        p2pNetwork.setTransferHandler(new LocalFileListTransferHandler(shareListManager.getGnutellaShareList()));
        try {
            p2pNetwork.getDropTarget().addDropTargetListener(new GhostDropTargetListener(p2pNetwork,ghostPane, new FriendAdapter(I18n.tr("the P2P Network"))));
        } catch (TooManyListenersException ignoreException) {            
        }  
        
        String friendTitle = I18n.tr("All Friends");
        friendNavItem = navigator.createNavItem(NavCategory.LIBRARY, friendTitle, friendLibraryPanel);
        allFriends = initializePanel(friendTitle, createAllFriendAction(friendNavItem), "LibraryNavigator.allFriends");
        allFriends.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendsPanel.signIn();
            }
        });
        addFriendListeners();
        
        setLayout(new MigLayout("insets 0, fill, gap 0"));

        
        JXPanel friendsListPanel = new JXPanel(new VerticalLayout(2));
        friendsListPanel.setOpaque(false);
        friendsListPanel.setScrollableTracksViewportHeight(false);
        friendsScrollArea = new JScrollPane(friendsListPanel);
        friendsScrollArea.setName("LibraryNavigator.friendsScrollArea");
        friendsScrollArea.setOpaque(false);
        friendsScrollArea.getViewport().setOpaque(false);
        friendsScrollArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        friendsScrollArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        friendsScrollArea.setBorder(BorderFactory.createEmptyBorder());
        
        // Increase the painted gaps a bit to make sure it's not smushed.
        myLibrary.setTopGap(2);
        myLibrary.setBottomGap(2);
        allFriends.setTopGap(2);
        allFriends.setBottomGap(2);
        
        addItem(myLibrary, this, "growx, wmin 0, wrap", null, p2pNetwork.getAction());
        addItem(p2pNetwork, this, "growx, wmin 0, wrap", myLibrary.getAction(), allFriends.getAction());
        addItem(allFriends, this, "growx, wmin 0, wrap", p2pNetwork.getAction(), new MoveAction(limewireList, true));
        addItem(friendsPanel, this, "growx, wmin 0, wrap", null, null);
        addItem(friendRequestPanel, this, "growx, wmin 0, wrap, gaptop 2, hidemode 3", null, null);
        addItem(friendsScrollArea, this, "grow, wmin 0, wrap",  null, null);
        addItem(limewireList, friendsListPanel, "", allFriends.getAction(), new MoveAction(onlineList, true));
        addItem(onlineList,  friendsListPanel, "", new MoveAction(limewireList, false), new MoveAction(offlineList, true));
        addItem(offlineList, friendsListPanel, "", new MoveAction(onlineList, false), null);

        new FriendLibraryUpdater().install(remoteLibraryManager.getSwingFriendLibraryList());
    }
    
    private void addItem(JComponent item, JComponent parent, String constraints, Action upAction, Action downAction) {
        parent.add(item, constraints);
        if(upAction != null) {
            item.getActionMap().put(NavKeys.MOVE_UP, upAction);
        }
        if(downAction != null) {
            item.getActionMap().put(NavKeys.MOVE_DOWN, downAction);
        }
    }    
    
    private Action createMyLibraryAction(final NavItem item) {
        // When "My Library" is clicked, we want to remove any filter.
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                item.select();
                LibraryNavigatorImpl.this.myLibraryPanel.showAllFiles();
            }
        };
        // When the NavItem becomes selected, we only want to select
        // this action if P2P Network IS NOT the current filter.
        item.addNavItemListener(new NavItemListener() {
            public void itemRemoved() {
            }

            public void itemSelected(boolean selected) {
                Friend currentFriend = myLibraryPanel.getCurrentFriend();
                boolean p2pSelected = currentFriend != null && currentFriend.getId().equals(Friend.P2P_FRIEND_ID);
                action.putValue(Action.SELECTED_KEY, selected && !p2pSelected);
            }
        });

        action.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        item.select();
                    }
                }
            }
        });        
        action.putValue(NavigatorUtils.NAV_ITEM, item);
        
        // Add a listener for changing friends, so that we can select the action
        // if the item is selected.
        myLibraryPanel.addFriendListener(new ListSourceChanger.ListChangedListener() {
            @Override
            public void friendChanged(Friend currentFriend) {
                boolean p2pSelected = currentFriend != null && currentFriend.getId().equals(Friend.P2P_FRIEND_ID);
                action.putValue(Action.SELECTED_KEY, item.isSelected() && !p2pSelected);
            }
        });
        
        return action;
    }
    
    /**
	 * Creates action to select All Friend Library. This handles the selection when the
     * user mouse clicks on this friend. This friend can also be selected through
     * navigation. This is handled in addFriendListeners()
     */
    private Action createAllFriendAction(final NavItem friendLibraryItem) {
        // When "All Friend" is clicked, we want to remove any filter.
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibraryNavigatorImpl.this.friendLibraryPanel.setFriend(null);
                friendLibraryItem.select();
            }
        };   

		// if this is selected, display the friend nav item
        action.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        friendLibraryItem.select();
                    }
                }
            }
        });        
        action.putValue(NavigatorUtils.NAV_ITEM, friendLibraryItem);
        
        friendActionMap.put(ALL_FRIEND_ACTION, action);
        
        return action;
    }

    /**
     * Listens for changes in navigator and friend selection. If the FriendLibraryPanel
	 * is navigated away from, this unhighlights the selected friend. If the FriendLibraryPanel
	 * is navigated to, this highlights the appropriate friend. If the FriendLibraryPanel 
	 * remaines selected, but the friend being filtered on is changed, this highlights/
	 * unhighlights the appropriate friend. 
     */
    private void addFriendListeners() {
        // Add a listener for changing friends so that we can select the action
        // if the item is selected. This is needed since all friends share the 
        // same navItem now.
        friendLibraryPanel.addFriendListener(new ListSourceChanger.ListChangedListener() {
            @Override
            public void friendChanged(Friend currentFriend) {
                // unselect the last selected action
                if(selectedFriend != null) {
                    Action action = friendActionMap.get(selectedFriend.getId());
                    if(action != null)
                        action.putValue(Action.SELECTED_KEY, false);
                } else {
                    friendActionMap.get(ALL_FRIEND_ACTION).putValue(Action.SELECTED_KEY, false);
                }
                
                selectedFriend = currentFriend;
                
                //select the current friend
                if(currentFriend == null) {
                    friendActionMap.get(ALL_FRIEND_ACTION).putValue(Action.SELECTED_KEY, true);
                } else {
                    friendActionMap.get(currentFriend.getId()).putValue(Action.SELECTED_KEY, true);
                    ensureFriendVisible(currentFriend);
                }
            }
        });
        
        // Add a listener for Navigator changes. If the navigator
        // selects the friendNavItem, highlight the appropriate friend,
        // if the Navigator selects a different NavItem, unhighlight
        // the selected friend. 
        friendNavItem.addNavItemListener(new NavItemListener() {
            public void itemRemoved() {
            }

            public void itemSelected(boolean selected) {
            	// if friend navItem was selected, highlight appropriate friend
                if(selected) {
                    selectedFriend = friendLibraryPanel.getSelectedFriend();
                    
                    //select the current friend
                    if(selectedFriend == null) {
                        friendActionMap.get(ALL_FRIEND_ACTION).putValue(Action.SELECTED_KEY, true);
                    } else {
                        friendActionMap.get(selectedFriend.getId()).putValue(Action.SELECTED_KEY, true);
                        ensureFriendVisible(selectedFriend);
                    }
                } else { // friend navItem no longer selected, unhighlight friend
                    selectedFriend = null;
                    Friend currentFriend = friendLibraryPanel.getSelectedFriend();
                    if(currentFriend == null) {
                        friendActionMap.get(ALL_FRIEND_ACTION).putValue(Action.SELECTED_KEY, selected);                
                    } else {
                        friendActionMap.get(currentFriend.getId()).putValue(Action.SELECTED_KEY, selected);
                    }
                }
            }
        });
    }
    
    private Action createP2PAction(final NavItem libraryItem) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectP2pNetworkShareList();
            }
        };
        // When the NavItem becomes selected, we only want to select
        // this action if P2P Network IS the current filter.
        libraryItem.addNavItemListener(new NavItemListener() {
            public void itemRemoved() {
            }

            public void itemSelected(boolean selected) {
                Friend currentFriend = myLibraryPanel.getCurrentFriend();
                boolean p2pSelected = currentFriend != null && currentFriend.getId().equals(Friend.P2P_FRIEND_ID);
                action.putValue(Action.SELECTED_KEY, selected && p2pSelected);
            }
        });

        action.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        libraryItem.select();
                    }
                }
            }
        });        
        action.putValue(NavigatorUtils.NAV_ITEM, libraryItem);
        
        // Add a listener for changing friends, so that we can select the action
        // if the item is selected.
        myLibraryPanel.addFriendListener(new ListSourceChanger.ListChangedListener() {
            @Override
            public void friendChanged(Friend currentFriend) {
                boolean p2pSelected = currentFriend != null && currentFriend.getId().equals(Friend.P2P_FRIEND_ID);
                action.putValue(Action.SELECTED_KEY, libraryItem.isSelected() && p2pSelected);
            }
        });
        return action;
    }
    
    private NavPanel initializePanel(String title, Action action, String name) {
        NavPanel panel = navPanelFactory.createNavPanel(action, null);
        panel.setTitle(title);
        panel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        panel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        panel.setName(name);                
        return panel;
    }
    
    @Inject void register(@Named("known") ListenerSupport<FriendEvent> knownListeners,
                          @Named("available") ListenerSupport<FriendEvent> availListeners,
                          ListenerSupport<FriendConnectionEvent> connectionListeners,
                          ListenerSupport<FriendRequestEvent> friendRequestListeners) {
        
        friendRequestListeners.addListener(new EventListener<FriendRequestEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendRequestEvent event) {
                friendRequestPanel.addRequest(event.getData());
            }
        });
        
        connectionListeners.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch(event.getType()) {
                case CONNECT_FAILED:
                case DISCONNECTED:
                case CONNECTING:
                    friendsScrollArea.setOpaque(false);
                    repaint();
                    break;
                case CONNECTED:
                    friendsScrollArea.setOpaque(true);
                    repaint();
                    break;
                }
            }
        });
        
        knownListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getData());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        LOG.debugf("creating new friend nav panel {0}", event.getData().getId());
                        panel = createFriendNavPanel(event.getData());
                        offlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
                        LOG.debugf("removing matching friend library {0}", event.getData().getId());
                        // Shift selection to the All Friends library
                        if(panel.isSelected()) {
                            friendLibraryPanel.setFriend(null);
                        }
                        panel.getParentList().removePanel(panel);
                        disposeNavPanel(panel);
                    }
					break;
                }
            }
        });
        
        availListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getData());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        panel = createFriendNavPanel(event.getData());
                        onlineList.addNavPanel(panel);
                    } else if(panel.getParentList() == offlineList) {
                        offlineList.removePanel(panel);
                        onlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
                        panel.getParentList().removePanel(panel);
                        offlineList.addNavPanel(panel);
                    }
					break;
                }
            }
        });
        
    }
    
    /**
	 * Creates Action to select a friend library or browse host. This handles the selection 
	 * when the user mouse clicks on this friend. This friend can also be selected through
	 * navigation. This is handled in addFriendListeners().
	 */
	 // ideally the logic in addFriendListeners would be localized here however these actions
	 // get destroyed when logging out and its difficult to remove the listeners if localized
    private Action createFriendAction(final NavItem libraryItem, final Friend friend) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                friendLibraryPanel.setFriend(friend);
                libraryItem.select();
            }
        };

        action.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        libraryItem.select();
                    }
                }
            }
        });        
        
        friendActionMap.put(friend.getId(), action);

        return action;
    }
    
    private NavPanel getPanelForFriend(Friend friend) {
        for(NavList list : allLists) {
            NavPanel panel = list.getPanelForFriend(friend);
            if(panel != null) {
                return panel;
            }
        }
        return null;
    }
    
    @Override
    public JXPanel getComponent() {
        return this;
    }
    
    @Override
    public void selectFriendLibrary(Friend friend) {
        for(NavList list : allLists) {
            if(list.selectFriendLibrary(friend) != null) {
                break;
            }
        }
    }
    
    private void selectP2pNetworkShareList() {
        myLibrary.select();
        myLibraryPanel.showSharingState(SharingTarget.GNUTELLA_SHARE.getFriend());
    }    
    
    @Override
    public void selectFriendShareList(Friend friend) {
        myLibrary.select();
        myLibraryPanel.showSharingState(friend);
    }
    
    @Override
    public void selectLibrary() {
        myLibrary.select();
        myLibraryPanel.showAllFiles();
    }

    @Override
    public void selectInLibrary(URN urn, Category category) {
        myLibrary.select();
        myLibraryPanel.selectItem(urn, new Catalog(category));
    }
    
    @Override
    public void selectInLibrary(File file, Category category) {
        myLibrary.select();
        myLibraryPanel.selectItem(file, new Catalog(category));
    }
    
    @Override
    public File getPreviousInLibrary(File file) {
        return myLibraryPanel.getPreviousItem(file, activeCatalog);
    }
    
    @Override
    public File getNextInLibrary(File file) {
        return myLibraryPanel.getNextItem(file, activeCatalog);
    }
    
    @Override
    public void setActiveCatalog(Catalog catalog) {
        this.activeCatalog = catalog;
    }
    
    private void ensureFriendVisible(Friend friend) {
        for(NavList list : allLists) {
            if(list.ensureFriendVisible(friend) != null) {
                break;
            }
        }
    }
    
    /**
	 * Cleanup a friend panel
	 */
    private void disposeNavPanel(NavPanel navPanel) {
        navPanel.removeBrowse();       
        friendActionMap.remove(navPanel.getFriend().getId());
    }
    
    private NavPanel createFriendNavPanel(final Friend friend) {
        NavPanel navPanel = navPanelFactory.createNavPanel(createFriendAction(friendNavItem, friend), friend);

        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        // don't add a transfer handler to anonymous browse hosts
        if(friend != null && !friend.isAnonymous()) {
            navPanel.setTransferHandler(new LocalFileListTransferHandler(shareListManager.getOrCreateFriendShareList(friend)));
            try {
                navPanel.getDropTarget().addDropTargetListener(new GhostDropTargetListener(navPanel,ghostPane, friend));
            } catch (TooManyListenersException ignoreException) {            
            }   
        }
        
        return navPanel;
    }
    
    @Override
    public Friend getSelectedFriend() {
        return selectedFriend;
    }
    
    private class FriendLibraryUpdater extends AbstractListEventListener<FriendLibrary> {
        @Override
        protected void itemAdded(FriendLibrary item, int idx, EventList<FriendLibrary> source) {
            LOG.debugf("friend library {0} added ...", item.getFriend().getId());  
            NavPanel panel = getPanelForFriend(item.getFriend());
            if(panel != null) {
                LOG.debugf("... removing existing friend library {0}", item.getFriend().getId());  
                panel.getParentList().removePanel(panel);
            } else {
                LOG.debugf("... creating new friend nav panel {0}", item.getFriend().getId()); 
                panel = createFriendNavPanel(item.getFriend());
            }
            
            limewireList.addNavPanel(panel);
            updatePanel(item, panel);
        }
        
        @Override
        protected void itemRemoved(FriendLibrary item, int idx, EventList<FriendLibrary> source) {
            LOG.debugf("friend library {0} removed ...", item.getFriend().getId()); 
            Friend friend = item.getFriend();
            if(friend.isAnonymous()) {
                NavPanel panel = limewireList.removePanelForFriend(item.getFriend());
                if(panel != null) {
                    // If this was selected, display the 
                    // All Friends library
                    if(panel.isSelected()) {
						friendLibraryPanel.setFriend(null);
                    }
                    disposeNavPanel(panel);
                }
            } else {
                NavPanel panel = limewireList.getPanelForFriend(item.getFriend());
                if(panel != null && panel.getFriendLibrary() == item) {
                    LOG.debugf("... removing matching friend library {0}", item.getFriend().getId()); 
                    // extra check is needed b/c when
                    // glazedlist batches up updates, add/remove events
                    // can get dispatched out of order
                    limewireList.removePanel(panel);
                    panel.removeBrowse();
                    onlineList.addNavPanel(panel); // Assume still online.
                } else {
                    // else probably signed off & cleared the lists.
                    LOG.debugf("... friend library {0} was already removed", item.getFriend().getId()); 
                }
            }
        }
        
        @Override
        protected void itemUpdated(FriendLibrary item, FriendLibrary prior, int idx, EventList<FriendLibrary> source) {
            NavPanel panel = limewireList.getPanelForFriend(item.getFriend());                
            if(panel != null) {
                LOG.debugf("updating navpanel for {0} to state {1}", item.getFriend().getId(), item.getState());  
                updatePanel(item, panel);
            } else {
                LOG.debugf("null navpanel for {0}", item.getFriend().getId());    
            }
        }
        
        private void updatePanel(FriendLibrary item, NavPanel panel) {
            panel.updateLibrary(item);
        }
    }
    
    private class MoveAction extends AbstractAction {
        private final NavList navList;
        private final boolean selectFirst;
        
        MoveAction(NavList navList, boolean selectFirst) {
            this.navList = navList;
            this.selectFirst = selectFirst;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(selectFirst) {
                navList.selectFirst();
            } else {
                navList.selectLast();
            }
        }
    }
    
    private static class FriendAdapter implements Friend {
        private final String text;
        
       private FriendAdapter(String text) {
           this.text = text;
       }
        
        @Override
        public String getFirstName() {
            return text;
        }
        @Override
        public String getId() {
            return text;
        }
        @Override
        public String getName() {
            return text;
        }
        @Override
        public Network getNetwork() {
            return null;
        }
        @Override
        public String getRenderName() {
            return text;
        }
        @Override
        public boolean isAnonymous() {
            return false;
        }
        @Override
        public void setName(String name) {
        }

        @Override
        public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        }

        @Override
        public MessageWriter createChat(MessageReader reader) {
            return null;
        }

        @Override
        public void setChatListenerIfNecessary(IncomingChatListener listener) {
        }

        @Override
        public void removeChatListener() {
        }

        @Override
        public FriendPresence getActivePresence() {
            return null;
        }

        @Override
        public boolean hasActivePresence() {
            return false;
        }

        @Override
        public boolean isSignedIn() {
            return false;
        }

        @Override
        public Map<String, FriendPresence> getPresences() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }
    }
}
