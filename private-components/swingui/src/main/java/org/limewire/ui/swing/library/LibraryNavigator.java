package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JViewport;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
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
    private final List<FriendLibraryNavPanel> friendNavPanels = new ArrayList<FriendLibraryNavPanel>();

    @Inject
    LibraryNavigator(Navigator navigator, LibraryManager libraryManager,
            ListenerSupport<FriendRemoteLibraryEvent> friendLibrarySupport,
            MyLibraryPanel myLibraryPanel, final FriendLibraryPanel friendLibraryPanel) {
        GuiUtils.assignResources(this);

        setOpaque(false);

        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");

        setLayout(new MigLayout("insets 0, gap 0"));
        add(titleLabel, "growx, alignx left, aligny top,  wrap");
        
        final NavItem meNav = navigator.createNavItem(NavCategory.LIBRARY, "Me", myLibraryPanel.getComponent());
        final NavItem friendNav = navigator.createNavItem(NavCategory.LIBRARY, "Friends", friendLibraryPanel.getComponent());
        
        add(new MyLibraryNavPanel(meNav, myLibraryPanel, libraryManager.getLibraryManagedList().getSwingModel()),
                "alignx left, aligny top, growx, wrap");

        friendLibrarySupport.addListener(new EventListener<FriendRemoteLibraryEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendRemoteLibraryEvent event) {
                FriendLibraryNavPanel panel;
                switch (event.getType()) {
                case FRIEND_LIBRARY_ADDED:
                    panel = new FriendLibraryNavPanel(event.getFriend(), friendNav, friendLibraryPanel, event.getFileList().getSwingModel());
                    friendNavPanels.add(panel);
                    add(panel, "alignx left, aligny top, growx, wrap");
                    validate();
                    break;
                case FRIEND_LIBRARY_REMOVED:
                    for(Iterator<FriendLibraryNavPanel> i = friendNavPanels.iterator(); i.hasNext(); ) {
                        panel = i.next();
                        if(panel.getFriend().getId().equals(event.getFriend().getId())) {
                            i.remove();
                            remove(panel);
                            panel.dispose();
                            validate();
                            break;
                        }
                    }
                    if(friendNav.isSelected() && friendNavPanels.isEmpty()) {
                        meNav.select();
                    }
                    break;
                }
            }
        });
        
        friendNav.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                throw new IllegalStateException("cannot remove friends library.");
            }

            @Override
            public void itemSelected(boolean selected) {
                if (!selected) {
                    for (FriendLibraryNavPanel nav : friendNavPanels) {
                        nav.collapse();
                    }
                }
            }
        });
    }
    
    private static class MyLibraryNavPanel extends JXPanel {
        public MyLibraryNavPanel(NavItem navItem, LibraryPanel<LocalFileItem> libraryPanel, EventList<LocalFileItem> eventList) {
            super(new MigLayout("insets 0, gap 0, fill"));
            setOpaque(false);
        
            final CategoriesPanel categories = new CategoriesPanel<LocalFileItem>(libraryPanel, eventList);
            categories.setAnimated(false);
            categories.setCollapsed(true);
            categories.setAnimated(true);
            JXLabel me = new ActionLabel(NavigatorUtils.getNavAction(navItem), false);
            me.setText(I18n.tr("Me"));
            navItem.addNavItemListener(new NavItemListener() {
                @Override
                public void itemRemoved() {
                    throw new IllegalStateException("cannot remove my library.");
                }
                
                @Override
                public void itemSelected(boolean selected) {
                    categories.ensureSelected();
                    categories.setCollapsed(!selected);
                }
            });
            add(me, "gapbefore 5, grow, wrap");
            add(categories, "gapbefore 10, grow, wrap");
        }
    }
    
    private void collapseOtherFriendNavs(FriendLibraryNavPanel selected) {
        for (FriendLibraryNavPanel nav : friendNavPanels) {
            if (nav != selected) {
                nav.collapse();
            }
        }
    }
    
    private class FriendLibraryNavPanel extends JXPanel {
        private final Friend friend;
        private final CategoriesPanel categories;
        
        public FriendLibraryNavPanel(Friend friend, NavItem navItem, FriendLibraryPanel libraryPanel, EventList<RemoteFileItem> eventList) {
            super(new MigLayout("insets 0, gap 0, fill"));
            setOpaque(false);
        
            this.friend = friend;
            this.categories = new CategoriesPanel<RemoteFileItem>(libraryPanel, eventList);
            categories.setAnimated(false);
            categories.setCollapsed(true);
            categories.setAnimated(true);
            ActionLabel me = new ActionLabel(NavigatorUtils.getNavAction(navItem), false);
            me.setText(friend.getRenderName());
            me.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    expand();
                }
            });
            add(me, "gapbefore 5, grow, wrap");
            add(categories, "gapbefore 10, grow, wrap");
        }
        
        public Friend getFriend() {
            return friend;
        }
        
        public CategoriesPanel getCategories() {
            return categories;
        }
        
        public void collapse() {
            categories.setCollapsed(true);
        }
        
        public void expand() {        
            categories.ensureSelected();
            categories.setCollapsed(false);
            collapseOtherFriendNavs(FriendLibraryNavPanel.this);
        }
        
        public void dispose() {
            categories.dispose();
        }

        public void setName(String name) {
            
        }
    }
    
    private static class CategoriesPanel<T extends FileItem> extends JXCollapsiblePane {
        private final Map<Category, CategoryButton> categories;
        private Category selected = null;
        
        public CategoriesPanel(LibraryPanel<T> libraryPanel, EventList<T> eventList) {
            setOpaque(false);
            this.categories = new EnumMap<Category, CategoryButton>(Category.class);
            for(Category category : Category.values()) {
                categories.put(category, 
                               new CategoryButton<T>(libraryPanel, eventList, category, this));
            }
            
            JXPanel panel = new JXPanel(new MigLayout("gap 0, insets 0, fill")) {
                @Override
                public boolean isOpaque() {
                    return false;
                }
            };
            for(Category category : Category.getCategoriesInOrder()) {
                panel.add(categories.get(category), "grow, wrap");
            }
            setContentPane(panel);
            // The below is a hack in order to set the viewport transparent.
            JViewport viewport = (JViewport)getComponent(0);
            viewport.setOpaque(false);
        }
        
        public void ensureSelected() {
            if(selected == null) {
                categories.get(Category.AUDIO).getAction().putValue(Action.SELECTED_KEY, true);
            } else {
                for(CategoryButton button : categories.values()) {
                    // Refresh the selection.
                    if(button.isSelected()) {
                        button.refreshSelection();
                    }
                }
            }
        }
        
        public void setSelected(Category category) {
            this.selected = category;
            for(CategoryButton button : categories.values()) {
                button.getAction().putValue(Action.SELECTED_KEY, selected == button.getCategory());
            }
        }
        
        public void dispose() {
            for(CategoryButton button : categories.values()) {
                button.dispose();
            }
        }
    }
    
    private static class CategoryButton<T extends FileItem> extends ActionLabel {
        private final Category category;
        private final FilterList<T> eventList;
        private final LibraryPanel<T> libraryPanel;
        private final CategoriesPanel<T> categoriesPanel;
        
        public CategoryButton(LibraryPanel<T> libPanel, EventList<T> evList,
                Category cat, CategoriesPanel<T> catPanel) {
            super(new AbstractAction(I18n.tr(cat.toString())) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    putValue(SELECTED_KEY, true);
                }
            }, false);
            this.libraryPanel = libPanel;      
            this.category = cat;
            this.eventList = GlazedListsFactory.filterList(evList, new CategoryFilter(category));
            this.categoriesPanel = catPanel;
            
            setFont(new Font("Arial", Font.PLAIN, 11));
            setForeground(Color.decode("#313131"));
            
            getAction().addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(evt.getNewValue().equals(Boolean.TRUE)) {
                            refreshSelection();
                            setBackground(Color.decode("#93AAD1"));
                            setForeground(Color.WHITE);
                            FontUtils.bold(CategoryButton.this);
                            setOpaque(true);
                        } else {
                            setOpaque(false);
                            setForeground(Color.decode("#313131"));
                            FontUtils.plain(CategoryButton.this);
                        }
                    }
                }
            });
        }
        
        public Category getCategory() {
            return category;
        }
        
        public boolean isSelected() {
            return Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY));
        }
        
        public void refreshSelection() {
            libraryPanel.setCategory(category, eventList);
            categoriesPanel.setSelected(category);
        }
        
        public void dispose() {
            eventList.dispose();
        }
    }
}
