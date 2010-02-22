package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.Application;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.ContentSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.NonNullJComboBox;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SearchSettingListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Search Option View.
 */
public class SearchOptionPanel extends OptionPanel {

    private final AutoCompleteDictionary searchHistory;
    private final SpamManager spamManager;
    private final Application application;
    
    private final UnsafeTypeOptionPanel unsafeOptionPanel;    
    private final Provider<FilterKeywordOptionPanel> filterKeywordOptionPanelProvider;
    private final Provider<FilterFileExtensionsOptionPanel> filterFileExtensionsOptionPanelProvider;
  
    
    private SearchBarPanel searchBarPanel;
    private FilteringPanel filteringPanel;
    private JCheckBox groupSimilarResults;
    
    @Inject
    public SearchOptionPanel(@Named("searchHistory") AutoCompleteDictionary searchHistory,
            SpamManager spamManager,
            UnsafeTypeOptionPanel unsafeOptionPanel,
            Application application,
            Provider<FilterKeywordOptionPanel> filterKeywordOptionPanelProvider,
            Provider<FilterFileExtensionsOptionPanel> filterFileExtensionsOptionPanelProvider) {
        this.application = application;
        this.spamManager = spamManager;
        this.searchHistory = searchHistory;
        this.unsafeOptionPanel = unsafeOptionPanel;
 		this.filterKeywordOptionPanelProvider = filterKeywordOptionPanelProvider;
        this.filterFileExtensionsOptionPanelProvider = filterFileExtensionsOptionPanelProvider;
        
        groupSimilarResults = new JCheckBox(I18n.tr("Group similar search results together"));
        groupSimilarResults.setContentAreaFilled(false);
        
        setLayout(new MigLayout("nogrid, insets 15 15 15 15, fillx, gap 4"));
        add(getSearchBarPanel(), "growx, wrap");
        add(getFilteringPanel(), "growx, wrap");
        
        add(groupSimilarResults);
        add(new LearnMoreButton("http://www.limewire.com/client_redirect/?page=groupSimilarResults", application), "wrap");
    }

    private OptionPanel getSearchBarPanel() {
        if (searchBarPanel == null) {
            searchBarPanel = new SearchBarPanel();
        }
        return searchBarPanel;
    }

    @Override
    ApplyOptionResult applyOptions() {
        SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.setValue(groupSimilarResults.isSelected());
        ApplyOptionResult result = null;
        
        result = getSearchBarPanel().applyOptions();
        result.applyResult(getFilteringPanel().applyOptions());
        
        return result;
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
            defaultSearchSpinner = new NonNullJComboBox(SearchCategory.values());
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
        ApplyOptionResult applyOptions() {
            
            SearchCategory category = (SearchCategory) defaultSearchSpinner.getSelectedItem();
            if (category != null) {
                SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID
                    .setValue(category.getId());
            }
            
            SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.setValue(suggestFriendFiles.isSelected());
            SwingUiSettings.KEEP_SEARCH_HISTORY.setValue(searchTabNumberCheckBox.isSelected());
            return new ApplyOptionResult(false, true);
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
    
    private static class CategoryCellRenderer extends DefaultListCellRenderer {
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

        private final class SpamOptionPanel extends JPanel {
            {
                setLayout(new MigLayout("gapy 10, nogrid"));
                add(new JLabel(I18n.tr("Do you want to reset the Spam Filter?")) , "wrap");
                add(new MultiLineLabel(I18n.tr("This will clear all the files marked as spam. Doing this may result in more spam in search results."), 400) , "wrap");
                JButton okButton = new JButton(new OKDialogAction(I18n.tr("Reset")));
                okButton.addActionListener(new ActionListener() {
                   @Override
                    public void actionPerformed(ActionEvent e) {
                       spamManager.clearFilterData();
                    } 
                });
                
                add(okButton, "tag ok, alignx right,");
                add(new JButton(new CancelDialogAction()), "tag cancel");
            }
        }

        private FilterKeywordOptionPanel filterKeywordPanel;
        private FilterFileExtensionsOptionPanel filterFileExtensionPanel;
        
        private JCheckBox copyrightContentCheckBox;
        private JCheckBox adultContentCheckBox;
        private JButton filterKeywordsButton;
        private JButton filterFileExtensionsButton;
        
        private final JButton configureButton;
        private JButton clearSpamButton;

        public FilteringPanel() {
            super(I18n.tr("Search Filtering"));
           
            configureButton = new JButton(new DialogDisplayAction( SearchOptionPanel.this,
                    unsafeOptionPanel, I18n.tr("Unsafe File Sharing"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe file sharing settings")));
            
            filterKeywordPanel = filterKeywordOptionPanelProvider.get();
            filterKeywordPanel.setPreferredSize(new Dimension(300,400));
            
            filterFileExtensionPanel = filterFileExtensionsOptionPanelProvider.get();
            filterFileExtensionPanel.setPreferredSize(new Dimension(300,400));
            
            copyrightContentCheckBox = new JCheckBox("<html>"+I18n.tr("Don't let me download or upload files copyright owners request not be shared.")+"</html>");
            copyrightContentCheckBox.setContentAreaFilled(false);
            
            adultContentCheckBox = new JCheckBox("<html>"+I18n.tr("Don't show adult content in search results")+"</html>");
            adultContentCheckBox.setContentAreaFilled(false);
            
            filterKeywordsButton = new JButton(new DialogDisplayAction(SearchOptionPanel.this,
                    filterKeywordPanel, I18n.tr("Filter Keywords"),
                    I18n.tr("Filter Keywords..."),
                    I18n.tr("Restrict files with certain words from being displayed in search results")));
            
            filterFileExtensionsButton = new JButton(new DialogDisplayAction( SearchOptionPanel.this,
                    filterFileExtensionPanel, I18n.tr("Filter File Extensions"),
                    I18n.tr("Filter File Extensions..."),
                    I18n.tr("Restrict files with certain extensions from being displayed in search results")));
           
            clearSpamButton = new JButton(new DialogDisplayAction(SearchOptionPanel.this, new SpamOptionPanel(), I18n.tr("Reset Spam Filter"), I18n.tr("Reset Spam Filter..."), I18n.tr("Reset the Spam filter by clearing all files marked as spam")));
            
            JLabel programSharingLabel = new JLabel(I18n.tr("Change the ability to search for Programs"));
            add(programSharingLabel, "");
            add(configureButton, "wrap");
            
            add(copyrightContentCheckBox);
            add(new LearnMoreButton("http://www.limewire.com/client_redirect/?page=copyright", application), "wrap");
            add(adultContentCheckBox, "gapbottom 10, wrap");
            
            add(filterKeywordsButton, "gapright 10, alignx left");
            add(filterFileExtensionsButton, "gapright 10, alignx left");
            add(clearSpamButton, "gapright 10, alignx left, wrap");
        }
        
        @Override
        ApplyOptionResult applyOptions() {
            ContentSettings.USER_WANTS_MANAGEMENTS.setValue(copyrightContentCheckBox.isSelected());
            
            if (FilterSettings.FILTER_ADULT.getValue() != adultContentCheckBox.isSelected()) {
                FilterSettings.FILTER_ADULT.setValue(adultContentCheckBox.isSelected());
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        spamManager.adjustSpamFilters();
                    }
                });
            }
            boolean restartRequired = filterKeywordPanel.applyOptions().isRestartRequired()
                                      || filterFileExtensionPanel.applyOptions().isRestartRequired() ||
                                      unsafeOptionPanel.applyOptions().isRestartRequired();
            return new ApplyOptionResult(restartRequired, true);
        }

        @Override
        boolean hasChanged() {
            return  ContentSettings.USER_WANTS_MANAGEMENTS.getValue() != copyrightContentCheckBox.isSelected()
                    ||FilterSettings.FILTER_ADULT.getValue() != adultContentCheckBox.isSelected()
                    || filterKeywordPanel.hasChanged()
                    || filterFileExtensionPanel.hasChanged()
                    || unsafeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            copyrightContentCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());
            adultContentCheckBox.setSelected(FilterSettings.FILTER_ADULT.getValue());
            filterKeywordPanel.initOptions();
            filterFileExtensionPanel.initOptions();
            unsafeOptionPanel.initOptions();
        }

    }

}
