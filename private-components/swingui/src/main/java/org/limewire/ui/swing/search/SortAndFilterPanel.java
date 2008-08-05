package org.limewire.ui.swing.search;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a panel that contains components for filtering and sorting
 * search results.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
class SortAndFilterPanel extends JXPanel {

    private static final int FILTER_WIDTH = 10;
    
    private List<ModeListener> modeListeners = new ArrayList<ModeListener>();
    
    @Resource private Icon listViewPressedIcon;
    @Resource private Icon listViewUnpressedIcon;
    @Resource private Icon tableViewPressedIcon;
    @Resource private Icon tableViewUnpressedIcon;

    private final JComboBox sortBox = new JComboBox(new String[] {
        "Sources", "Relevance", "Size", "File Extension" });
    
    private final JLabel sortLabel = new JLabel("Sort by:");
    private final JTextField filterBox = new FilteredTextField(FILTER_WIDTH);
    private final JToggleButton listViewToggleButton = new JToggleButton();
    private final JToggleButton tableViewToggleButton = new JToggleButton();

    SortAndFilterPanel() {
        GuiUtils.assignResources(this);
        
        setBackground(Color.LIGHT_GRAY);

        sortLabel.setForeground(Color.WHITE);
        // TODO: RMV Are the following lines needed?
        // FontUtils.changeSize(sortLabel, 1);
        // FontUtils.changeStyle(sortLabel, Font.BOLD);
        
        configureViewButtons();
        
        layoutComponents();
    }

    public synchronized void addModeListener(ModeListener listener) {
        modeListeners.add(listener);
    }
    
    private void configureViewButtons() {
        Insets insets = new Insets(0, 0, 0, 0);
        
        final SortAndFilterPanel outerThis = this;
        
        listViewToggleButton.setIcon(listViewUnpressedIcon);
        listViewToggleButton.setPressedIcon(listViewPressedIcon);
        listViewToggleButton.setSelected(true);
        listViewToggleButton.setMargin(insets);
        listViewToggleButton.setToolTipText("List View");
        listViewToggleButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    outerThis.notifyModeListeners(ModeListener.Mode.LIST);
                }
            }
        });
        
        tableViewToggleButton.setIcon(tableViewUnpressedIcon);
        tableViewToggleButton.setPressedIcon(tableViewPressedIcon);
        tableViewToggleButton.setMargin(insets);
        tableViewToggleButton.setToolTipText("Table View");
        tableViewToggleButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    outerThis.notifyModeListeners(ModeListener.Mode.TABLE);
                }
            }
        });
        
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewToggleButton);
        viewGroup.add(tableViewToggleButton);
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
        
        ItemEvent itemEvent = new ItemEvent(
            sortBox, ItemEvent.ITEM_STATE_CHANGED,
            sortBox.getSelectedItem(), ItemEvent.SELECTED);
        listener.itemStateChanged(itemEvent);
        sortBox.addItemListener(listener);
        
        return sortedList;
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(3, 2, 2, 10); // top, left, bottom, right
        add(filterBox, gbc);

        gbc.insets.right = 5;
        add(sortLabel, gbc);

        add(sortBox, gbc);
        
        gbc.insets.left = gbc.insets.right = 0;
        add(listViewToggleButton, gbc);
        add(tableViewToggleButton, gbc);
    }

    private synchronized void notifyModeListeners(ModeListener.Mode mode) {
        for (ModeListener listener : modeListeners) {
            listener.setMode(mode);
        }
    }

    public synchronized void removeModeListener(ModeListener listener) {
        modeListeners.remove(listener);
    }
    
    /**
     * Sets the state of the view toggle buttons.
     * @param mode the current mode ... LIST or TABLE
     */
    public void setMode(ModeListener.Mode mode) {
        if (mode == ModeListener.Mode.LIST) {
            listViewToggleButton.setSelected(true);
            tableViewToggleButton.setSelected(false);
        } else if (mode == ModeListener.Mode.TABLE) {
            listViewToggleButton.setSelected(false);
            tableViewToggleButton.setSelected(true);
        }
    }
    
    private static class VisualSearchResultTextFilterator
    implements TextFilterator<VisualSearchResult> {
        @Override
        public void getFilterStrings(
                List<String> baseList, VisualSearchResult element) {
            baseList.add(element.getDescription());
            baseList.add(element.getCategory().toString());
            baseList.add(element.getFileExtension());
            baseList.add(String.valueOf(element.getSize()));
        }
    }
}