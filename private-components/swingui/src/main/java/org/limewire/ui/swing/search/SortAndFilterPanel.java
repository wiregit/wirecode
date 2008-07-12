package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.FontUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

class SortAndFilterPanel extends JXPanel {
    
    private final JComboBox sortBox;
    private final JTextField filterBox;
    
    SortAndFilterPanel() {
        this.sortBox = new JComboBox(new String[] { "Relevance", "Size", "File Extension" } );
        this.filterBox = new JTextField();
        setBackground(new Color(100, 100, 100));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        JLabel sortLabel = new JLabel("Sort by");
        FontUtils.changeFontSize(sortLabel, 1);
        FontUtils.changeStyle(sortLabel, Font.BOLD);
        sortLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(5, 10, 5, 5);
        add(sortLabel, gbc);
        gbc.insets = new Insets(5, 0, 5, 15);
        add(sortBox, gbc);
        
        JLabel filterLabel = new JLabel("Filter by");
        FontUtils.changeFontSize(filterLabel, 1);
        FontUtils.changeStyle(filterLabel, Font.BOLD);
        filterLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(5, 0, 5, 5);
        add(filterLabel, gbc);
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.ipadx = 150;
        add(filterBox, gbc);
        
        gbc.weightx = 1;
        gbc.ipady = 42;
        gbc.ipadx = 0;
        add(Box.createGlue(), gbc);
    }

    public EventList<VisualSearchResult> getSortedAndFilteredList(EventList<VisualSearchResult> visualSearchResults) {
        EventList<VisualSearchResult> filteredList = new FilterList<VisualSearchResult>(visualSearchResults, new TextComponentMatcherEditor<VisualSearchResult>(filterBox, new VisualSearchResultTextFilterator(), true));
        final SortedList<VisualSearchResult> sortedList = new SortedList<VisualSearchResult>(filteredList, null);
        sortBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    if(e.getItem().equals("Relevance")) {
                        sortedList.setComparator(null);
                    } else if(e.getItem().equals("Size")) {
                        sortedList.setComparator(new Comparator<VisualSearchResult>() {
                            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                                return ((Long)o1.getSize()).compareTo(o2.getSize());
                            }
                        });
                    } else if(e.getItem().equals("File Extension")) {
                        sortedList.setComparator(new Comparator<VisualSearchResult>() {
                            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                                // TODO: Support locales better.
                                return o1.getFileExtension().compareToIgnoreCase(o2.getFileExtension());
                            }
                        });
                    }
                }
            }
        });
        return sortedList;
    }
    
    private static class VisualSearchResultTextFilterator implements TextFilterator<VisualSearchResult> {
        @Override
        public void getFilterStrings(List<String> baseList, VisualSearchResult element) {
            baseList.add(element.getDescription());
            baseList.add(element.getCategory().toString());
            baseList.add(element.getFileExtension());
            baseList.add(String.valueOf(element.getSize()));
        }
    }

}
