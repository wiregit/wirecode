package org.limewire.ui.swing.search;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.util.Objects.compareToNull;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.friends.ChatLoginState;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.search.model.SimilarResultsGroupingComparator;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;

/**
 * This class is a panel that contains components for filtering and sorting
 * search results.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class SortAndFilterPanel extends JXPanel {

    private final String COMPANY = tr("Company");
    private final String PLATFORM = tr("Platform");
    private final String TYPE = tr("Type");
    private final String DATE_CREATED = tr("Date created (more recent)");
    private final String QUALITY = tr("Quality");
    private final String YEAR = tr("Year");
    private final String FILE_EXTENSION = tr("File extension");
    private final String TITLE = tr("Title");
    private final String LENGTH = tr("Length");
    private final String ALBUM = tr("Album");
    private final String ARTIST = tr("Artist");
    private final String SIZE_LOW_TO_HIGH = tr("Size (low to high)");
    private final String SIZE_HIGH_TO_LOW = tr("Size (high to low)");
    private final String FILE_TYPE = tr("File type");
    private final String NAME = tr("Name");
    private final String FRIEND_ITEM = tr("Friend (a-z)");
    private final String RELEVANCE_ITEM = tr("Relevance");
    private static final int FILTER_WIDTH = 10;

    private ChatLoginState chatLoginState;
    
    private List<ModeListener> modeListeners = new ArrayList<ModeListener>();
    
//    private final List<SearchFilterListener> filterListeners =
//        new CopyOnWriteArrayList<SearchFilterListener>();

    @Resource private Icon listViewPressedIcon;
    @Resource private Icon listViewUnpressedIcon;
    @Resource private Icon tableViewPressedIcon;
    @Resource private Icon tableViewUnpressedIcon;

    private final JComboBox sortCombo = new JComboBox();
    
    private final JLabel sortLabel = new JLabel(tr("Sort by:"));
    private final JTextField filterBox = new FilteredTextField(FILTER_WIDTH);
    private final JToggleButton listViewToggleButton = new JToggleButton();
    private final JToggleButton tableViewToggleButton = new JToggleButton();
    
    private String sortBy;

    private VisualSearchResultTextFilterator filterator =
        new VisualSearchResultTextFilterator();

    private TextComponentMatcherEditor<VisualSearchResult> editor =
        new TextComponentMatcherEditor<VisualSearchResult>(
            filterBox, filterator, true); // true for "live"

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

//        filterBox.getDocument().addDocumentListener(new DocumentListener() {
//            @Override
//            public void insertUpdate(DocumentEvent e) {
//                new FilterEvent(filterBox.getText()).publish();
//            }
//
//            @Override
//            public void removeUpdate(DocumentEvent e) {
//                new FilterEvent(filterBox.getText()).publish();
//            }
//
//            @Override
//            public void changedUpdate(DocumentEvent e) {
//                new FilterEvent(filterBox.getText()).publish();
//            }
//        });

//        editor.addMatcherEditorListener(new Listener<VisualSearchResult>() {
//            public void changedMatcher(Event<VisualSearchResult> arg0) {
//                for (SearchFilterListener listener : filterListeners) {
//                    listener.searchFiltered();
//                }
//            }
//        });

//        EventAnnotationProcessor.subscribe(this);
    }

//    public void addFilterListener(SearchFilterListener listener) {
//        filterListeners.add(listener);
//    }
//
//    public void removeFilterListener(SearchFilterListener listener) {
//        filterListeners.remove(listener);
//    }
    
    public void addModeListener(ModeListener listener) {
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
                    sortLabel.setVisible(true);
                    sortCombo.setVisible(true);
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
                    sortLabel.setVisible(false);
                    sortCombo.setVisible(false);
                }
            }
        });
        
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewToggleButton);
        viewGroup.add(tableViewToggleButton);
    }
    
    public EventList<VisualSearchResult> getFilteredAndSortedList(
        EventList<VisualSearchResult> simpleList) {

        // Create a list that is filtered by a text field.
        

        // Created a SortedList that doesn't have a Comparator yet.
        final SortedList<VisualSearchResult> sortedList =
            new SortedList<VisualSearchResult>(simpleList, getFloatComparator(PropertyKey.RELEVANCE, false));

        EventList<VisualSearchResult> filteredList =
            new FilterList<VisualSearchResult>(sortedList, editor);
        
        ItemListener listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                String item = e.getItem().toString();
                if (!repopulatingCombo
                    && e.getStateChange() == ItemEvent.SELECTED
                    && !item.equals(sortBy)) { // changing sort order
                    Comparator<VisualSearchResult> comparator =
                        getComparator(item);
                    // TODO: RMV Determine which row, if any, is selected.
                    sortedList.setComparator(comparator);
                    // TODO: RMV Restore the row selection.
                    sortBy = item;
                }
            }
        };
        sortCombo.addItemListener(listener);
        
        // Trigger the initial sort.
        sortCombo.setSelectedItem(RELEVANCE_ITEM);

        return filteredList;
    }

    private static Comparator<VisualSearchResult> getDateComparator(
        final PropertyKey key, final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending);
            }
        };
    }

    private static Comparator<VisualSearchResult> getFloatComparator(
            final PropertyKey key, final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            protected int doCompare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Float v1 = (Float) vsr1.getProperty(key);
                Float v2 = (Float) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending);
            }
        };
    }

    private static Comparator<VisualSearchResult> getLongComparator(
        final PropertyKey key, final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending);
            }
        };
    }

    private static Comparator<VisualSearchResult> getStringComparator(
        final PropertyKey key, final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(
                VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = (String) vsr1.getProperty(key);
                String v2 = (String) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending);
            }
        };
    }
    
    private Comparator<VisualSearchResult> getComparator(String item) {

        if (ALBUM.equals(item)) {
            return getStringComparator(PropertyKey.ALBUM_TITLE, true);
        }

        if (ARTIST.equals(item)) {
            return getStringComparator(PropertyKey.ARTIST_NAME, true);
        }

        if (COMPANY.equals(item)) {
            return getStringComparator(PropertyKey.COMPANY, true);
        }

        if (DATE_CREATED.equals(item)) {
            return getDateComparator(PropertyKey.DATE_CREATED, false);
        }

        if (FILE_EXTENSION.equals(item)
            || FILE_TYPE.equals(item)
            || TYPE.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                @Override
                public int doCompare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    return compareToNull(vsr1.getMediaType(), vsr2.getMediaType());
                }
            };
        }

        if (FRIEND_ITEM.equals(item)) {
            return null; // TODO: RMV What to do here?
        }

        if (LENGTH.equals(item)) {
            return getLongComparator(PropertyKey.LENGTH, true);
        }

        if (NAME.equals(item)
            || "Filename".equals(item)
            || TITLE.equals(item)) {
            return getStringComparator(PropertyKey.NAME, true);
        }

        if (PLATFORM.equals(item)) {
            return getStringComparator(PropertyKey.PLATFORM, true);
        }

        if (QUALITY.equals(item)) {
            return getLongComparator(PropertyKey.QUALITY, false);
        }

        if (RELEVANCE_ITEM.equals(item)) {
            return getFloatComparator(PropertyKey.RELEVANCE, false);
        }

        if (SIZE_HIGH_TO_LOW.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                @Override
                public int doCompare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    return compareToNull(vsr2.getSize(), vsr1.getSize());
                }
            };
        }

        if (SIZE_LOW_TO_HIGH.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                @Override
                public int doCompare(
                    VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    return compareToNull(vsr1.getSize(), vsr2.getSize());
                }
            };
        }

        if (YEAR.equals(item)) {
            return getLongComparator(PropertyKey.YEAR, true);
        }

        throw new IllegalArgumentException(tr("unknown item \"{0}\"", item));
    }
    
    private static int compareNullCheck(Comparable c1, Comparable c2, boolean ascending) {
        return ascending ? compareToNull(c1, c2) : compareToNull(c2, c1);
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

    private void notifyModeListeners(ModeListener.Mode mode) {
        for (ModeListener listener : modeListeners) {
            listener.setMode(mode);
        }
    }

    public void removeModeListener(ModeListener listener) {
        modeListeners.remove(listener);
    }

    public void clearFilterBox() {
        filterBox.setText("");
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
                    RELEVANCE_ITEM, NAME, FILE_TYPE,
                    SIZE_HIGH_TO_LOW, SIZE_LOW_TO_HIGH
                };
                break;
            case AUDIO:
                items = new String[] {
                    RELEVANCE_ITEM, NAME, ARTIST, ALBUM, LENGTH,
                    QUALITY
                };
                break;
            case VIDEO:
                items = new String[] {
                    RELEVANCE_ITEM, TITLE, FILE_EXTENSION, LENGTH,
                    YEAR, QUALITY
                };
                break;
            case IMAGE:
                items = new String[] {
                    RELEVANCE_ITEM, NAME, FILE_EXTENSION,
                    DATE_CREATED
                };
                break;
            case DOCUMENT:
                items = new String[] {
                    RELEVANCE_ITEM, NAME, TITLE, TYPE,
                    SIZE_LOW_TO_HIGH, DATE_CREATED
                };
                break;
            case PROGRAM:
                items = new String[] {
                    RELEVANCE_ITEM, NAME, SIZE_LOW_TO_HIGH,
                    PLATFORM, COMPANY
                };
                break;
            default:
                items = new String[] {
                    RELEVANCE_ITEM, NAME, TYPE,
                    SIZE_HIGH_TO_LOW, SIZE_LOW_TO_HIGH
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
                List<String> list, VisualSearchResult vsr) {
            list.add(vsr.getDescription());
            list.add(vsr.getFileExtension());
            list.add(String.valueOf(vsr.getSize()));
            
            //add(list, vsr.getMediaType());

            Map<SearchResult.PropertyKey, Object> props = vsr.getProperties();
            for (SearchResult.PropertyKey key : props.keySet()) {
                String value = vsr.getPropertyString(key);
                if(value != null) {
                    list.add(value);
                }
            }
        }
    }
}