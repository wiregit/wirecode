package org.limewire.ui.swing.search;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.util.Objects.compareToNull;
import static org.limewire.util.Objects.compareToNullIgnoreCase;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.SearchSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.search.model.SimilarResultsGroupingComparator;
import org.limewire.ui.swing.search.model.SimilarResultsGroupingDelegateComparator;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;

/**
 * This class is a panel that contains components for filtering and sorting
 * search results.
 * 
 * @see org.limewire.ui.swing.search.SearchResultsPanel.
 */
public class SortAndFilterPanel extends JXPanel {

    private final ButtonDecorator buttonDecorator;
    
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
    private final String CATEGORY = tr("Category");
    private final String NAME = tr("Name");
    private final String RELEVANCE_ITEM = tr("Relevance");

    private final HashMap<String,Action> actions = new HashMap<String,Action>(); 

    @Resource private Icon listViewIcon;
    @Resource private Icon tableViewIcon;

    private final LimeComboBox sortCombo;
    
    private final JLabel sortLabel = new JLabel(tr("Sort by:"));
    private final JTextField filterBox = new PromptTextField(tr("Filter"));
    private final JXButton listViewToggleButton = new JXButton();
    private final JXButton tableViewToggleButton = new JXButton();
    
    private String sortBy;

    private VisualSearchResultTextFilterator filterator =
        new VisualSearchResultTextFilterator();
    
    private MatcherEditor<VisualSearchResult> editor =
        new TextComponentMatcherEditor<VisualSearchResult>(
            filterBox, filterator, true); // true for "live"
    
    private boolean repopulatingCombo;

    @Inject
    SortAndFilterPanel(LimeComboBoxFactory comboBoxFactory, ButtonDecorator buttonDecorator) {
        GuiUtils.assignResources(this);
        
        this.buttonDecorator = buttonDecorator;
        
        this.populateActionList();
        
        this.sortCombo = comboBoxFactory.createDarkFullComboBox();
        
        // TODO: Resources and bar conversion
        this.setMinimumSize(new Dimension(10, 34));
        this.setMaximumSize(new Dimension(10000, 34));
        this.sortCombo.setPreferredSize(new Dimension(5, 22));
        this.sortCombo.setMinimumSize(this.sortCombo.getPreferredSize());
        this.sortCombo.setMaximumSize(new Dimension(1000,22));
        
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
        this.actions.put(CATEGORY,createAction(CATEGORY));
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

    private void configureViewButtons() {
        final SortAndFilterPanel outerThis = this;

        buttonDecorator.decorateDarkFullImageButton(listViewToggleButton, DrawMode.LEFT_ROUNDED);
        listViewToggleButton.setIcon(listViewIcon);
        listViewToggleButton.setPressedIcon(listViewIcon);
        listViewToggleButton.setToolTipText(tr("List view"));
        listViewToggleButton.setMargin(new Insets(0, 10, 0, 6));

        listViewToggleButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    SearchSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.LIST.getId());
                    selectListView(outerThis);
                }
            }
        });

        buttonDecorator.decorateDarkFullImageButton(tableViewToggleButton, DrawMode.RIGHT_ROUNDED);
        tableViewToggleButton.setIcon(tableViewIcon);
        tableViewToggleButton.setPressedIcon(tableViewIcon);
        tableViewToggleButton.setMargin(new Insets(0, 6, 0, 10));

        
        tableViewToggleButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    SearchSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.TABLE.getId());
                    selectTableView(outerThis);
                }
            }
        });

        
        //TODO this is not a singleton, need to remove the listener when we are done with this panel
        SearchSettings.SEARCH_VIEW_TYPE_ID.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                int newViewTypeId = SearchSettings.SEARCH_VIEW_TYPE_ID.getValue();
                SearchViewType newSearchViewType = SearchViewType.forId(newViewTypeId);
                updateView(outerThis, newSearchViewType);
            }
        });
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(listViewToggleButton);
        viewGroup.add(tableViewToggleButton);
        
        int viewTypeId = SearchSettings.SEARCH_VIEW_TYPE_ID.getValue();
        SearchViewType searchViewType = SearchViewType.forId(viewTypeId);
        
        updateView(outerThis, searchViewType);
    }

    private void updateView(final SortAndFilterPanel outerThis,
            SearchViewType newSearchViewType) {
        switch (newSearchViewType) {
        case LIST:
            selectListView(outerThis);
            break;
        case TABLE:
            selectTableView(outerThis);
            break;
        }
    }
    
    private void selectListView(final SortAndFilterPanel outerThis) {
        tableViewToggleButton.setSelected(false);
        listViewToggleButton.setSelected(true);
        sortLabel.setVisible(true);
        sortCombo.setVisible(true);
    }

    private void selectTableView(final SortAndFilterPanel outerThis) {
        tableViewToggleButton.setSelected(true);
        listViewToggleButton.setSelected(false);
        sortLabel.setVisible(false);
        sortCombo.setVisible(false);
    }

    public EventList<VisualSearchResult> getFilteredAndSortedList(
        EventList<VisualSearchResult> simpleList, final RowSelectionPreserver preserver) {
        // Created a SortedList that doesn't have a Comparator yet.
        final SortedList<VisualSearchResult> sortedList =
            GlazedListsFactory.sortedList(simpleList, getRelevanceComparator());

        EventList<VisualSearchResult> filteredList =
            GlazedListsFactory.filterList(sortedList, editor);
        
        SelectionListener listener = new SelectionListener() {
            @Override
            public void selectionChanged(Action action) {
                String item = (String)action.getValue(Action.NAME);
                if (!repopulatingCombo && !item.equals(sortBy)) { // changing sort order
                    Comparator<VisualSearchResult> comparator =getComparator(item);
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

    private static Comparator<VisualSearchResult> getDateComparator(final FilePropertyKey key,
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, true);
            }
        };
    }

    private static Comparator<VisualSearchResult> getLongComparator(final FilePropertyKey key,
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = vsr1.getPropertyString(key);
                String v2 = vsr2.getPropertyString(key);
                Long l1 = CommonUtils.parseLongNoException(v1);
                Long l2 = CommonUtils.parseLongNoException(v2);
                return compareNullCheck(l1, l2, ascending, true);
            }
        };
    }

    private static Comparator<VisualSearchResult> getStringComparator(final FilePropertyKey key,
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = (String) vsr1.getProperty(key);
                String v2 = (String) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, false);
            }
        };
    }

    private static Comparator<VisualSearchResult> getNameComparator(final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = vsr1.getHeading();
                String v2 = vsr2.getHeading();
                return ascending ? compareToNullIgnoreCase(v1, v2, false)
                        : compareToNullIgnoreCase(v2, v1, false);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static SimilarResultsGroupingComparator getStringPropertyPlusNameComparator(
            final FilePropertyKey FilePropertyKey, final boolean ascending) {
        return new SimilarResultsGroupingDelegateComparator(getStringComparator(FilePropertyKey, ascending), getNameComparator(ascending));
    }
    
    @SuppressWarnings("unchecked")
    private SimilarResultsGroupingDelegateComparator getRelevanceComparator() {
        return new SimilarResultsGroupingDelegateComparator(new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult o1, VisualSearchResult o2) {
                //descending
                return compareToNull(o2.getRelevance(), o1.getRelevance(), false);
            }
        }, getNameComparator(true));
    }

    @SuppressWarnings("unchecked")
    private SimilarResultsGroupingComparator getComparator(String item) {

        if (ALBUM.equals(item)) {
            return getStringPropertyPlusNameComparator(FilePropertyKey.ALBUM, true);
        }

        if (ARTIST.equals(item)) {
            return getStringPropertyPlusNameComparator(FilePropertyKey.AUTHOR, true);
        }

        if (COMPANY.equals(item)) {
            return getStringPropertyPlusNameComparator(FilePropertyKey.COMPANY, true);
        }

        if (DATE_CREATED.equals(item)) {
            return new SimilarResultsGroupingDelegateComparator(getDateComparator(FilePropertyKey.DATE_CREATED, false), getNameComparator(true)); 
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

        if (CATEGORY.equals(item)) {
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
            return new SimilarResultsGroupingDelegateComparator(getLongComparator(FilePropertyKey.LENGTH, false), getNameComparator(true));
        }

        if (NAME.equals(item) || TITLE.equals(item)) {
            return new SimilarResultsGroupingDelegateComparator(getNameComparator(true));
        }

        if (PLATFORM.equals(item)) {
            return getStringPropertyPlusNameComparator(FilePropertyKey.COMPANY, true);
        }

        if (QUALITY.equals(item)) {
            return new SimilarResultsGroupingDelegateComparator(getLongComparator(FilePropertyKey.QUALITY, false), getNameComparator(true));
        }

        if (RELEVANCE_ITEM.equals(item)) {
            return getRelevanceComparator();
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
                        FilePropertyKey.YEAR, true);

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

    public void clearFilterBox() {
        filterBox.setText("");
    }
    
    /**
     * Sets the state of the view toggle buttons.
     * @param mode the current mode ... LIST or TABLE
     */
    public void setMode(SearchViewType mode) {
        if (mode == SearchViewType.LIST) {
            listViewToggleButton.setSelected(true);
            tableViewToggleButton.setSelected(false);
        } else if (mode == SearchViewType.TABLE) {
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
                    RELEVANCE_ITEM, NAME, CATEGORY,
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

        List<Action> actionList = new LinkedList<Action>();
        
        for (String item : items) {
            actionList.add(this.actions.get(item));
        }
        
        sortCombo.addActions(actionList);

        repopulatingCombo = false;

        sortCombo.setSelectedAction(currentItem);
    }
    
    
    private static class VisualSearchResultTextFilterator
        implements TextFilterator<VisualSearchResult> {
        
        @Override
        public void getFilterStrings(
                List<String> list, VisualSearchResult vsr) {
            list.add(vsr.getFileExtension());
            Map<FilePropertyKey, Object> props = vsr.getProperties();
            for (FilePropertyKey key : props.keySet()) {
                
                if (!FilePropertyKey.getIndexableKeys().contains(key))  
                    continue;
             
                String value = vsr.getPropertyString(key);
                if(value != null) {
                    list.add(value);
                }
            }
        }
    }
}