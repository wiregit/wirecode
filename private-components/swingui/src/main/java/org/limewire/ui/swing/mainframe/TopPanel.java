package org.limewire.ui.swing.mainframe;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.nav.SearchBar;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JPanel {
    
    private final SearchBar searchBar;

    @Inject
    public TopPanel(final SearchHandler searchHandler) {
        setMinimumSize(new Dimension(0, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setPreferredSize(new Dimension(1024, 40));
        
        this.searchBar = new SearchBar();
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
        gbc.insets = new Insets(5, 1, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(search, gbc);
        
        gbc.insets = new Insets(5, 0, 5, 5);
        add(combo, gbc);
        
        gbc.ipadx = 150;
        add(searchBar, gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        add(Box.createGlue(), gbc);
    }
    
    @Override
    public boolean requestFocusInWindow() {
        return searchBar.requestFocusInWindow();
    }

}
