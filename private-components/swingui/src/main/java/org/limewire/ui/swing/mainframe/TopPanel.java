package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.SearchBar;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.components.TextFieldWithEnterButton;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.painter.TopPanelPainter;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class TopPanel extends JXPanel implements SearchNavigator {
    
    private final FancyTabList searchList;
    private final Navigator navigator;
    private final TextFieldWithEnterButton textField;

    // TODO: This needs to be controlled by some other code eventually.
    private final boolean programEnabled = true;
        
    private final NavItem homeNav;
    
    @Inject
    public TopPanel(final SearchHandler searchHandler, Navigator navigator,
                    @Named("friendLibraries") AutoCompleteDictionary friendLibraries,
                    HomePanel homePanel,
                    StorePanel storePanel) {
        GuiUtils.assignResources(this);
        this.navigator = navigator;
        
        setName("TopPanel");
        
        setBackgroundPainter(new TopPanelPainter());
        
        textField = new TextFieldWithEnterButton(I18n.tr("Search..."), friendLibraries);
        textField.setMaximumSize(120);
        textField.setName("TopPanel.searchInput");
        
        final JComboBox combo = new JComboBox(SearchCategory.values());
        combo.removeItem(SearchCategory.OTHER);
        if (!programEnabled)
            combo.removeItem(SearchCategory.PROGRAM);

        combo.setName("TopPanel.combo");
        JLabel search = new JLabel(I18n.tr("Search"));
        search.setName("TopPanel.SearchLabel");

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
                
                if (value != null) {
                    switch((SearchCategory) value) {
                    case ALL: value = I18n.tr("All"); break;
                    case AUDIO: value = I18n.tr("Music"); break;
                    case DOCUMENT: value = I18n.tr("Documents"); break;
                    case IMAGE: value = I18n.tr("Images"); break;
                    case PROGRAM: value = I18n.tr("Programs"); break;
                    case VIDEO: value = I18n.tr("Videos"); break;
                    case OTHER: value = I18n.tr("Other"); break;
                    default:
                        throw new IllegalArgumentException(
                            "invalid category: " + value);
                    }
                }
                
                return super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            }
        });
        
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = textField.getText();
                searchHandler.doSearch(
                    new DefaultSearchInfo(searchText,
                        (SearchCategory) combo.getSelectedItem()));
            }
        });
        
        homeNav = navigator.createNavItem(NavCategory.LIMEWIRE, HomePanel.NAME, homePanel);
        NavItem storeNav = navigator.createNavItem(NavCategory.LIMEWIRE, StorePanel.NAME, storePanel);
        JButton homeButton = new IconButton(NavigatorUtils.getNavAction(homeNav));
        homeButton.setName("TopPanel.homeButton");
        homeButton.setText(I18n.tr("Home"));
        JButton storeButton = new IconButton(NavigatorUtils.getNavAction(storeNav));
        storeButton.setName("TopPanel.storeButton");
        storeButton.setText(I18n.tr("Store"));
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 1;
                
        gbc.insets = new Insets(5, 10, 2, 0);
        gbc.fill = GridBagConstraints.NONE;
        add(homeButton, gbc);
        
        gbc.insets = new Insets(5, 10, 2, 0);
        gbc.fill = GridBagConstraints.NONE;
        add(storeButton, gbc);
        
        gbc.insets = new Insets(5, 50, 0, 5);
        gbc.fill = GridBagConstraints.NONE;
        add(search, gbc);
        
        gbc.insets = new Insets(5, 0, 0, 0);
                
        add(new SearchBar(combo, textField));
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        searchList = new FancyTabList();
        searchList.setCloseAllText(I18n.tr("Close all searches"));
        searchList.setCloseOneText(I18n.tr("Close search"));
        searchList.setCloseOtherText(I18n.tr("Close other searches"));
        searchList.setFixedLayout(50, 120, 120);
        searchList.setRemovable(true);
        searchList.setPreferredSize(
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        searchList.setSelectionPainter(new RectanglePainter<JXButton>(
            2, 2, 0, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY));
        searchList.setHighlightPainter(new RectanglePainter<JXButton>(
            2, 2, 0, 2, 5, 5, true, Color.YELLOW, 0f, Color.LIGHT_GRAY));
        searchList.setMaxVisibleTabs(3);
        searchList.setMaxTotalTabs(10);
        searchList.setName("TopPanel.SearchList");
        add(searchList, gbc);
    };

    @Override
    public boolean requestFocusInWindow() {
        return textField.requestFocusInWindow();
    }

    @Override
    public SearchNavItem addSearch(
        String title, JComponent searchPanel, final Search search) {
        
        final NavItem item = navigator.createNavItem(
            NavCategory.SEARCH, title, searchPanel);
        final SearchAction action = new SearchAction(item);
        search.addSearchListener(action);

        final Action moreTextAction = new NoOpAction();
        final Action repeat = new AbstractAction(I18n.tr("Repeat search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                search.repeat();
            }
        };
        
        final TabActionMap actionMap = new TabActionMap(
            action, action, moreTextAction, Collections.singletonList(repeat));
        
        searchList.addTabActionMapAt(actionMap, 0);
        
        item.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                searchList.removeTabActionMap(actionMap);
                search.stop();
            }

            @Override
            public void itemSelected(boolean selected) {
                textField.setText(item.getId());
                action.putValue(Action.SELECTED_KEY, selected);
            }
        });
        
        return new SearchNavItem() {
            @Override
            public void sourceCountUpdated(int newSourceCount) {
                moreTextAction.putValue(Action.NAME,
                    String.valueOf(newSourceCount));

                if (newSourceCount >= 100) {
                    action.killBusy();
                }
            }
            
            @Override
            public String getId() {
                return item.getId();
            }
            
            @Override
            public void remove() {
                item.remove();
            }
            
            @Override
            public void select() {
                item.select();
            }
            
            @Override
            public void addNavItemListener(NavItemListener listener) {
                item.addNavItemListener(listener);
            }
            
            @Override
            public void removeNavItemListener(NavItemListener listener) {
                item.removeNavItemListener(listener);
            }
            
            @Override
            public boolean isSelected() {
                return item.isSelected();
            }
        };
    }
    
    private class SearchAction extends AbstractAction implements SearchListener {
        private final NavItem item;
        private Timer busyTimer;
        
        public SearchAction(NavItem item) {
            super(item.getId());
            this.item = item;

            // Make sure this syncs up with any changes in selection.
            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if (evt.getNewValue().equals(Boolean.TRUE)) {
                            SearchAction.this.item.select();
                        }
                    }
                }
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(TabActionMap.SELECT_COMMAND)) {
                item.select();
            } else if (e.getActionCommand().equals(TabActionMap.REMOVE_COMMAND)) {
                item.remove();
            }
        }

        @Override
        public void handleSearchResult(SearchResult searchResult) {
        }
        
        @Override
        public void handleSponsoredResults(List<SponsoredResult> sponsoredResults) {
            // do nothing
        }
        
        void killBusy() {
            busyTimer.stop();
            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
        }
        
        @Override
        public void searchStarted() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    busyTimer = new Timer(30000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
                        }
                    });
                    busyTimer.setRepeats(false);
                    busyTimer.start();
                    putValue(TabActionMap.BUSY_KEY, Boolean.TRUE);
                }
            });
        }
        
        @Override
        public void searchStopped() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    killBusy();
                }
            });
        }
    }

    public void goHome() {
        homeNav.select();
    }
}