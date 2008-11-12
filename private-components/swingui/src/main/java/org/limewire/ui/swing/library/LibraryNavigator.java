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
import java.util.HashMap;
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
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.ActionKeys;
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
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JXPanel implements RegisteringEventListener<RosterEvent> {

    private static final String DIVIDER = "/";
    private static final String NAME = "__@internal@__";
    public static final String NAME_PREFIX = NAME + DIVIDER;

    private final SectionHeading titleLabel;
    private final List<LibraryPanel> navPanels = new ArrayList<LibraryPanel>();
    
    private final Map<String, LibraryPanel> friends = new HashMap<String, LibraryPanel>();
       
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
    private final FriendLibraryFactory friendLibraryFactory;
    
    private final Navigator navigator;

    @Inject
    LibraryNavigator(final Navigator navigator, LibraryManager libraryManager,
            RemoteLibraryManager remoteLibraryManager,
            MyLibraryFactory myLibraryFactory, 
            final FriendLibraryFactory friendLibraryFactory, 
            DownloadListManager downloadListManager,
            ShareListManager shareListManager) {
        
        GuiUtils.assignResources(this);
        EventAnnotationProcessor.subscribe(this);
        
        this.remoteLibraryManager = remoteLibraryManager;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.navigator = navigator;
        this.friendLibraryFactory = friendLibraryFactory;
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");

        setLayout(new MigLayout("insets 0, gap 0"));
        add(titleLabel, "growx, alignx left, aligny top, wrap");
        
        LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        addNavPanel(new LibraryPanel(createLibraryAction(navigator, myLibraryFactory, libraryList.getSwingModel()), Me.ME));
        libraryList.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("state")) {
                    updateNavPanelForMe(Me.ME, (LibraryState)evt.getNewValue());
                }
            }
        });

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                Friend friend = item.getFriend();
                if(friend.isAnonymous()) {
                    FriendLibraryPanel component = (FriendLibraryPanel) friendLibraryFactory.createFriendLibrary(friend);
                    addNavPanel(new LibraryPanel(createFriendAction(navigator, friend, component, item.getSwingModel()),
                                        friend, component, item.getState()));
                }
            }
            @Override
            protected void itemRemoved(FriendLibrary item) {
                Friend friend = item.getFriend();
                if(friend.isAnonymous())
                    removeNavPanelForFriend(item.getFriend());
                else
                    removeFriendBrowse(item.getFriend());
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item) {
                updateNavPanelForFriend(item.getFriend(), item.getState(), item.getSwingModel());
                //TODO: resort the order 
//                updateNavPanel(item.getFriend());
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            if(friends.containsKey(event.getSource().getId()))
                return;
            Friend friend = event.getSource();
            FriendLibraryPanel component = (FriendLibraryPanel) friendLibraryFactory.createFriendLibrary(friend);
            addNavPanel(new LibraryPanel(createFriendAction(navigator, friend, component, null), friend, component));
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            Friend friend = event.getSource();
            removeNavPanelForFriend(friend);
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        List<LibraryPanel> oldPanels = new ArrayList<LibraryPanel>(navPanels);
        for(LibraryPanel panel : oldPanels) {
            if(!panel.getFriend().equals(Me.ME))
                removeNavPanelForFriend(panel.getFriend());
        }
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    protected void updateNavPanelForFriend(Friend friend, LibraryState state, EventList<RemoteFileItem> eventList) {
        for(LibraryPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                panel.updateLibraryState(state);
                panel.updateLibrary(eventList, state);
                return;
            }
        }
    }
    
    protected void updateNavPanelForMe(Friend friend, LibraryState state) {
        for(LibraryPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                panel.updateLibraryState(state);
            }
        }
    }
    
    private void ensureFriendVisible(Friend friend) {
        for(LibraryPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                scrollRectToVisible(panel.getBounds());
                break;
            }
        }
    }

    private void addNavPanel(LibraryPanel panel) {
        // Find the index where to insert.
        int idx = Collections.binarySearch(navPanels, panel, new Comparator<LibraryPanel>() {
            @Override
            public int compare(LibraryPanel o1, LibraryPanel o2) {
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
        ListIterator<LibraryPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            LibraryPanel panel = iter.next();
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
        ListIterator<LibraryPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            LibraryPanel panel = iter.next();
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
        for(Iterator<LibraryPanel> i = navPanels.iterator(); i.hasNext(); ) {
            LibraryPanel panel = i.next();
            if(panel.getFriend() != Me.ME && panel.getFriend().getId().equals(friend.getId())) {
                panel.removeBrowse();
                break;
            }
        }
    }
    
    private void removeNavPanelForFriend(Friend friend) {
        for(Iterator<LibraryPanel> i = navPanels.iterator(); i.hasNext(); ) {
            LibraryPanel panel = i.next();
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
    
    public void collapseOthersAndExpandThis(Friend friend) {
        for(LibraryPanel panel : navPanels) {
            panel.select();
//            if(friend == null || !panel.getFriend().getId().equals(friend.getId())) {
//                panel.collapse();
//            } else {
//                panel.expand();
//            }
        }
    }
    
    private Action createLibraryAction(Navigator navigator, MyLibraryFactory factory, EventList<LocalFileItem> eventList) {
        JComponent component = factory.createMyLibrary(eventList);
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, NAME_PREFIX, component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, eventList, Me.ME);
    }
    
    private Action createFriendAction(Navigator navigator, Friend friend, JComponent component, EventList<RemoteFileItem> eventList) {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId(), component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateFriendAction(action, navItem, (Disposable)component, eventList, friend);
    }
    
    private <T> Action decorateFriendAction(final Action action, NavItem navItem, final Disposable disposable,
            final EventList<T> filterList, final Friend friend) {
        
        action.putValue(Action.NAME, I18n.tr("Library"));
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                disposable.dispose();
            }
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    ensureFriendVisible(friend);
//                    collapseOthersAndExpandThis(friend);
                }
            }
        });
        return action;
    }
    
    private <T> Action decorateAction(final Action action, NavItem navItem, final Disposable disposable,
            final EventList<LocalFileItem> eventList, final Friend friend) {
        final ListEventListener<T> listener;
        if(friend != Me.ME) {             
             listener = new ListEventListener<T>() {
                @Override
                public void listChanged(ListEvent<T> listChanges) {
                    action.putValue(ActionKeys.VISIBLE,eventList.size() > 0);
                }
            };
            listener.listChanged(null); // initial sync
        } else {
            listener = null;
        }
        
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                disposable.dispose();
            }
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    ensureFriendVisible(friend);
//                    collapseOthersAndExpandThis(friend);
                }
            }
        });
        action.putValue(Action.NAME, I18n.tr("Library"));
        return action;
    }
    
    private class LibraryPanel extends JXPanel {
        private Friend friend;
        private final CategoryLabel categoryLabel;
        private MouseListener removeListener;
        private JXBusyLabel statusIcon;
        private Action action;
        private FriendLibraryPanel libraryPanel;
        
        public LibraryPanel(Action action, Friend friend) {
            this(action, friend, null);
        }
        
        public LibraryPanel(Action action, Friend friend, FriendLibraryPanel component) {
            this(action, friend, component, null);
        }
        
        public LibraryPanel(Action action, Friend friend, FriendLibraryPanel component, LibraryState libraryState) {
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
            if(friend.isAnonymous()) {
                add(statusIcon, "gaptop 2, alignx right, gapafter 4, wrap");
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
            if(libraryPanel != null && state.equals(LibraryState.LOADED))
                libraryPanel.createLibraryPanel(eventList);
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
    }
}
