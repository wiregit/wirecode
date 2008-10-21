package org.limewire.ui.swing.search;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.util.Objects.compareToNull;
import static org.limewire.util.Objects.compareToNullIgnoreCase;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.painter.ButtonPainter;
import org.limewire.ui.swing.search.model.SimilarResultsGroupingComparator;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.EventList;
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
    private final String RELEVANCE_ITEM = tr("Relevance");
    private List<ModeListener> modeListeners = new ArrayList<ModeListener>();

    private final HashMap<String,Action> actions = new HashMap<String,Action>(); 

    @Resource private Icon listViewIcon;
    @Resource private Icon tableViewIcon;
    @Resource private Font sortLabelFont; 

    private final LimeComboBox sortCombo;
    
    private final JLabel sortLabel = new JLabel(tr("Sort by:"));
    private final JTextField filterBox = new PromptTextField();
    private final JXButton listViewToggleButton = new JXButton();
    private final JXButton tableViewToggleButton = new JXButton();
    
    private String sortBy;

    private VisualSearchResultTextFilterator filterator =
        new VisualSearchResultTextFilterator();

    private TextComponentMatcherEditor<VisualSearchResult> editor =
        new TextComponentMatcherEditor<VisualSearchResult>(
            filterBox, filterator, true); // true for "live"

    private boolean repopulatingCombo;

    @Inject
    SortAndFilterPanel(LimeComboBoxFactory comboBoxFactory) {
        GuiUtils.assignResources(this);
        
        this.populateActionList();
        
        this.sortCombo = comboBoxFactory.createFullComboBox();
        
        sortLabel.setForeground(Color.WHITE);
        sortLabel.setFont(sortLabelFont);
        listViewToggleButton.setModel(new JToggleButton.ToggleButtonModel());
        tableViewToggleButton.setModel(new JToggleButton.ToggleButtonModel());
        setSearchCategory(SearchCategory.ALL);
        configureViewButtons();
        layoutComponents();
    }

    private void populateActionList() {
        this.actions.put(COMPANY,createAction(COMPANY));
        this.actions.put(PLATFORM,createAction(PLATFORM));
        this.actions.put(TYPE,createAction(TYPE));
        this.actions.put(DATE_CREATED,createAction(DATE_CREATED));
        this.actions.put(QUALITY,createAction(QUALITY));
        this.actions.put(YEAR,createAction(YEAR));
        this.actions.put(FILE_EXTENSION,createAction(FILE_EXTENSION));
        this.actions.put(TITLE,createAction(TITLE));
        this.actions.put(LENGTH,createAction(LENGTH));
        this.actions.put(ALBUM,createAction(ALBUM));
        this.actions.put(ARTIST,createAction(ARTIST));
        this.actions.put(SIZE_LOW_TO_HIGH,createAction(SIZE_LOW_TO_HIGH));
        this.actions.put(SIZE_HIGH_TO_LOW,createAction(SIZE_HIGH_TO_LOW));
        this.actions.put(FILE_TYPE,createAction(FILE_TYPE));
        this.actions.put(NAME,createAction(NAME));
        this.actions.put(RELEVANCE_ITEM,createAction(RELEVANCE_ITEM));
    }
    
    private Action createAction(String name) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                
            }
        };
    }
    
    public void addModeListener(ModeListener listener) {
        modeListeners.add(listener);
    }
    
    private void configureViewButtons() {
        final SortAndFilterPanel outerThis = this;
        
        listViewToggleButton.setIcon(listViewIcon);
        listViewToggleButton.setPressedIcon(listViewIcon);
        listViewToggleButton.setSelected(true);
        listViewToggleButton.setMargin(new Insets(0, 10, 0, 6));
        listViewToggleButton.setOpaque(false);
        listViewToggleButton.setToolTipText(tr("List view"));
        listViewToggleButton.setBackgroundPainter(new ButtonPainter());
        listViewToggleButton.setBorderPainted(false);
        listViewToggleButton.setPaintBorderInsets(true);
        listViewToggleButton.setContentAreaFilled(false);
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
        
        tableViewToggleButton.setIcon(tableViewIcon);
        tableViewToggleButton.setPressedIcon(tableViewIcon);
        tableViewToggleButton.setMargin(new Insets(0, 6, 0, 10));
        tableViewToggleButton.setOpaque(false);
        tableViewToggleButton.setToolTipText(tr("Table view"));
        tableViewToggleButton.setBackgroundPainter(new ButtonPainter());
        tableViewToggleButton.setBorderPainted(false);
        tableViewToggleButton.setPaintBorderInsets(true);
        tableViewToggleButton.setContentAreaFilled(false);
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
        EventList<VisualSearchResult> simpleList, final RowSelectionPreserver preserver) {

        // Create a list that is filtered by a text field.
        

        // Created a SortedList that doesn't have a Comparator yet.
        final SortedList<VisualSearchResult> sortedList =
            GlazedListsFactory.sortedList(simpleList, getFloatComparator(PropertyKey.RELEVANCE, false, false));

        EventList<VisualSearchResult> filteredList =
            GlazedListsFactory.filterList(sortedList, editor);
        
        SelectionListener listener = new SelectionListener() {
            @Override
            public void selectionChanged(Action action) {
                String item = action.getValue("Name").toString();
                if (!repopulatingCombo
                    && !item.equals(sortBy)) { // changing sort order
                    Comparator<VisualSearchResult> comparator =
                        getComparator(item);
                    preserver.beforeUpdateEvent();
                    sortedList.setComparator(comparator);
                    preserver.afterUpdateEvent();
                    sortBy = item;
                }
            }
        };
        sortCombo.addSelectionListener(listener);
        
        // Trigger the initial sort.
        sortCombo.setSelectedAction(actions.get(RELEVANCE_ITEM));

        return filteredList;
    }

    private static Comparator<VisualSearchResult> getDateComparator(final PropertyKey key,
            final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, true);
            }
        };
    }

    private static Comparator<VisualSearchResult> getFloatComparator(final PropertyKey key,
            final boolean ascending, final boolean nullsFirst) {
        return new SimilarResultsGroupingComparator() {
            @Override
            protected int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Float v1 = (Float) vsr1.getProperty(key);
                Float v2 = (Float) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, nullsFirst);
            }
        };
    }

    private static Comparator<VisualSearchResult> getLongComparator(final PropertyKey key,
            final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = vsr1.getPropertyString(key);
                String v2 = vsr2.getPropertyString(key);
                Long l1 = CommonUtils.parseLongNoException(v1);
                Long l2 = CommonUtils.parseLongNoException(v2);
                return compareNullCheck(l1, l2, ascending, true);
            }
        };
    }

    private static Comparator<VisualSearchResult> getStringComparator(final PropertyKey key,
            final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = (String) vsr1.getProperty(key);
                String v2 = (String) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, false);
            }
        };
    }

    private static Comparator<VisualSearchResult> getNameComparator(final boolean ascending) {
        return new SimilarResultsGroupingComparator() {
            @Override
            public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = vsr1.getHeading();
                String v2 = vsr2.getHeading();
                return ascending ? compareToNullIgnoreCase(v1, v2, false)
                        : compareToNullIgnoreCase(v2, v1, false);
            }
        };
    }

    private static Comparator<VisualSearchResult> getStringPropertyPlusNameComparator(
            final PropertyKey propertyKey, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            private Comparator<VisualSearchResult> propertyComparator = getStringComparator(
                    propertyKey, ascending);

            private Comparator<VisualSearchResult> nameComparator = getNameComparator(ascending);

            @Override
            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                int compare = propertyComparator.compare(o1, o2);
                if (compare == 0) {
                    compare = nameComparator.compare(o1, o2);
                }
                return compare;
            }
        };
    }

    private Comparator<VisualSearchResult> getComparator(String item) {

        if (ALBUM.equals(item)) {
            return getStringPropertyPlusNameComparator(PropertyKey.ALBUM_TITLE, true);
        }

        if (ARTIST.equals(item)) {
            return getStringPropertyPlusNameComparator(PropertyKey.ARTIST_NAME, true);
        }

        if (COMPANY.equals(item)) {
            return getStringPropertyPlusNameComparator(PropertyKey.COMPANY, true);
        }

        if (DATE_CREATED.equals(item)) {
            return new Comparator<VisualSearchResult>() {
                private Comparator<VisualSearchResult> propertyComparator = getDateComparator(
                        PropertyKey.DATE_CREATED, false);

                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                    int compare = propertyComparator.compare(o1, o2);
                    if (compare == 0) {
                        compare = nameComparator.compare(o1, o2);
                    }
                    return compare;
                }
            };
        }

        if (FILE_EXTENSION.equals(item) || TYPE.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr1.getFileExtension(), vsr2.getFileExtension());
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (FILE_TYPE.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr1.getCategory(), vsr2.getCategory());
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (LENGTH.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                private Comparator<VisualSearchResult> propertyComparator = getLongComparator(
                        PropertyKey.LENGTH, false);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = propertyComparator.compare(vsr1, vsr2);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (NAME.equals(item) || TITLE.equals(item)) {
            return getNameComparator(true);
        }

        if (PLATFORM.equals(item)) {
            return getStringPropertyPlusNameComparator(PropertyKey.COMPANY, true);
        }

        if (QUALITY.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                private Comparator<VisualSearchResult> propertyComparator = getLongComparator(
                        PropertyKey.QUALITY, false);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = propertyComparator.compare(vsr1, vsr2);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (RELEVANCE_ITEM.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                private Comparator<VisualSearchResult> propertyComparator = getFloatComparator(
                        PropertyKey.RELEVANCE, false, false);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = propertyComparator.compare(vsr1, vsr2);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (SIZE_HIGH_TO_LOW.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr2.getSize(), vsr1.getSize(), false);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (SIZE_LOW_TO_HIGH.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = compareToNull(vsr1.getSize(), vsr2.getSize(), false);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        if (YEAR.equals(item)) {
            return new SimilarResultsGroupingComparator() {
                private Comparator<VisualSearchResult> nameComparator = getNameComparator(true);

                private Comparator<VisualSearchResult> propertyComparator = getLongComparator(
                        PropertyKey.YEAR, true);

                @Override
                public int doCompare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                    int compare = propertyComparator.compare(vsr1, vsr2);
                    if (compare == 0) {
                        compare = nameComparator.compare(vsr1, vsr2);
                    }
                    return compare;
                }
            };
        }

        throw new IllegalArgumentException("unknown item " +  item);
    }

    private static int compareNullCheck(Comparable c1, Comparable c2, boolean ascending,
            boolean nullsFirst) {
        return ascending ? compareToNull(c1, c2, nullsFirst) : compareToNull(c2, c1, nullsFirst);
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
        Action currentItem = sortCombo.getSelectedAction();
        repopulatingCombo = true;
        sortCombo.removeAllActions();
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
            sortCombo.addAction(this.actions.get(item));
        }

        repopulatingCombo = false;

        sortCombo.setSelectedAction(currentItem);
    }
    
    private static class VisualSearchResultTextFilterator
    implements TextFilterator<VisualSearchResult> {
        @Override
        public void getFilterStrings(
                List<String> list, VisualSearchResult vsr) {
            list.add(vsr.getFileExtension());
            list.add(String.valueOf(vsr.getSize()));
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