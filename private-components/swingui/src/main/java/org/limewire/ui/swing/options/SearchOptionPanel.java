package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SearchSettingListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Search Option View
 */
@Singleton
public class SearchOptionPanel extends OptionPanel {
    
    private static final int MAX_FRIEND_SUGGESTIONS = 8;
    private static final int MAX_RECENT_SEARCHES = 5;
    private static final int MIN = 0;
    
    private SearchBarPanel searchBarPanel;
    private SearchResultsPanel searchResultsPanel;
    
    @Inject
    public SearchOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getSearchBarPanel(), "pushx, growx");
        add(getSearchResultsPanel(), "pushx, growx");
    }
    
    private OptionPanel getSearchBarPanel() {
        if(searchBarPanel == null) {
            searchBarPanel = new SearchBarPanel();
        }
        return searchBarPanel;
    }
    
    private OptionPanel getSearchResultsPanel() {
        if(searchResultsPanel == null) {
            searchResultsPanel = new SearchResultsPanel();
        }
        return searchResultsPanel;
    }

    @Override
    void applyOptions() {
        getSearchBarPanel().applyOptions();
        getSearchResultsPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getSearchBarPanel().hasChanged() || getSearchResultsPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getSearchBarPanel().initOptions();
        getSearchResultsPanel().initOptions();
    }
    
    private class SearchBarPanel extends OptionPanel {

        private JComboBox defaultSearchSpinner;
        
        private JCheckBox suggestCheckBox;
        private JSpinner suggestSpinner;
        
        private JCheckBox searchTabNumberCheckBox;
        private JSpinner searchTabNumberSpinner;
        
        private JButton clearNowButton;
        
        public SearchBarPanel() {
            super(I18n.tr("Search Bar"));
            
            createComponents();
            
            add(new JLabel(I18n.tr("By default, search for")), "split");
            add(defaultSearchSpinner, "wrap");
            
            add(suggestCheckBox, "split 4");
            add(new JLabel(I18n.tr("Suggest")));
            add(suggestSpinner);
            add(new JLabel(I18n.tr("files from friends when signed on.")), "wrap");
            
            add(searchTabNumberCheckBox,"split 4");
            add(new JLabel(I18n.tr("Remember my")));
            add(searchTabNumberSpinner);
            add(new JLabel(I18n.tr("most recent searches")), "push");
            
            add(clearNowButton, "alignx right");
        }
        
        private void createComponents() {
            defaultSearchSpinner = new JComboBox(SearchCategory.values());
            defaultSearchSpinner.removeItem(SearchCategory.OTHER);

            LibrarySettings.PROGRAM_SHARING_ENABLED.addSettingListener(new SearchSettingListener(LibrarySettings.PROGRAM_SHARING_ENABLED, SearchCategory.PROGRAM, defaultSearchSpinner));
            
            suggestCheckBox = new JCheckBox();
            suggestCheckBox.setContentAreaFilled(false);
            suggestSpinner = new JSpinner(new SpinnerNumberModel(MIN, MIN, MAX_FRIEND_SUGGESTIONS, 1));
            suggestSpinner.setEnabled(false);
            suggestCheckBox.addItemListener(new CheckBoxListener(suggestSpinner));
            suggestSpinner.addChangeListener(new SpinnerListener(suggestCheckBox, suggestSpinner));
            
            searchTabNumberCheckBox = new JCheckBox();
            searchTabNumberCheckBox.setContentAreaFilled(false);
            searchTabNumberSpinner = new JSpinner(new SpinnerNumberModel(MIN, MIN, MAX_RECENT_SEARCHES, 1));
            searchTabNumberSpinner.setEnabled(false);
            searchTabNumberCheckBox.addItemListener(new CheckBoxListener(searchTabNumberSpinner));
            searchTabNumberSpinner.addChangeListener(new SpinnerListener(searchTabNumberCheckBox, searchTabNumberSpinner));
            
            clearNowButton = new JButton(I18n.tr("Clear Now"));
            clearNowButton.setBorderPainted(false);
        }
        
        @Override
        void applyOptions() {
            SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.setValue(((SearchCategory)defaultSearchSpinner.getSelectedItem()).getId());
            SearchSettings.POPULATE_SEARCH_BAR_NUMBER_FRIEND_FILES.setValue(((SpinnerNumberModel)suggestSpinner.getModel()).getNumber().intValue());
            SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.setValue(suggestCheckBox.isSelected());
            SearchSettings.POPULATE_SEARCH_BAR_NUMBER_OLD_SEARCH.setValue(((SpinnerNumberModel)searchTabNumberSpinner.getModel()).getNumber().intValue());
            SearchSettings.REMEMBER_OLD_SEARCHES_SEARCH_BAR.setValue(searchTabNumberCheckBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue() != ((SearchCategory)defaultSearchSpinner.getSelectedItem()).getId() 
                || SearchSettings.POPULATE_SEARCH_BAR_NUMBER_FRIEND_FILES.getValue() != ((SpinnerNumberModel)suggestSpinner.getModel()).getNumber().intValue()
                || SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.getValue() != suggestCheckBox.isSelected()
                || SearchSettings.POPULATE_SEARCH_BAR_NUMBER_OLD_SEARCH.getValue() != ((SpinnerNumberModel)searchTabNumberSpinner.getModel()).getNumber().intValue()
                || SearchSettings.REMEMBER_OLD_SEARCHES_SEARCH_BAR.getValue() != searchTabNumberCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            defaultSearchSpinner.setSelectedItem(SearchCategory.forId(SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue()));
            //TODO: this setting isn't linked to anything
            suggestSpinner.getModel().setValue(SearchSettings.POPULATE_SEARCH_BAR_NUMBER_FRIEND_FILES.getValue());
            //TODO: this setting isn't linked to anything
            suggestCheckBox.setSelected(SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.getValue());
            
            //TODO: this setting isn't linked to anything
            searchTabNumberSpinner.getModel().setValue(SearchSettings.POPULATE_SEARCH_BAR_NUMBER_OLD_SEARCH.getValue());
            //TODO: this setting isn't linked to anything
            searchTabNumberCheckBox.setSelected(SearchSettings.REMEMBER_OLD_SEARCHES_SEARCH_BAR.getValue());
        }
    }
    
    private class SearchResultsPanel extends OptionPanel {

        private JCheckBox groupSimilarResults;
        private JCheckBox searchFriendLibrary;
        private JCheckBox moveDownloadedFiles;
        
        public SearchResultsPanel() {
            super(I18n.tr("Search Result"));
            
            groupSimilarResults = new JCheckBox();
            groupSimilarResults.setContentAreaFilled(false);
            searchFriendLibrary = new JCheckBox();
            searchFriendLibrary.setContentAreaFilled(false);
            moveDownloadedFiles = new JCheckBox();
            moveDownloadedFiles.setContentAreaFilled(false);
            
            add(groupSimilarResults);
            add(new JLabel(I18n.tr("Group similar search results")), "wrap");
            
            add(searchFriendLibrary);
            add(new JLabel(I18n.tr("Also search my friends' libraries when signed on")), "wrap");
            
            add(moveDownloadedFiles);
            add(new JLabel(I18n.tr("Move files I've already downloaded to the bottom")));
        }
        
        @Override
        void applyOptions() {
            SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.setValue(groupSimilarResults.isSelected());
            SearchSettings.SEARCH_FRIENDS_LIBRARIES.setValue(searchFriendLibrary.isSelected());
            SearchSettings.MOVE_DOWNLOADED_FILES_TO_BOTTOM.setValue(moveDownloadedFiles.isSelected());
        }

        @Override
        boolean hasChanged() {
            return groupSimilarResults.isSelected() != SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue()
                    || searchFriendLibrary.isSelected() != SearchSettings.SEARCH_FRIENDS_LIBRARIES.getValue()
                    || moveDownloadedFiles.isSelected() != SearchSettings.MOVE_DOWNLOADED_FILES_TO_BOTTOM.getValue();
        }

        @Override
        public void initOptions() {
            groupSimilarResults.setSelected(SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue());
            searchFriendLibrary.setSelected(SearchSettings.SEARCH_FRIENDS_LIBRARIES.getValue());
            moveDownloadedFiles.setSelected(SearchSettings.MOVE_DOWNLOADED_FILES_TO_BOTTOM.getValue());
        }
    }
    
    private class SpinnerListener implements ChangeListener {

        private JCheckBox checkBox;
        private JSpinner spinner;
        
        public SpinnerListener(JCheckBox checkBox, JSpinner spinner) {
            this.checkBox = checkBox;
            this.spinner = spinner;
        }
        
        @Override
        public void stateChanged(ChangeEvent e) {
            if( (Integer)spinner.getModel().getValue() <= MIN) {
                checkBox.setSelected(false);
            } else
                checkBox.setSelected(true);
        }
    }
    
    private class CheckBoxListener implements ItemListener {
        private JSpinner spinner;
        
        public CheckBoxListener(JSpinner spinner) {
            this.spinner = spinner;
        }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            if(e.getSource() instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) e.getSource();
                spinner.setEnabled(checkBox.isSelected());
            }
        }
    }
}
