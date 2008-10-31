package org.limewire.ui.swing.mainframe;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
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
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.painter.SearchTabSelectionPainter;
import org.limewire.ui.swing.painter.TopPanelPainter;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JXPanel implements SearchNavigator {
    
    private final FancyTabList searchList;
    private final Navigator navigator;
    private final TextFieldWithEnterButton textField;
        
    private final NavItem homeNav;
    
    @Inject
    public TopPanel(final SearchHandler searchHandler, Navigator navigator,
                    final FriendAutoCompleters friendLibraries,
                    HomePanel homePanel,
                    StorePanel storePanel) {
        GuiUtils.assignResources(this);
        this.navigator = navigator;
        
        setName("TopPanel");
        
        setBackgroundPainter(new TopPanelPainter());
        
        textField = new TextFieldWithEnterButton(I18n.tr("Search..."),
                friendLibraries.getDictionary(SearchCategory.forId(SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue())));
        textField.setMaximumSize(120);
        textField.setName("TopPanel.searchInput");
        
        final JComboBox combo = new JComboBox(SearchCategory.values());
        combo.removeItem(SearchCategory.OTHER);
        LibrarySettings.PROGRAM_SHARING_ENABLED.addSettingListener(new SearchSettingListener(LibrarySettings.PROGRAM_SHARING_ENABLED, SearchCategory.PROGRAM, combo));
                
        combo.setSelectedItem(SearchCategory.forId(SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue()));
        combo.setName("TopPanel.combo");
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    SearchCategory category = (SearchCategory)e.getItem();
                    textField.setAutoCompleteDictionary(friendLibraries.getDictionary(category));
                }
            }
        });
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
        homeButton.setIconTextGap(1);
        JButton storeButton = new IconButton(NavigatorUtils.getNavAction(storeNav));
        storeButton.setName("TopPanel.storeButton");
        storeButton.setText(I18n.tr("Store"));
        storeButton.setIconTextGap(1);
        
        searchList = new FancyTabList();
        searchList.setName("TopPanel.SearchList");
        searchList.setMaxVisibleTabs(3);
        searchList.setMaxTotalTabs(10);
        searchList.setCloseAllText(I18n.tr("Close all searches"));
        searchList.setCloseOneText(I18n.tr("Close search"));
        searchList.setCloseOtherText(I18n.tr("Close other searches"));
        searchList.setRemovable(true);
        searchList.setPreferredSize(
                new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        searchList.setSelectionPainter(new SearchTabSelectionPainter());
        searchList.setHighlightPainter(null);
        
        setLayout(new MigLayout("gap 0, insets 0, fill", "", "[center]"));        
        add(homeButton);
        add(storeButton);
        add(search, "gapleft 50");
        add(new SearchBar(combo, textField), "gapleft 5");
        add(searchList, "gapleft 4, gaptop 6, grow");
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
                if(!item.isSelected()) {
                    action.putValue(TabActionMap.NEW_HINT, true);
                }

                if (newSourceCount >= 50) {
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
                select(null);
            }
            
            @Override
            public void select(NavSelectable selectable) {
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
    
    private final class SearchSettingListener implements SettingListener {
        private final JComboBox combo;
        private final BooleanSetting booleanSetting;
        private final SearchCategory searchCategory;
        private boolean oldValue;
        

        /**
         * Listener tracking changes to the given boolean setting, depending on the setting 
         * value the provided search category is added or removed from the given combo box.
         */
        private SearchSettingListener(BooleanSetting booleanSetting, SearchCategory searchCategory, JComboBox combo) {
            this.booleanSetting = booleanSetting;
            this.searchCategory = searchCategory;
            this.combo = combo;
            oldValue = booleanSetting.getValue();
            if (!booleanSetting.getValue()) {
                combo.removeItem(searchCategory);
            }
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            boolean newValue = booleanSetting.getValue();
            if(oldValue != newValue) {
                if(newValue) {
                    combo.addItem(searchCategory);
                } else {
                    combo.removeItem(searchCategory);
                }
                oldValue = newValue;
            }
        }
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
                            putValue(TabActionMap.NEW_HINT, null);
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
                    busyTimer = new Timer(15000, new ActionListener() {
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