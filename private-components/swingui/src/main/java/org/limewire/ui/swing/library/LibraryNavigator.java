package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.components.ShiftedIcon;
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
    
    @Resource private Color selectedBackground;
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font textFont;
    @Resource private Color textColor;

    @Inject
    LibraryNavigator(final Navigator navigator, LibraryManager libraryManager,
            RemoteLibraryManager remoteLibraryManager,
            MyLibraryFactory myLibraryFactory, 
            final FriendLibraryFactory friendLibraryFactory) {
        GuiUtils.assignResources(this);

        setOpaque(false);

        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");

        setLayout(new MigLayout("insets 0, gap 0"));
        add(titleLabel, "growx, alignx left, aligny top,  wrap");
       
        addNavPanel(new NavPanel(Me.ME, createMyCategories(navigator, myLibraryFactory, libraryManager.getLibraryManagedList().getSwingModel())));

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                Friend friend = item.getFriend();
                addNavPanel(new NavPanel(friend,
                            createFriendCategories(navigator, friend,
                                    friendLibraryFactory, item.getSwingModel())));
            }
            @Override
            protected void itemRemoved(FriendLibrary item) {
                removeNavPanelForFriend(item.getFriend());
            }
            @Override
            protected void itemUpdated(FriendLibrary item) {
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
    
    private void addNavPanel(NavPanel panel) {
        navPanels.add(panel);
        add(panel, "alignx left, aligny top, growx, wrap");
    }
    
    private void moveDown() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                if(!panel.incrementSelection()) {
                    if(iter.hasNext()) {
                        iter.next().selectFirst();
                    }
                }
                break;
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
                    if(iter.hasPrevious()) {
                        iter.previous().selectLast();
                    }
                }
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
        repaint(); // Must forcibly paint, otherwise might not redraw w/o panel.
    }
    
    private void collapseOthersAndExpandThis(Friend friend) {
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
    
    private Action decorateAction(Action action, NavItem navItem, final Disposable disposable,
            final Category category, final FilterList<?> filterList, final Friend friend) {
        action.putValue(Action.NAME, I18n.tr(category.toString()));
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
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
        switch (category) {
        case AUDIO:
            action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, audioIcon));
            break;
        case DOCUMENT:
            action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, documentIcon));
            break;
        case IMAGE:
            action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, imageIcon));
            break;
        case OTHER:
            action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, otherIcon));
            break;
        case PROGRAM:
            action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, appIcon));
            break;
        case VIDEO:
            action.putValue(Action.SMALL_ICON, new ShiftedIcon(26, 0, videoIcon));
            break;
        }
        return action;
    }
    
    private class NavPanel extends JXPanel {
        private final CategoriesPanel categories;
        private final Friend friend;
        private final ActionLabel categoryLabel;
        
        public NavPanel(Friend friend, Map<Category, Action> actions) {
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
            categoryLabel.setForeground(textColor);
            categoryLabel.setFont(textFont);
            add(categoryLabel, "gapbefore 12, gaptop 7, grow, wrap");
            add(categories, "grow, wrap"); // the gap here is implicit in the width of the icon
                                           // see decorateAction
            
        }
        
        public void selectFirst() {
            categories.selectFirst();
        }

        public boolean incrementSelection() {
            return categories.incrementSelection();
        }

        public void selectLast() {
            categories.selectLast();
        }

        public boolean decrementSelection() {
            return categories.decrementSelection();
        }

        public boolean hasSelection() {
            return categories.hasSelection();
        }

        public void expand() {
            categories.setCollapsed(false);
            GuiUtils.setActionHandDrawingDisabled(categoryLabel, true);
        }

        public void dispose() {
            categories.dispose();
        }
        
        public void collapse() {
            categories.setCollapsed(true);
            GuiUtils.setActionHandDrawingDisabled(categoryLabel, false);
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
            
            JXPanel panel = new JXPanel(new MigLayout("gap 0, insets 0, fill")) {
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            for(Category category : categories.keySet()) {
                Action action = categories.get(category);
                action.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if(evt.getPropertyName().equals(Action.SELECTED_KEY) && Boolean.TRUE.equals(evt.getNewValue())) {
                            lastSelectedAction = (Action)evt.getSource();
                            requestFocus();
                        }
                    }
                });
                CategoryLabel label = new CategoryLabel(action);
                label.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        requestFocus();
                    }
                });
                panel.add(label, "grow, wrap");
            }
            setContentPane(panel);
            // The below is a hack in order to set the viewport transparent.
            JViewport viewport = (JViewport)getComponent(0);
            viewport.setOpaque(false);
                        
            getActionMap().put(MoveDown.KEY, new MoveDown());
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), MoveDown.KEY);
            
            getActionMap().put(MoveUp.KEY, new MoveUp());
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), MoveUp.KEY);
        }
        
        public boolean hasSelection() {
            for(Action action : categories.values()) {
                if(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
                    return true;
                }
            }
            return false;
        }

        public boolean decrementSelection() {
            List<Action> actions = new ArrayList<Action>(categories.values());
            ListIterator<Action> iter = actions.listIterator();
            while(iter.hasNext()) {
                Action action = iter.next();
                if(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
                    iter.previous(); // back us up a step.
                    if(iter.hasPrevious()) {
                        iter.previous().actionPerformed(null);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        public void selectLast() {
            LinkedList<Action> actions = new LinkedList<Action>(categories.values());
            actions.getLast().actionPerformed(null);
        }

        public boolean incrementSelection() {
            List<Action> actions = new ArrayList<Action>(categories.values());
            ListIterator<Action> iter = actions.listIterator();
            while(iter.hasNext()) {
                Action action = iter.next();
                if(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
                    if(iter.hasNext()) {
                        iter.next().actionPerformed(null);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        public void selectFirst() {
            categories.values().iterator().next().actionPerformed(null);
        }

        public void dispose() {
            for(Action action : categories.values()) {
                NavItem item = (NavItem)action.getValue(NavigatorUtils.NAV_ITEM);
                item.remove();
            }
        }

        public void ensureSelected() {
            if(lastSelectedAction == null) {
                categories.get(Category.AUDIO).putValue(Action.SELECTED_KEY, true);
            } else {
                lastSelectedAction.putValue(Action.SELECTED_KEY, true);
            }
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
            setPreferredSize(new Dimension(500, 22));
            
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
