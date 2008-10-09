package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Cursor;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.BusyPainter;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.action.ActionKeys;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.components.ShiftedIcon;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JXPanel {

    private static final String DIVIDER = "/";
    private static final String NAME = "__@internal@__";
    public static final String NAME_PREFIX = NAME + DIVIDER;

    private final SectionHeading titleLabel;
    private final List<NavPanel> navPanels = new ArrayList<NavPanel>();
    
    @Resource private Icon audioIcon;
    @Resource private Icon videoIcon;
    @Resource private Icon imageIcon;
    @Resource private Icon appIcon;
    @Resource private Icon documentIcon;
    @Resource private Icon otherIcon;
    
    @Resource private Icon removeLibraryIcon;
    @Resource private Icon removeLibraryHoverIcon;
    
    @Resource private Color selectedBackground;
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font failedTextFont;
    @Resource private Font textFont;
    @Resource private Color textColor;
    
    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    LibraryNavigator(final Navigator navigator, LibraryManager libraryManager,
            RemoteLibraryManager remoteLibraryManager,
            MyLibraryFactory myLibraryFactory, 
            final FriendLibraryFactory friendLibraryFactory) {
        GuiUtils.assignResources(this);
        this.remoteLibraryManager = remoteLibraryManager;
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");

        setLayout(new MigLayout("insets 0, gap 0"));
        add(titleLabel, "growx, alignx left, aligny top,  wrap");
       
        addNavPanel(new NavPanel(Me.ME, createMyCategories(navigator, myLibraryFactory, libraryManager.getLibraryManagedList().getSwingModel()), LibraryState.LOADED));

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                Friend friend = item.getFriend();
                addNavPanel(new NavPanel(friend,
                            createFriendCategories(navigator, friend,
                                    friendLibraryFactory, item.getSwingModel()),
                                    item.getState()));
            }
            @Override
            protected void itemRemoved(FriendLibrary item) {
                removeNavPanelForFriend(item.getFriend());
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item) {
                updateNavPanelForFriend(item.getFriend(), item.getState());
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
        
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {
            }
        
            @Override
            public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {
            }
        
            @Override
            public void itemSelected(NavCategory category, NavItem navItem, JComponent panel) {
                if(category != NavCategory.LIBRARY) {
                    collapseOthersAndExpandThis(null);
                }
            }
        });
    }
    
    protected void updateNavPanelForFriend(Friend friend, LibraryState state) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                panel.updateLibraryState(state);
            }
        }
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
                if(!panel.incrementSelection()) {
                    while(iter.hasNext()) {
                        if(iter.next().selectFirst()) {
                            return; // We selected something!
                        }
                    }
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
                if(!panel.decrementSelection()) {
                    iter.previous(); // back us up a step.
                    while(iter.hasPrevious()) {
                        if(iter.previous().selectLast()) {
                            return; // We selected something!
                        }
                    }
                }
                return; // No selection possible.
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
        repaint(); // Must forcibly paint, otherwise might not redraw w/o panel.
    }
    
    public void collapseOthersAndExpandThis(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(friend == null || !panel.getFriend().getId().equals(friend.getId())) {
                panel.collapse();
            } else {
                panel.expand();
            }
        }
    }
    
    private Map<Category, Action> createMyCategories(Navigator navigator, MyLibraryFactory factory, EventList<LocalFileItem> eventList) {
        Map<Category, Action> categories = new LinkedHashMap<Category, Action>();
        for(Category category : Category.getCategoriesInOrder()) {
            categories.put(category, createMyCategoryAction(navigator, factory, category, eventList));
        }
        return categories;
    }
    
    private Action createMyCategoryAction(Navigator navigator, MyLibraryFactory factory, Category category, EventList<LocalFileItem> eventList) {
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        JComponent component = factory.createMyLibrary(category, filtered);
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, NAME_PREFIX + category, component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, category, filtered, Me.ME);
    }
    
    private Map<Category, Action> createFriendCategories(Navigator navigator, Friend friend, FriendLibraryFactory factory, EventList<RemoteFileItem> eventList) {
        Map<Category, Action> categories = new LinkedHashMap<Category, Action>();
        for(Category category : Category.getCategoriesInOrder()) {
            categories.put(category, createFriendCategoryAction(navigator, friend, factory, category, eventList));
        }
        return categories;
    }
    
    private Action createFriendCategoryAction(Navigator navigator, Friend friend, FriendLibraryFactory factory, Category category, EventList<RemoteFileItem> eventList) {
        FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        JComponent component = factory.createFriendLibrary(friend, category, filtered);
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId() + DIVIDER + category, component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, category, filtered, friend);
    }
    
    private <T> Action decorateAction(final Action action, NavItem navItem, final Disposable disposable,
            final Category category, final FilterList<T> filterList, final Friend friend) {
        final ListEventListener<T> listener;
        if(friend != Me.ME) {             
             listener = new ListEventListener<T>() {
                @Override
                public void listChanged(ListEvent<T> listChanges) {
                    action.putValue(ActionKeys.VISIBLE,filterList.size() > 0);
                }
            };
            listener.listChanged(null); // initial sync
            filterList.addListEventListener(listener);
        } else {
            listener = null;
        }
        
        action.putValue(Action.NAME, I18n.tr(category.toString()));
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                if(listener != null) {
                    filterList.removeListEventListener(listener);
                }
                filterList.dispose();
                disposable.dispose();
            }
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    collapseOthersAndExpandThis(friend);
                }
            }
        });
        Icon icon;
        switch (category) {
        case AUDIO:    icon = audioIcon; break;
        case DOCUMENT: icon = documentIcon; break;
        case IMAGE:    icon = imageIcon; break;
        case OTHER:    icon = otherIcon; break;
        case PROGRAM:  icon = appIcon; break;
        case VIDEO:    icon = videoIcon; break;
        default:       icon = new EmptyIcon(16, 16); break;
        }
        action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, icon));
        return action;
    }
    
    private class NavPanel extends JXPanel {
        private final CategoriesPanel categories;
        private final Friend friend;
        private final ActionLabel categoryLabel;
        private final JXBusyLabel statusIcon;
        private MouseListener removeListener;
        
        public NavPanel(Friend friend, Map<Category, Action> actions, LibraryState libraryState) {
            super(new MigLayout("insets 0, gap 0, fill"));
            setOpaque(false);
            this.categories = new CategoriesPanel(actions);
            this.friend = friend;
            categories.setAnimated(false);
            categories.setCollapsed(true);
            categories.setAnimated(true);
            categoryLabel = new ActionLabel(new AbstractAction(friend.getRenderName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    categories.ensureSelected();
                    getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
                }
            }, false);
            PropertyChangeListener changeListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                   if(categories.isCollapsed()) {
                       if(evt.getPropertyName().equals(ActionKeys.VISIBLE)) {
                           ActionHandListener.setActionHandDrawingDisabled(categoryLabel, !categories.isAnyVisible());
                        }
                   }
                }
            };
            for(Action action : actions.values()) {
                action.addPropertyChangeListener(changeListener);
            }
            ActionHandListener.setActionHandDrawingDisabled(categoryLabel, !categories.isAnyVisible());
            categoryLabel.setMinimumSize(new Dimension(30, 0));
            categoryLabel.setForeground(textColor);
            categoryLabel.setFont(textFont);
            statusIcon = new JXBusyLabel(new Dimension(12, 12));
            statusIcon.setOpaque(false);
            add(categoryLabel, "gapbefore 12, gaptop 2, grow");
            add(statusIcon, "gaptop 2, alignx right, gapafter 4, wrap");
            add(categories, "span, grow, wrap"); // the gap here is implicit in the width of the icon
                                                 // see decorateAction
            updateLibraryState(libraryState);
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

        public boolean selectFirst() {
            return categories.selectFirst();
        }

        public boolean incrementSelection() {
            return categories.incrementSelection();
        }

        public boolean selectLast() {
            return categories.selectLast();
        }

        public boolean decrementSelection() {
            return categories.decrementSelection();
        }

        public boolean hasSelection() {
            return categories.hasSelection();
        }

        public void expand() {
            if(categories.isAnyVisible()) {
                categories.setCollapsed(false);
                categories.ensureSelected();
                ActionHandListener.setActionHandDrawingDisabled(categoryLabel, true);
            }
        }

        public void dispose() {
            categories.dispose();
        }
        
        public void collapse() {
            categories.setCollapsed(true);
            ActionHandListener.setActionHandDrawingDisabled(categoryLabel, !categories.isAnyVisible());
        }
        
        public Friend getFriend() {
            return friend;
        }
        
    }
    
    private class CategoriesPanel extends JXCollapsiblePane {
        private final Map<Category, Action> categories;
        private Action lastSelectedAction = null;
        
        public CategoriesPanel(Map<Category, Action> categoryActions) {
            this.categories = categoryActions;
            setOpaque(false);
            
            // This must be subclassed because JXCollapsiblePanel forcibly
            // changes it.
            JXPanel panel = new JXPanel(new MigLayout("gap 0, insets 0, fill")) {
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            PropertyChangeListener changeListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY) && Boolean.TRUE.equals(evt.getNewValue())) {
                        lastSelectedAction = (Action)evt.getSource();
                        requestFocus();
                    } else if(evt.getPropertyName().equals(ActionKeys.VISIBLE)) {
                        // Trigger a new animation for showing the new visibility.
                        // Otherwise the height is incorrect.
                        // Must call it twice because setting to the same state
                        // is ignored.
                        boolean collapsed = isCollapsed();
                        setCollapsed(!collapsed);
                        setCollapsed(collapsed);
                    }
                }
            };
            ActionListener actionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    requestFocus();
                }
            };
            for(Category category : categories.keySet()) {
                Action action = categories.get(category);
                action.addPropertyChangeListener(changeListener);
                CategoryLabel label = new CategoryLabel(action);
                label.addActionListener(actionListener);
                panel.add(label, "grow, wrap, hidemode 2");
            }
            setContentPane(panel);
            
            // HACK -- Required to set the viewport transparent.
            JViewport viewport = (JViewport)getComponent(0);
            viewport.setOpaque(false);
            // END HACK
                        
            getActionMap().put(MoveDown.KEY, new MoveDown());
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), MoveDown.KEY);
            
            getActionMap().put(MoveUp.KEY, new MoveUp());
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), MoveUp.KEY);
        }
        
        public boolean isAnyVisible() {
            for(Action action : categories.values()) {
                if(isVisible(action)) {
                    return true;
                }
            }
            return false;
        }

        /** Returns true if any item is selected. */
        public boolean hasSelection() {
            for(Action action : categories.values()) {
                if(isSelected(action)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Selects the first visible item prior to the selected item
         * If there is nothing visible prior, this returns false.
         * Otherwise, returns true.
         */
        public boolean decrementSelection() {
            List<Action> actions = new ArrayList<Action>(categories.values());
            ListIterator<Action> iter = actions.listIterator();
            while(iter.hasNext()) {
                Action action = iter.next();
                if(isSelected(action)) {
                    iter.previous(); // back us up a step.
                    while(iter.hasPrevious()) {
                        Action previous = iter.previous();
                        if(isVisible(previous)) {
                            previous.actionPerformed(null);
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }

        /**
         * Selects the last visible item & returns true. If there are no visible
         * items, returns false.
         */
        public boolean selectLast() {
            List<Action> actions = new ArrayList<Action>(categories.values());
            ListIterator<Action> iter = actions.listIterator(actions.size());
            while(iter.hasPrevious()) {
                Action previous = iter.previous();
                if(isVisible(previous)) {
                    previous.actionPerformed(null);
                    return true;
                }
            }
            return false;
        }

        /**
         * Selects the first visible item after the selected item and returns
         * true. If there are no visible items after, returns false.
         */
        public boolean incrementSelection() {
            List<Action> actions = new ArrayList<Action>(categories.values());
            ListIterator<Action> iter = actions.listIterator();
            while(iter.hasNext()) {
                Action action = iter.next();
                if(isSelected(action)) {
                    while(iter.hasNext()) {
                        Action next = iter.next();
                        if(isVisible(next)) {
                            next.actionPerformed(null);
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }

        /**
         * Selects the first visible item and returns true. If there are no
         * visible items, returns false.
         */
        public boolean selectFirst() {
            for(Action action : categories.values()) {
                if(isVisible(action)) {
                    action.actionPerformed(null);
                    return true;
                }
            }
            return false;
        }

        public void dispose() {
            for(Action action : categories.values()) {
                NavItem item = (NavItem)action.getValue(NavigatorUtils.NAV_ITEM);
                item.remove();
            }
        }

        public void ensureSelected() {
            if(lastSelectedAction == null) {
                selectFirst();
            } else {
                setSelected(lastSelectedAction, true);
            }
        }
        
        private boolean isVisible(Action action) {
            return !Boolean.FALSE.equals(action.getValue(ActionKeys.VISIBLE));
        }
        
        private boolean isSelected(Action action) {
            return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
        }
        
        private void setSelected(Action action, boolean selected) {
            action.putValue(Action.SELECTED_KEY, selected);
        }
    }
    
    private class CategoryLabel extends ActionLabel {
        public CategoryLabel(Action action) {
            super(action, false);
            
            setFont(textFont);
            setForeground(textColor);
            setIconTextGap(6);
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
    }
}
