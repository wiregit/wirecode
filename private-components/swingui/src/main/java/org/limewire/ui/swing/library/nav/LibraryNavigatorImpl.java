package org.limewire.ui.swing.library.nav;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

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
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryNavTransferHandler;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.library.FriendLibraryMediatorFactory;
import org.limewire.ui.swing.library.MyLibraryMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class LibraryNavigatorImpl extends JXPanel implements LibraryNavigator {

    private static final String NAME = "__@internal@__";

    private final NavPanel myLibrary;
    private final NavPanel p2pNetwork;
    private final NavPanel allFriends;
    private final NavList limewireList;
    private final NavList onlineList;
    private final NavList offlineList;
    private final NavList[] allLists;
    
    private final Navigator navigator;
    private final MyLibraryMediator myLibraryMediator;
    private final ShareListManager shareListManager;
    private final FriendLibraryMediatorFactory friendLibraryMediatorFactory;
    private final NavPanelFactory navPanelFactory;    

    @Inject
    LibraryNavigatorImpl(Navigator navigator,
            LibraryManager libraryManager,
            RemoteLibraryManager remoteLibraryManager,
            DownloadListManager downloadListManager,
            ShareListManager shareListManager,
            MyLibraryMediator myLibraryMediator,
            NavPanelFactory navPanelFactory,
            FriendLibraryMediatorFactory friendLibraryMediatorFactory,
            FriendsSignInPanel friendsPanel,
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, 999));
        setPreferredSize(new Dimension(150, 999));
        
        this.myLibraryMediator = myLibraryMediator;
        this.shareListManager = shareListManager;
        this.limewireList = new NavList();
        this.onlineList = new NavList();
        this.offlineList = new NavList();
        this.allLists = new NavList[] { limewireList, onlineList, offlineList };
        this.navPanelFactory = navPanelFactory;
        this.friendLibraryMediatorFactory = friendLibraryMediatorFactory;
        this.navigator = navigator;
        
        limewireList.setTitleText(I18n.tr("On LimeWire"));
        onlineList.setTitleText(I18n.tr("Online"));
        offlineList.setTitleText(I18n.tr("Offline"));
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        
        LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        Friend me = new FriendAdapter(NAME, I18n.tr("My Library"));
        myLibraryMediator.setMainCardEventList(me, libraryList.getSwingModel());
        myLibrary = navPanelFactory.createNavPanel(createMyLibraryAction(), me, null);
        myLibrary.updateLibraryState(libraryList.getState());        
        myLibrary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibraryNavigatorImpl.this.myLibraryMediator.showLibraryCard();
            }
        });        
        myLibrary.setTransferHandler(new MyLibraryNavTransferHandler(downloadListManager, libraryManager, saveLocationExceptionHandler));
        myLibrary.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        myLibrary.setName("LibraryNavigator.myLibrary");
        libraryList.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("state")) {
                    myLibrary.updateLibraryState((LibraryState)evt.getNewValue());
                }
            }
        });
        
        Friend p2p = new FriendAdapter("p2p", I18n.tr("P2P Network"));
        p2pNetwork = navPanelFactory.createNavPanel(createP2PNetworkAction(), p2p, null);
        p2pNetwork.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        p2pNetwork.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        p2pNetwork.setName("LibraryNavigator.p2pNetwork");
        
        Friend all = new FriendAdapter("allFriends", I18n.tr("All Friends"));
        allFriends = navPanelFactory.createNavPanel(createAllFriendsAction(), all, null);
        allFriends.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        allFriends.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        allFriends.setName("LibraryNavigator.allFriends");

        setLayout(new MigLayout("insets 0, fill, gap 2"));
        add(myLibrary, "gaptop 2, growx, wmin 0, wrap"); 
        add(p2pNetwork, "growx, wmin 0, wrap");
        add(allFriends, "growx, wmin 0, wrap");
        add(friendsPanel, "growx, wmin 0, wrap");
        
        JXPanel friendsListPanel = new JXPanel(new VerticalLayout(2));
        friendsListPanel.setOpaque(false);
        friendsListPanel.setScrollableTracksViewportHeight(false);
        JScrollPane scrollableNav = new JScrollPane(friendsListPanel);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        add(scrollableNav, "grow, wmin 0, wrap");
        
        // Add all the navlists and hook up the actions.
        myLibrary.getActionMap().put(NavKeys.MOVE_DOWN, p2pNetwork.getAction());
        p2pNetwork.getActionMap().put(NavKeys.MOVE_UP, myLibrary.getAction());
        p2pNetwork.getActionMap().put(NavKeys.MOVE_DOWN, allFriends.getAction());
        allFriends.getActionMap().put(NavKeys.MOVE_UP, p2pNetwork.getAction());
        allFriends.getActionMap().put(NavKeys.MOVE_DOWN, new MoveAction(allLists[0], true));
        for(int i = 0; i < allLists.length; i++) {
            // Move up action goes to My Library if first
            if(i == 0) {
                allLists[i].getActionMap().put(NavKeys.MOVE_UP, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        allFriends.select();
                    }
                });
            } else {
                // Otherwise it goes to the prior list.
                allLists[i].getActionMap().put(NavKeys.MOVE_UP, new MoveAction(allLists[i-1], false));
            }
            
            // If this isn't the last list, down goes to the next
            if(i < allLists.length - 1) {
                allLists[i].getActionMap().put(NavKeys.MOVE_DOWN, new MoveAction(allLists[i+1], true));
            }
            
            friendsListPanel.add(allLists[i]);
        }

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                NavPanel panel = getPanelForFriend(item.getFriend());
                if(panel != null) {
                    panel.getParentList().removePanel(panel);
                } else {
                    panel = createFriendNavPanel(item.getFriend());
                }
                
                limewireList.addNavPanel(panel);
                updatePanel(item, panel);
            }
            
            @Override
            protected void itemRemoved(FriendLibrary item) {
                Friend friend = item.getFriend();
                if(friend.isAnonymous()) {
                    NavPanel panel = limewireList.removePanelForFriend(item.getFriend());
                    if(panel != null) {
                        disposeNavPanel(panel);
                    }
                } else {
                    NavPanel panel = limewireList.getPanelForFriend(item.getFriend());
                    if(panel != null) {
                        limewireList.removePanel(panel);
                        panel.removeBrowse();
                        onlineList.addNavPanel(panel); // Assume still online.
                    } // else probably signed off & cleared the lists.
                }
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item) {
                NavPanel panel = limewireList.getPanelForFriend(item.getFriend());
                if(panel != null) {
                    updatePanel(item, panel);
                }
            }
            
            void updatePanel(FriendLibrary item, NavPanel panel) {
                panel.updateLibraryState(item.getState());
                panel.updateLibrary(item.getSwingModel(), item.getState());
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
    }
    
    @Inject void register(@Named("known") ListenerSupport<FriendEvent> knownListeners,
                          @Named("available") ListenerSupport<FriendEvent> availListeners) {
        
        knownListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getSource());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        panel = createFriendNavPanel(event.getSource());
                        offlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
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
                NavPanel panel = getPanelForFriend(event.getSource());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        panel = createFriendNavPanel(event.getSource());
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
    
    private Action createMyLibraryAction() {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, NAME, myLibraryMediator);
        Action action = NavigatorUtils.getNavAction(navItem);
        return action;
    }
    
    private Action createP2PNetworkAction() {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, "P2P Network", new JLabel("P2P Network"));
        Action action = NavigatorUtils.getNavAction(navItem); 
        return action;
    }
    
    private Action createAllFriendsAction() {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, "All Friends", new JLabel("All Friends"));
        Action action = NavigatorUtils.getNavAction(navItem); 
        return action;
    }
    
    @Override
    public void selectFriendLibrary(Friend friend) {
        for(NavList list : allLists) {
            if(list.selectFriendLibrary(friend) != null) {
                break;
            }
        }
    }
    
    @Override
    public void selectInLibrary(URN urn, Category category) {
        throw new RuntimeException("TODO: select in library");
//        navigator.getNavItem(NavCategory.LIBRARY,
//                NAME_PREFIX + propertiable.getCategory()).select(
//                new NavSelectable<URN>() {
//                    @Override
//                    public URN getNavSelectionId() {
//                        return ;
//                    }
//                });   
    }
    
    private void ensureFriendVisible(Friend friend) {
        for(NavList list : allLists) {
            if(list.ensureFriendVisible(friend) != null) {
                break;
            }
        }
    }
    
    private void disposeNavPanel(NavPanel navPanel) {
        navPanel.removeBrowse();
        navigator.getNavItem(NavCategory.LIBRARY, navPanel.getFriend().getId()).remove();
    }    
    
    private NavPanel createFriendNavPanel(Friend friend) {
        final FriendLibraryMediator component = friendLibraryMediatorFactory.createFriendLibraryBasePanel(friend);
        NavPanel navPanel = navPanelFactory.createNavPanel(createFriendAction(navigator, friend, component), 
                friend, component);
        navPanel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                component.showLibraryCard();
            }
        });
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        navPanel.setTransferHandler(new FriendLibraryNavTransferHandler(friend, shareListManager));
        
        return navPanel;
    }
    
    private Action createFriendAction(Navigator navigator, Friend friend, JComponent component) {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId(), component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, friend);
    }
    
    private Action decorateAction(Action action, NavItem navItem, final Disposable disposable, final Friend friend) {        
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                disposable.dispose();
            }
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    ensureFriendVisible(friend);
                }
            }
        });
        return action;
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
        private final String id;
        private final String renderName;
        
        public FriendAdapter(String id, String renderName) {
            this.id = id;
            this.renderName = renderName;
        }
        
        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getRenderName() {
            return renderName;
        }

        @Override
        public void setName(String name) {
        }

        public Network getNetwork() {
            return null;
        }

        @Override
        public Map<String, FriendPresence> getFriendPresences() {
            return Collections.emptyMap();
        }
    }
}
