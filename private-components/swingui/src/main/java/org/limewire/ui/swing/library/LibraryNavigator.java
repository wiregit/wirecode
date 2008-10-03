package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendRemoteLibraryEvent;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryNavigator extends JPanel {

    private final SectionHeading titleLabel;
    private final List<NavPanel> navPanels = new ArrayList<NavPanel>();

    @Inject
    LibraryNavigator(final Navigator navigator, LibraryManager libraryManager,
            ListenerSupport<FriendRemoteLibraryEvent> friendLibrarySupport,
            MyLibraryFactory myLibraryFactory, 
            final FriendLibraryFactory friendLibraryFactory) {
        GuiUtils.assignResources(this);

        setOpaque(false);

        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");

        setLayout(new MigLayout("insets 0, gap 0"));
        add(titleLabel, "growx, alignx left, aligny top,  wrap");
       
        addNavPanel(new NavPanel(Me.ME, createMyCategories(navigator, myLibraryFactory, libraryManager.getLibraryManagedList().getSwingModel())));

        friendLibrarySupport.addListener(new EventListener<FriendRemoteLibraryEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendRemoteLibraryEvent event) {
                switch (event.getType()) {
                case FRIEND_LIBRARY_ADDED:
                    addNavPanel(new NavPanel(event.getFriend(),
                            createFriendCategories(navigator, event.getFriend(), 
                                    friendLibraryFactory, event.getFileList().getSwingModel())));
                    break;
                case FRIEND_LIBRARY_REMOVED:
                    removeNavPanelForFriend(event.getFriend());
                    break;
                }
            }
        });
        
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
        EnumMap<Category, Action> categories = new EnumMap<Category, Action>(Category.class);
        for(Category category : Category.values()) {
            categories.put(category, createMyCategoryAction(navigator, factory, category, eventList));
        }
        return categories;
    }
    
    private Action createMyCategoryAction(Navigator navigator, MyLibraryFactory factory, Category category, EventList<LocalFileItem> eventList) {
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        JComponent component = factory.createMyLibrary(category, filtered);
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, "__@internal@__" + "/" + category, component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, category, filtered, Me.ME);
    }
    
    private Map<Category, Action> createFriendCategories(Navigator navigator, Friend friend, FriendLibraryFactory factory, EventList<RemoteFileItem> eventList) {
        EnumMap<Category, Action> categories = new EnumMap<Category, Action>(Category.class);
        for(Category category : Category.values()) {
            categories.put(category, createFriendCategoryAction(navigator, friend, factory, category, eventList));
        }
        return categories;
    }
    
    private Action createFriendCategoryAction(Navigator navigator, Friend friend, FriendLibraryFactory factory, Category category, EventList<RemoteFileItem> eventList) {
        FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        JComponent component = factory.createFriendLibrary(friend, category, filtered);
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId() + "/" + category, component);
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
        return action;
    }
    
    private class NavPanel extends JXPanel {
        private final CategoriesPanel categories;
        private final Friend friend;
        
        public NavPanel(Friend friend, Map<Category, Action> actions) {
            super(new MigLayout("insets 0, gap 0, fill"));
            setOpaque(false);
            this.categories = new CategoriesPanel(actions);
            this.friend = friend;
            categories.setAnimated(false);
            categories.setCollapsed(true);
            categories.setAnimated(true);
            ActionLabel me = new ActionLabel(new AbstractAction(friend.getRenderName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    categories.ensureSelected();
                }
            }, false);
            add(me, "gapbefore 5, grow, wrap");
            add(categories, "gapbefore 10, grow, wrap");
            
        }
        
        public void expand() {
            categories.setCollapsed(false);
        }

        public void dispose() {
            categories.dispose();
        }
        
        public void collapse() {
            categories.setCollapsed(true);
        }
        
        public Friend getFriend() {
            return friend;
        }
    }
    
    private static class CategoriesPanel extends JXCollapsiblePane {
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
            for(Category category : Category.getCategoriesInOrder()) {
                Action action = categories.get(category);
                action.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if(evt.getPropertyName().equals(Action.SELECTED_KEY) && Boolean.TRUE.equals(evt.getNewValue())) {
                            lastSelectedAction = (Action)evt.getSource(); 
                        }
                    }
                });
                panel.add(new CategoryLabel(action), "grow, wrap");
            }
            setContentPane(panel);
            // The below is a hack in order to set the viewport transparent.
            JViewport viewport = (JViewport)getComponent(0);
            viewport.setOpaque(false);
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
    
    private static class CategoryLabel extends ActionLabel {
        public CategoryLabel(Action action) {
            super(action, false);
            
            setFont(new Font("Arial", Font.PLAIN, 11));
            setForeground(Color.decode("#313131"));
            
            getAction().addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(evt.getNewValue().equals(Boolean.TRUE)) {
                            setBackground(Color.decode("#93AAD1"));
                            setForeground(Color.WHITE);
                            FontUtils.bold(CategoryLabel.this);
                            setOpaque(true);
                        } else {
                            setOpaque(false);
                            setForeground(Color.decode("#313131"));
                            FontUtils.plain(CategoryLabel.this);
                        }
                    }
                }
            });
        }
    }
    
    private static class Me implements Friend {
        private static final Me ME = new Me();

        @Override
        public String getId() {
            return "__@internal@__";
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
