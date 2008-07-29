package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

/**
 * This class is a panel that contains components for filtering and sorting
 * search results.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
class SortAndFilterPanel extends JXPanel {

    private static final int FILTER_WIDTH = 15;

    private final JComboBox sortBox = new JComboBox(new String[] { "Sources", "Relevance", "Size",
            "File Extension" });

    private final JTextField filterBox = new FilteredTextField(FILTER_WIDTH);

    SortAndFilterPanel() {
        setBackground(Color.LIGHT_GRAY);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(3, 2, 2, 10);
        add(filterBox, gbc);

        JLabel sortLabel = new JLabel("Sort by:");
        // FontUtils.changeSize(sortLabel, 1);
        // FontUtils.changeStyle(sortLabel, Font.BOLD);
        sortLabel.setForeground(Color.WHITE);
        gbc.insets.right = 5;
        add(sortLabel, gbc);

        add(sortBox, gbc);
    }

    public EventList<VisualSearchResult> getSortedAndFilteredList(
            EventList<VisualSearchResult> visualSearchResults) {

        VisualSearchResultTextFilterator filterator =
            new VisualSearchResultTextFilterator();
        TextComponentMatcherEditor<VisualSearchResult> editor =
            new TextComponentMatcherEditor<VisualSearchResult>(
                filterBox, filterator, true);
        EventList<VisualSearchResult> filteredList =
            new FilterList<VisualSearchResult>(visualSearchResults, editor);
        final SortedList<VisualSearchResult> sortedList =
            new SortedList<VisualSearchResult>(filteredList, null);

        ItemListener listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (e.getItem().equals("Relevance")) {
                        sortedList.setComparator(null);
                    } else if (e.getItem().equals("Size")) {
                        sortedList.setComparator(new Comparator<VisualSearchResult>() {
                            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                                return ((Long) o2.getSize()).compareTo(o1.getSize());
                            }
                        });
                    } else if (e.getItem().equals("File Extension")) {
                        sortedList.setComparator(new Comparator<VisualSearchResult>() {
                            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                                // TODO: Support locales better.
                                return o2.getFileExtension().compareToIgnoreCase(
                                        o1.getFileExtension());
                            }
                        });
                    } else if (e.getItem().equals("Sources")) {
                        sortedList.setComparator(new Comparator<VisualSearchResult>() {
                            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                                return ((Integer) o2.getSources().size()).compareTo(o1.getSources()
                                        .size());
                            }
                        });
                    }
                }
            }
        };
        listener.itemStateChanged(new ItemEvent(sortBox, ItemEvent.ITEM_STATE_CHANGED, sortBox
                .getSelectedItem(), ItemEvent.SELECTED));
        sortBox.addItemListener(listener);
        return sortedList;
    }

    private static class VisualSearchResultTextFilterator implements
            TextFilterator<VisualSearchResult> {
        @Override
        public void getFilterStrings(List<String> baseList, VisualSearchResult element) {
            baseList.add(element.getDescription());
            baseList.add(element.getCategory().toString());
            baseList.add(element.getFileExtension());
            baseList.add(String.valueOf(element.getSize()));
        }
    }

}
