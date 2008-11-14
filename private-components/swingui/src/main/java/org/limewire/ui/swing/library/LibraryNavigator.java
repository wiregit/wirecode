package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.BusyPainter;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryNavTransferHandler;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JXPanel implements RegisteringEventListener<RosterEvent> {

    private static final String DIVIDER = "/";
    private static final String NAME = "__@internal@__";
    public static final String NAME_PREFIX = NAME + DIVIDER;

    private final SectionHeading titleLabel;
    private final List<NavPanel> navPanels = new ArrayList<NavPanel>();
       
    @Resource private Icon removeLibraryIcon;
    @Resource private Icon removeLibraryHoverIcon;
    
    @Resource private Color selectedBackground;
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font failedTextFont;
    @Resource private Font textFont;
    @Resource private Color textColor;
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final DownloadListManager downloadListManager;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;
    private final FriendLibraryMediatorFactory friendLibraryBaseFactory;
    private final MyLibraryMediator myLibraryBasePanel;
    private final SharingLibraryFactory sharingFactory;
    
    private final Navigator navigator;

    @Inject
    LibraryNavigator(final Navigator navigator, LibraryManager libraryManager,
            RemoteLibraryManager remoteLibraryManager,
            FriendLibraryMediatorFactory friendFactory,
            DownloadListManager downloadListManager,
            ShareListManager shareListManager,
            MyLibraryMediator myLibraryBasePanel,
            SharingLibraryFactory sharingFactory) {
        
        GuiUtils.assignResources(this);
        EventAnnotationProcessor.subscribe(this);
        
        this.remoteLibraryManager = remoteLibraryManager;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.navigator = navigator;
        this.friendLibraryBaseFactory = friendFactory;
        this.myLibraryBasePanel = myLibraryBasePanel;
        this.sharingFactory = sharingFactory;
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");

        setLayout(new MigLayout("insets 0, gap 0"));
        add(titleLabel, "growx, alignx left, aligny top, wrap");

        createMyLibrary();

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                addOrUpdateNavPanelForFriend(item.getFriend(), item.getSwingModel(), item.getState());
            }
            
            @Override
            protected void itemRemoved(FriendLibrary item) {
                Friend friend = item.getFriend();
                if(friend.isAnonymous()) {
                    removeNavPanelForFriend(item.getFriend());
                } else {
                    removeFriendBrowse(item.getFriend());
                }
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item) {
                updateNavPanelForFriend(item.getFriend(), item.getState(), item.getSwingModel());
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
    }
    
    private void addOrUpdateNavPanelForFriend(Friend friend, EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        if (!containsFriend(friend)) {
            FriendLibraryMediator component = friendLibraryBaseFactory.createFriendLibraryBasePanel(friend);
            NavPanel navPanel = new NavPanel(createAction(navigator, friend, friend.getId(), component), 
                    friend, component, libraryState);
            addNavPanel(navPanel);
            if(eventList != null) {
                navPanel.updateLibrary(eventList, libraryState);
            }
        } else {
            updateNavPanelForFriend(friend, libraryState, eventList);
        }
    }
    
    private void createMyLibrary() {
        LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        myLibraryBasePanel.setMainCardEventList(libraryList.getSwingModel());
        addNavPanel(new NavPanel(createAction(navigator, Me.ME, NAME_PREFIX, myLibraryBasePanel), 
                        Me.ME, null, libraryList.getState()));
        libraryList.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("state")) {
                    updateNavPanelForFriend(Me.ME, (LibraryState)evt.getNewValue(), null);
                }
            }
        });
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(RosterEvent event) {
        Friend friend = event.getSource();
        switch (event.getType()) {
        case USER_ADDED:
            addOrUpdateNavPanelForFriend(friend, null, null);
            break;
        case USER_REMOVED:
            removeNavPanelForFriend(friend);
            break;
        }
    }
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        for(NavPanel panel : oldPanels) {
            if(!panel.getFriend().equals(Me.ME)) {
                removeNavPanelForFriend(panel.getFriend());
            }
        }
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    private void updateNavPanelForFriend(Friend friend, LibraryState state, EventList<RemoteFileItem> eventList) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                panel.updateLibraryState(state);
                if(eventList != null) {
                    panel.updateLibrary(eventList, state);
                } else {
                    assert friend == Me.ME;
                }
                break;
            }
        }
    }
    
    private void ensureFriendVisible(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                scrollRectToVisible(panel.getBounds());
                break;
            }
        }
    }
    
    private boolean containsFriend(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                return true;
            }
        }
        return false;
    }

    private void addNavPanel(NavPanel panel) {
        // Find the index where to insert.
        int idx = Collections.binarySearch(navPanels, panel, new Comparator<NavPanel>() {
            @Override
            public int compare(NavPanel o1, NavPanel o2) {
                Friend f1 = o1.getFriend();
                Friend f2 = o2.getFriend();
                if(o1 == o2) {
                    return 0;
                } else if(f2 instanceof Me) {
                    return 1;
                } else if(f1 instanceof Me) {
                    return -1;
                } else if(f1.isAnonymous() && !f2.isAnonymous()) {
                    return 1;
                } else if(f2.isAnonymous() && !f1.isAnonymous()) {
                    return -1;
                } else {
//                    boolean f1HasLibrary = remoteLibraryManager.hasFriendLibrary(f1);
//                    boolean f2HasLibrary = remoteLibraryManager.hasFriendLibrary(f2);
//                    // show buddies that are logged into limewire first by alphabetical order, than everyone else
//                    if(f1HasLibrary && !f2HasLibrary) {
//                        return -1;
//                    } else if(!f1HasLibrary && f2HasLibrary) {
//                        return 1;
//                    } else                   
                        return f1.getRenderName().compareToIgnoreCase(f2.getRenderName());
                }
            }
        });
        int insertIdx = idx >= 0 ? idx : -(idx+1);
        navPanels.add(insertIdx, panel);
        add(panel, "alignx left, aligny top, growx, wrap", insertIdx+1); // +1 because of title
    }
       
    private void moveDown() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                if(iter.hasNext()) {
                    panel = iter.next();
                    panel.select();
                    return;
                }
                return; // No selection possible.
            }
        }
    }
     
    private void moveUp() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                iter.previous();
                if(iter.hasPrevious()) {
                    panel = iter.previous();
                    panel.select();
                    return;
                }
                return; // No selection possible.
            }
        }
    }
    
    //removes the browse but keeps the buddy reference
    private void removeFriendBrowse(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                panel.removeBrowse();
                break;
            }
        }
    }
    
    private void removeNavPanelForFriend(Friend friend) {
        for(Iterator<NavPanel> i = navPanels.iterator(); i.hasNext(); ) {
            NavPanel panel = i.next();
            if(panel.getFriend() != Me.ME && panel.getFriend().getId().equals(friend.getId())) {
                i.remove();
                remove(panel);
                panel.dispose();
                break;
            }
        }
        invalidate();
        repaint(); // Must forcibly paint, otherwise might not redraw w/o panel.
    }
    
    public void selectFriendLibrary(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                panel.select();
                break;
            }
        }
    }
    
    public void selectFriendShareList(Friend friend) {
        JComponent component = sharingFactory.createSharingLibrary(myLibraryBasePanel, friend, 
            libraryManager.getLibraryManagedList().getSwingModel(),
            shareListManager.getFriendShareList(friend));

        myLibraryBasePanel.setAuxCard(component);
        myLibraryBasePanel.showAuxCard();

        selectFriendLibrary(Me.ME);
    }
    
    
    private Action createAction(Navigator navigator, Friend friend, String navId, JComponent component) {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId(), component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, friend);
    }
    
    private <T> Action decorateAction(Action action, NavItem navItem, final Disposable disposable, final Friend friend) {        
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
        action.putValue(Action.NAME, I18n.tr("Library"));
        return action;
    }
    
    private class NavPanel extends JXPanel {
        private Friend friend;
        private final CategoryLabel categoryLabel;
        private MouseListener removeListener;
        private JXBusyLabel statusIcon;
        private Action action;
        private FriendLibraryMediator libraryPanel;
        
        public NavPanel(Action action, Friend friend, FriendLibraryMediator component, LibraryState libraryState) {
            super(new MigLayout("insets 0, gap 0, fill"));
            setOpaque(false);
            this.action = action;
            this.friend = friend;           
            this.libraryPanel = component;
            
            categoryLabel = new CategoryLabel(action);
            categoryLabel.setText(friend.getRenderName());
            categoryLabel.addActionListener(actionListener);
            statusIcon = new JXBusyLabel(new Dimension(12, 12));
            statusIcon.setOpaque(false);
            
            add(categoryLabel, "gapbefore 0, gaptop 2, grow");
            add(statusIcon, "gaptop 2, alignx right, gapafter 4, hidemode 2, wrap");
            if(libraryState == null) {
                unbusy();
            } else {
                updateLibraryState(libraryState);
            }
            
            if(friend == Me.ME){
                setTransferHandler(new MyLibraryNavTransferHandler(downloadListManager, libraryManager));
            } else {
                setTransferHandler(new FriendLibraryNavTransferHandler(friend, shareListManager));
            }
            
            getActionMap().put(MoveDown.KEY, new MoveDown());
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), MoveDown.KEY);
          
            getActionMap().put(MoveUp.KEY, new MoveUp());
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), MoveUp.KEY);
        }
        
        private void busy() {
            removeEjectListener();
            BusyPainter painter = statusIcon.getBusyPainter();
            statusIcon.setIcon(new EmptyIcon(12, 12));
            statusIcon.setBusyPainter(painter);
            statusIcon.setVisible(true);
            statusIcon.setBusy(true);
        }
        
        private void unbusy() {
            if(friend.isAnonymous()) {
                statusIcon.setVisible(true);
                statusIcon.setBusy(false);
                statusIcon.setIcon(removeLibraryIcon);
                addEjectListener();
            } else {
                removeEjectListener();
                statusIcon.setVisible(false);
                statusIcon.setBusy(false);
                statusIcon.setIcon(new EmptyIcon(12, 12));
            }
        }
        
        private void removeEjectListener() {
            if(removeListener != null) {
                statusIcon.removeMouseListener(removeListener);
                removeListener = null;
            }
        }
        
        private void addEjectListener() {
            if (removeListener == null) {
                removeListener = new ActionHandListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { 
                        remoteLibraryManager.removeFriendLibrary(friend);
                    }
                }) {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        super.mouseEntered(e);
                        statusIcon.setIcon(removeLibraryHoverIcon);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        super.mouseExited(e);
                        statusIcon.setIcon(removeLibraryIcon);
                    }
                };
                statusIcon.addMouseListener(removeListener);
            }
        }
        
        public void updateLibraryState(LibraryState libraryState) {
            switch(libraryState) {
            case FAILED_TO_LOAD:
                categoryLabel.setFont(failedTextFont);
                unbusy();
                break;
            case LOADED:
                categoryLabel.setFont(textFont);
                unbusy();
                break;
            case LOADING:
                categoryLabel.setFont(textFont);
                busy();
                break;
            }
        }
        
        public void updateLibrary(EventList<RemoteFileItem> eventList, LibraryState state) {
            libraryPanel.createLibraryPanel(eventList, state);
        }

        public boolean hasSelection() {
            return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
        }
        
        public void select() { 
            action.actionPerformed(null);
        }
        
        public void removeBrowse() {
            if(libraryPanel != null)
                libraryPanel.showMainCard();
        }

        public void dispose() {
            NavItem navItem = navigator.getNavItem(NavCategory.LIBRARY, friend.getId());
            navItem.remove();
        }
        
        public Friend getFriend() {
            return friend;
        }
        
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestFocus();
            }
        };
        
    }

    private class CategoryLabel extends ActionLabel {
        public CategoryLabel(Action action) {
            super(action, false);
            
            setFont(textFont);
            setForeground(textColor);
            setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
            setMinimumSize(new Dimension(0, 22));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 22));
            
            getAction().addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(evt.getNewValue().equals(Boolean.TRUE)) {
                            setBackground(selectedBackground);
                            setForeground(selectedTextColor);
                            setFont(selectedTextFont);
                            setOpaque(true);
                        } else {
                            setOpaque(false);
                            setForeground(textColor);
                            setFont(textFont);
                        }
                    }
                }
            });
        }
    }
    
    private class MoveDown extends AbstractAction {
        final static String KEY = "MOVE_DOWN";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            moveDown();
        }
    }
    
    private class MoveUp extends AbstractAction {
        final static String KEY = "MOVE_UP";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            moveUp();
        }
    }
    
    private static class Me implements Friend {
        private static final Me ME = new Me();
        
        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public String getId() {
            return NAME;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getRenderName() {
            return I18n.tr("Me");
        }

        @Override
        public void setName(String name) {
        }

        public Network getNetwork() {
            return null;
        }

        @Override
        public Map<String, Presence> getPresences() {
            return Collections.emptyMap();
        }
    }
}
