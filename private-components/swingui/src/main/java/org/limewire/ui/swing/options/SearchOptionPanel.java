package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.ContentSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SearchSettingListener;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Search Option View.
 */
public class SearchOptionPanel extends OptionPanel {

    private final AutoCompleteDictionary searchHistory;
    private final SpamManager spamManager;
    
    private final UnsafeTypeOptionPanel unsafeOptionPanel;
    
    private SearchBarPanel searchBarPanel;
    private FilteringPanel filteringPanel;
    private JCheckBox groupSimilarResults;

    @Inject
    public SearchOptionPanel(@Named("searchHistory") AutoCompleteDictionary searchHistory,
            SpamManager spamManager,
            UnsafeTypeOptionPanel unsafeOptionPanel) {
        
        this.spamManager = spamManager;
        this.searchHistory = searchHistory;
        this.unsafeOptionPanel = unsafeOptionPanel;
        
        groupSimilarResults = new JCheckBox(I18n.tr("Group similar search results together"));
        groupSimilarResults.setContentAreaFilled(false);
        
        setLayout(new MigLayout("nogrid, insets 15 15 15 15, fillx"));
        add(getSearchBarPanel(), "growx, wrap");
        add(getFilteringPanel(), "growx, wrap");
        
        add(groupSimilarResults);
        add(new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new NotImplementedException("group learn more");
            }
        }), "wrap");
    }

    private OptionPanel getSearchBarPanel() {
        if (searchBarPanel == null) {
            searchBarPanel = new SearchBarPanel();
        }
        return searchBarPanel;
    }

    @Override
    boolean applyOptions() {
        SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.setValue(groupSimilarResults.isSelected());
        
        boolean restart = getSearchBarPanel().applyOptions();
        restart |= getFilteringPanel().applyOptions();
        
        return restart;
    }

    @Override
    boolean hasChanged() {
        return getSearchBarPanel().hasChanged()
        || getFilteringPanel().hasChanged()
        || groupSimilarResults.isSelected() != SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue();
    }

    @Override
    public void initOptions() {
        getSearchBarPanel().initOptions();
        getFilteringPanel().initOptions();
        
        groupSimilarResults.setSelected(SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue());
    }

    private class SearchBarPanel extends OptionPanel {

        private JComboBox defaultSearchSpinner;

        private JCheckBox searchTabNumberCheckBox;

        private JCheckBox suggestFriendFiles;

        private JButton clearNowButton;

        public SearchBarPanel() {
            super(I18n.tr("Search Bar"));

            createComponents();

            add(new JLabel(I18n.tr("By default, search for")));
            add(defaultSearchSpinner, "wrap");

            add(suggestFriendFiles, "wrap");

            add(searchTabNumberCheckBox, "gapright push");

            add(clearNowButton, "alignx right, wrap");
        }

        private void createComponents() {
            defaultSearchSpinner = new JComboBox(SearchCategory.values());
            defaultSearchSpinner.setRenderer(new CategoryCellRenderer());
            defaultSearchSpinner.removeItem(SearchCategory.OTHER);

            LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SearchSettingListener(
                    LibrarySettings.ALLOW_PROGRAMS, SearchCategory.PROGRAM,
                    defaultSearchSpinner));

            suggestFriendFiles = new JCheckBox(I18n.tr("Suggest files from friends"));
            suggestFriendFiles.setOpaque(false);

            searchTabNumberCheckBox = new JCheckBox(I18n.tr("Remember my recent searches"));
            searchTabNumberCheckBox.setOpaque(false);
            
            clearNowButton = new JButton(new AbstractAction(I18n.tr("Clear Now")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchHistory.clear();
                }
            });
        }

        @Override
        boolean applyOptions() {
            SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID
                    .setValue(((SearchCategory) defaultSearchSpinner.getSelectedItem()).getId());
            
            SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.setValue(suggestFriendFiles.isSelected());
            SwingUiSettings.KEEP_SEARCH_HISTORY.setValue(searchTabNumberCheckBox.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue() != ((SearchCategory) defaultSearchSpinner.getSelectedItem()).getId()
                    || SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.getValue() != suggestFriendFiles.isSelected()
                    || SwingUiSettings.KEEP_SEARCH_HISTORY.getValue() != searchTabNumberCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            defaultSearchSpinner.setSelectedItem(SearchCategory.forId(SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue()));
            suggestFriendFiles.setSelected(SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.getValue());
            searchTabNumberCheckBox.setSelected(SwingUiSettings.KEEP_SEARCH_HISTORY.getValue());
        }
    }
    
    private class CategoryCellRenderer extends DefaultListCellRenderer {
        public CategoryCellRenderer() {
            setOpaque(true);
        }
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            SearchCategory category = (SearchCategory)value;
            setText(SearchCategoryUtils.getOptionsName(category));
           
            return this;
        }
    }

    private OptionPanel getFilteringPanel() {
        if(filteringPanel == null) {
            filteringPanel = new FilteringPanel();
        }
        return filteringPanel;
    }

    
    private class FilteringPanel extends OptionPanel {

        private FilterKeywordOptionPanel filterKeywordPanel;
        private FilterFileExtensionsOptionPanel filterFileExtensionPanel;
        
        private JCheckBox copyrightContentCheckBox;
        private JCheckBox adultContentCheckBox;
        private JButton filterKeywordsButton;
        private JButton filterFileExtensionsButton;
        
        private final JButton configureButton;
        
        public FilteringPanel() {
            super(I18n.tr("Search Filtering"));
           
            configureButton = new JButton(new DialogDisplayAction( SearchOptionPanel.this,
                    unsafeOptionPanel, I18n.tr("Unsafe Categories"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe categories")));
            
            filterKeywordPanel = new FilterKeywordOptionPanel(spamManager, new OKDialogAction());
            filterKeywordPanel.setPreferredSize(new Dimension(300,400));
            
            filterFileExtensionPanel = new FilterFileExtensionsOptionPanel(spamManager, new OKDialogAction());
            filterFileExtensionPanel.setPreferredSize(new Dimension(300,400));
            
            copyrightContentCheckBox = new JCheckBox(I18n.tr("Don't let me download or upload files copyright owners request not be shared."));
            copyrightContentCheckBox.setContentAreaFilled(false);
            
            adultContentCheckBox = new JCheckBox(I18n.tr("Don't show adult content in search results"));
            adultContentCheckBox.setContentAreaFilled(false);
            
            filterKeywordsButton = new JButton(new DialogDisplayAction(SearchOptionPanel.this,
                    filterKeywordPanel, I18n.tr("Filter Keywords"),
                    I18n.tr("Filter Keywords..."),
                    I18n.tr("Restrict files with certain words from being displayed in search results")));
            
            filterFileExtensionsButton = new JButton(new DialogDisplayAction( SearchOptionPanel.this,
                    filterFileExtensionPanel, I18n.tr("Filter File Extensions"),
                    I18n.tr("Filter File Extensions..."),
                    I18n.tr("Restrict files with certain extensions from being displayed in search results")));
           
            add(new JLabel(I18n.tr("LimeWire is helping to prevent viruses by not showing Programs in search results")));
            add(configureButton, "wrap");
            
            add(copyrightContentCheckBox);
            add(new HyperlinkButton(new AbstractAction(I18n.tr("Learn more")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new NotImplementedException("copyright learn more");
                }
            }), "wrap");
            add(adultContentCheckBox, "wrap");
            
            add(filterKeywordsButton, "gapright 10, alignx left");
            add(filterFileExtensionsButton, "alignx left, wrap");
        }
        
        @Override
        boolean applyOptions() {
            ContentSettings.USER_WANTS_MANAGEMENTS.setValue(copyrightContentCheckBox.isSelected());
            
            FilterSettings.FILTER_ADULT.setValue(adultContentCheckBox.isSelected());
            return filterKeywordPanel.applyOptions() || filterFileExtensionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return  ContentSettings.USER_WANTS_MANAGEMENTS.getValue() != copyrightContentCheckBox.isSelected()
                    ||FilterSettings.FILTER_ADULT.getValue() != adultContentCheckBox.isSelected()
                    || filterKeywordPanel.hasChanged()
                    || filterFileExtensionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            copyrightContentCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());
            adultContentCheckBox.setSelected(FilterSettings.FILTER_ADULT.getValue());
            filterKeywordPanel.initOptions();
            filterFileExtensionPanel.initOptions();
        }
    }

}
