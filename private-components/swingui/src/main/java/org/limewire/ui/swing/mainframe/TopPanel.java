package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectionListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SearchBar;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JPanel implements SearchNavigator {
    
    private final SearchBar searchBar;
    private final FancyTabList searchList;
    private final Navigator navigator;

    @Inject
    public TopPanel(final SearchHandler searchHandler, Navigator navigator) {
        this.navigator = navigator;
        
        setMinimumSize(new Dimension(0, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setPreferredSize(new Dimension(1024, 40));
        
        this.searchBar = new SearchBar();
        searchBar.setColumns(1); // This is required to give it a fixed size.
        searchBar.setName("TopPanel.searchBar");
        final JComboBox combo = new JComboBox(SearchCategory.values());
        combo.setName("TopPanel.combo");
        JLabel search = new JLabel(I18n.tr("Search"));
        search.setName("TopPanel.SearchLabel");
        
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                if(value != null) {
                    switch((SearchCategory)value) {
                    case ALL: value = I18n.tr("All"); break;
                    case AUDIO: value = I18n.tr("Audio"); break;
                    case DOCUMENTS: value = I18n.tr("Documents"); break;
                    case IMAGES: value = I18n.tr("Images"); break;
                    case VIDEO: value = I18n.tr("Video"); break;
                    default:
                        throw new IllegalArgumentException("invalid category: " + value);
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        searchBar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchHandler.doSearch(new DefaultSearchInfo(searchBar.getText(), (SearchCategory)combo.getSelectedItem()));
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
        
        gbc.ipadx = 150;
        add(searchBar, gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.ipadx = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 0, 0);
        searchList = new FancyTabList();
        searchList.setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        searchList.setSelectionPainter(new RectanglePainter<JXPanel>(2, 2, 0, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY));
        searchList.setMaxActionsToList(3);
        add(searchList, gbc);
    }
    
    @Override
    public boolean requestFocusInWindow() {
        return searchBar.requestFocusInWindow();
    }
    
    @Override
    public NavItem addSearch(String title, JComponent search) {
        final NavItem item = navigator.addNavigablePanel(NavCategory.SEARCH, title, search, true);
        final SearchAction action = new SearchAction(item);
        searchList.addActionAt(action, 0);
        return new NavItem() {
            @Override
            public String getName() {
                return item.getName();
            }
            
            @Override
            public void remove() {
                item.remove();
                searchList.removeAction(action);
            }
            
            @Override
            public void select() {
                action.actionPerformed(null);
            }
        };
    }
    
    private class SearchAction extends AbstractAction {
        private final NavItem item;
        public SearchAction(NavItem item) {
            super(item.getName());
            this.item = item;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            item.select();
            putValue(Action.SELECTED_KEY, true);
            navigator.addNavListener(new NavSelectionListener() {
                @Override
                public void navItemSelected(NavCategory category, NavItem navItem) {
                    if(navItem != item) {
                        navigator.removeNavListener(this);
                        putValue(Action.SELECTED_KEY, false);
                    }
                }
            });
        }
    }
    
    
}
