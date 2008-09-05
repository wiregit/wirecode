package org.limewire.ui.swing.mainframe;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.TextFieldWithEnterButton;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

@Singleton
class TopPanel extends JPanel implements SearchNavigator {
    
    private final FancyTabList searchList;
    private final Navigator navigator;
    private final TextFieldWithEnterButton textField;
    
    @Resource private Icon enterUpIcon;
    @Resource private Icon enterOverIcon;
    @Resource private Icon enterDownIcon;

    private List<SearchListener> listeners = new ArrayList<SearchListener>();
    
    @Inject
    public TopPanel(final SearchHandler searchHandler, Navigator navigator) {
        this.navigator = navigator;
        
        setMinimumSize(new Dimension(0, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setPreferredSize(new Dimension(1024, 40));
        
	    GuiUtils.assignResources(this);
        textField = new TextFieldWithEnterButton(
            15, "Search...", enterUpIcon, enterOverIcon, enterDownIcon);
        textField.setMaximumSize(120);
        
        final JComboBox combo = new JComboBox(SearchCategory.values());
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
                Search search = searchHandler.doSearch(
                    new DefaultSearchInfo(
                        textField.getText(),
                        (SearchCategory) combo.getSelectedItem()));

                for (SearchListener listener : listeners) {
                    search.addSearchListener(listener);
                }
            }
        });
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 1;
        
        gbc.insets = new Insets(5, 1, 0, 5);
        gbc.fill = GridBagConstraints.NONE;
        add(search, gbc);
        
        gbc.insets = new Insets(5, 0, 0, 5);
        add(combo, gbc);
        
        add(textField, gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 0, 0);
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
    
    /**
     * Adds a listener that will be notified as each search result comes in.
     * @param listener the SearchListener
     */
    public void addSearchListener(SearchListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean requestFocusInWindow() {
        return textField.requestFocusInWindow();
    }

    @Override
    public SearchNavItem addSearch(
        String title, JComponent searchPanel, final Search search) {
        
        final NavItem item = navigator.addNavigablePanel(
            NavCategory.SEARCH, title, searchPanel, false);
        final SearchAction action = new SearchAction(item, search);
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
        
        return new SearchNavItem() {
            @Override
            public String getName() {
                return item.getName();
            }
            
            @Override
            public void remove() {
                searchList.removeTabActionMap(actionMap);
                action.remove();
            }
            
            @Override
            public void select() {
                action.putValue(Action.SELECTED_KEY, true);
            }
            
            @Override
            public void sourceCountUpdated(int newSourceCount) {
                moreTextAction.putValue(Action.NAME,
                    String.valueOf(newSourceCount));
                if (newSourceCount >= 100) {
                    action.killBusy();
                }
            }
        };
    }
    
    private class SearchAction extends AbstractAction implements SearchListener {
        private final NavItem item;
        private final Search search;
        private Timer busyTimer;
        
        public SearchAction(NavItem item, Search search) {
            super(item.getName());
            this.item = item;
            this.search = search;

            // Make sure this syncs up with any changes in selection.
            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if (evt.getNewValue().equals(Boolean.TRUE)) {
                            select();
                        }
                    }
                }
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(TabActionMap.SELECT_COMMAND)) {
                select();
            } else if (e.getActionCommand().equals(TabActionMap.REMOVE_COMMAND)) {
                remove();
            }
        }

        @Override
        public void handleSearchResult(SearchResult searchResult) {
            // TODO: RMV Should this method do anything?
            //System.out.println(
            //    "TopPanel.SearchAction.handleSearchResult: searchResult = "
            //    + searchResult);
        }
        
        @Override
        public void handleSponsoredResults(List<SponsoredResult> sponsoredResults) {
            // do nothing
        }
        
        void killBusy() {
            busyTimer.stop();
            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
        }
        
        public void remove() {
            item.remove();
            search.stop();
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

        public void select() {
            item.select();
            textField.setText(item.getName());
            navigator.addNavListener(new NavSelectionListener() {
                @Override
                public void navItemSelected(
                    NavCategory category, NavItem navItem) {
                    if (navItem != item) {
                        navigator.removeNavListener(this);
                        putValue(Action.SELECTED_KEY, false);
                    }
                }
            });
        }
    }
}