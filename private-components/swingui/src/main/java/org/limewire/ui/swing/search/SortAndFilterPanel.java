package org.limewire.ui.swing.search;

import java.util.Date;

import static org.limewire.core.api.search.SearchResult.PropertyKey;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
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

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.ChatLoginState;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.AllTableFormat;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class is a panel that contains components for filtering and sorting
 * search results.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class SortAndFilterPanel extends JXPanel {

    private static final String FRIEND_ITEM = "Friend (a-z)";
    private static final String RELEVANCE_ITEM = "Relevance";
    private static final int FILTER_WIDTH = 10;
    
    private ChatLoginState chatLoginState;
    
    private List<ModeListener> modeListeners = new ArrayList<ModeListener>();
    
    @Resource private Icon listViewPressedIcon;
    @Resource private Icon listViewUnpressedIcon;
    @Resource private Icon tableViewPressedIcon;
    @Resource private Icon tableViewUnpressedIcon;

    private final JComboBox sortCombo = new JComboBox();
    
    private final JLabel sortLabel = new JLabel("Sort by:");
    private final JTextField filterBox = new FilteredTextField(FILTER_WIDTH);
    private final JToggleButton listViewToggleButton = new JToggleButton();
    private final JToggleButton tableViewToggleButton = new JToggleButton();
    
    private String sortBy;

    private VisualSearchResultTextFilterator filterator =
        new VisualSearchResultTextFilterator();

    private TextComponentMatcherEditor<VisualSearchResult> editor =
        new TextComponentMatcherEditor<VisualSearchResult>(
            filterBox, filterator, true);

    private boolean repopulatingCombo;

    @Inject
    SortAndFilterPanel(ChatLoginState chatLoginState) {
        this.chatLoginState = chatLoginState;
        GuiUtils.assignResources(this);
        setBackground(Color.LIGHT_GRAY);
        sortLabel.setForeground(Color.WHITE);
        setSearchCategory(SearchCategory.ALL);
        configureViewButtons();
        layoutComponents();

        EventAnnotationProcessor.subscribe(this);
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
        listViewToggleButton.setToolTipText("List view");
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
        tableViewToggleButton.setToolTipText("Table view");
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
    
    public SortedList<VisualSearchResult> getFilteredAndSortedList(
        EventList<VisualSearchResult> simpleList) {

        // Create a list that is filtered by a text field.
        EventList<VisualSearchResult> filteredList =
            new FilterList<VisualSearchResult>(simpleList, editor);

        // Created a SortedList that doesn't have a Comparator yet.
        final SortedList<VisualSearchResult> sortedList =
            new SortedList<VisualSearchResult>(filteredList, null);

        ItemListener listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String item = e.getItem().toString();
                if (!repopulatingCombo
                    && e.getStateChange() == ItemEvent.SELECTED
                    && !item.equals(sortBy)) { // changing sort order
                    Comparator<VisualSearchResult> comparator =
                        getComparator(item);
                    sortedList.setComparator(comparator);
                    sortBy = item;
                }
            }
        };
        sortCombo.addItemListener(listener);
        
        // Trigger the initial sort.
        sortCombo.setSelectedItem(RELEVANCE_ITEM);

        return sortedList;
    }

    private static Comparator<VisualSearchResult> getDateComparator(
        final PropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            public int compare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Date v1 = (Date) vsr1.getProperty(key);
                Date v2 = (Date) vsr2.getProperty(key);
                return v1 == null ? 0 :
                    ascending ? v1.compareTo(v2) : v2.compareTo(v1);
            }
        };
    }

    private static Comparator<VisualSearchResult> getFloatComparator(
        final PropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            public int compare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Float v1 = (Float) vsr1.getProperty(key);
                Float v2 = (Float) vsr2.getProperty(key);
                return v1 == null ? 0 :
                    ascending ? v1.compareTo(v2) : v2.compareTo(v1);
            }
        };
    }

    private static Comparator<VisualSearchResult> getLongComparator(
        final PropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            public int compare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return v1 == null ? 0 :
                    ascending ? v1.compareTo(v2) : v2.compareTo(v1);
            }
        };
    }

    private static Comparator<VisualSearchResult> getStringComparator(
        final PropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            public int compare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = (String) vsr1.getProperty(key);
                String v2 = (String) vsr2.getProperty(key);
                return v1 == null ? 0 :
                    ascending ? v1.compareTo(v2) : v2.compareTo(v1);
            }
        };
    }

    private Comparator<VisualSearchResult> getComparator(String item) {

        if ("Album".equals(item)) {
            return getStringComparator(PropertyKey.ALBUM_TITLE, true);
        }

        if ("Artist".equals(item)) {
            return getStringComparator(PropertyKey.ARTIST_NAME, true);
        }

        if ("Date created (more recent)".equals(item)) {
            return getDateComparator(PropertyKey.DATE_CREATED, false);
        }

        if ("File type".equals(item) || "Type".equals(item)) {
            return new Comparator<VisualSearchResult>() {
                public int compare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    String v1 = AllTableFormat.getMediaType(vsr1);
                    String v2 = AllTableFormat.getMediaType(vsr2);
                    return v1 == null ? 0 : v1.compareTo(v2);
                }
            };
        }

        if (FRIEND_ITEM.equals(item)) {
            return null; // TODO: RMV What to do here?
        }

        if ("Length".equals(item)) {
            return getLongComparator(PropertyKey.LENGTH, true);
        }

        if ("Name".equals(item) || "Filename".equals(item)) {
            return getStringComparator(PropertyKey.NAME, true);
        }

        if ("Quality".equals(item)) {
            return getLongComparator(PropertyKey.QUALITY, false);
        }

        if (RELEVANCE_ITEM.equals(item)) {
            return getFloatComparator(PropertyKey.RELEVANCE, false);
        }

        if ("Size (high to low)".equals(item)) {
            return new Comparator<VisualSearchResult>() {
                public int compare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    Long v1 = vsr1.getSize();
                    Long v2 = vsr2.getSize();
                    return v2.compareTo(v1);
                }
            };
        }

        if ("Size (low to high)".equals(item)) {
            return new Comparator<VisualSearchResult>() {
                public int compare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    Long v1 = vsr1.getSize();
                    Long v2 = vsr2.getSize();
                    return v1.compareTo(v2);
                }
            };
        }

        // TODO: How does Title differ from Name for Documents?

        // TODO: RMV Does Type ever differ from File type?

        if ("Year".equals(item)) {
            return getLongComparator(PropertyKey.YEAR, true);
        }

        throw new IllegalArgumentException("unknown item \"" + item + '"');
    }

    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        sortCombo.addItem(FRIEND_ITEM);
    }
    
    @EventSubscriber
    public void handleSignoffEvent(SignoffEvent event) {
        sortCombo.removeItem(FRIEND_ITEM);
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(3, 2, 2, 10); // top, left, bottom, right
        add(filterBox, gbc);

        gbc.insets.right = 5;
        add(sortLabel, gbc);

        add(sortCombo, gbc);
        
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

    public void setSearchCategory(SearchCategory category) {
        Object currentItem = sortCombo.getSelectedItem();
        repopulatingCombo = true;
        sortCombo.removeAllItems();
        String[] items = null;

        switch (category) {
            case ALL:
                items = new String[] {
                    RELEVANCE_ITEM, "Name", "File type",
                    "Size (high to low)", "Size (low to high)"
                };
                break;
            case AUDIO:
                items = new String[] {
                    RELEVANCE_ITEM, "Name", "Artist", "Album", "Length",
                    "Quality"
                };
                break;
            case VIDEO:
                items = new String[] {
                    RELEVANCE_ITEM, "Title", "Type", "Length", "Year",
                    "Quality"
                };
                break;
            case IMAGES:
                items = new String[] {
                    RELEVANCE_ITEM, "Name", "Type", "Date created (more recent)"
                };
                break;
            case DOCUMENTS:
                items = new String[] {
                    RELEVANCE_ITEM, "Filename", "Type",
                    "Size (low to high)", "Date created (more recent)"
                };
                break;
            default:
                items = new String[] {
                    RELEVANCE_ITEM, "Name", "File type",
                    "Size (high to low)", "Size (low to high)"
                };
                break;
        }

        for (String item : items) {
            sortCombo.addItem(item);
        }

        if (chatLoginState.isLoggedIn()) {
            sortCombo.addItem(FRIEND_ITEM);
        }

        repopulatingCombo = false;

        sortCombo.setSelectedItem(currentItem);
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