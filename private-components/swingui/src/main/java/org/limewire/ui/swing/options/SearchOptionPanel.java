package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.SearchSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Search Option View
 */
public class SearchOptionPanel extends OptionPanel {

    private SearchBarPanel searchBarPanel;
    private SearchResultsPanel searchResultsPanel;
    
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
    void initOptions() {
        getSearchBarPanel().initOptions();
        getSearchResultsPanel().initOptions();
    }
    
    private class SearchBarPanel extends OptionPanel {

        private JCheckBox suggestCheckBox;
        private JSpinner suggestSpinner;
        
        private JCheckBox searchTabNumberCheckBox;
        private JSpinner searchTabNumberSpinner;
        
        private JButton clearNowButton;
        
        public SearchBarPanel() {
            super(I18n.tr("Search Bar"));
            
            createComponents();
            
            add(suggestCheckBox);
            add(new JLabel("Suggest"), "split");
            add(suggestSpinner);
            add(new JLabel("files from friends when signed on."), "wrap");
            
            add(searchTabNumberCheckBox);
            add(new JLabel("Remember my"), "split");
            add(searchTabNumberSpinner);
            add(new JLabel("most recent searches"), "push");
            
            add(clearNowButton);
        }
        
        private void createComponents() {
            suggestCheckBox = new JCheckBox();
            suggestSpinner = new JSpinner();
            
            searchTabNumberCheckBox = new JCheckBox();
            searchTabNumberSpinner = new JSpinner();
            
            clearNowButton = new JButton(I18n.tr("Clear Now"));
        }
        
        @Override
        void applyOptions() {

        }

        @Override
        boolean hasChanged() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        void initOptions() {

        }
    }
    
    private class SearchResultsPanel extends OptionPanel {

        private JCheckBox groupSimilarResults;
        private JCheckBox searchFriendLibrary;
        private JCheckBox moveDownloadedFiles;
        
        public SearchResultsPanel() {
            super(I18n.tr("Search Result"));
            
            groupSimilarResults = new JCheckBox();
            searchFriendLibrary = new JCheckBox();
            moveDownloadedFiles = new JCheckBox();
            
            add(groupSimilarResults);
            add(new JLabel("Group similar search results"), "wrap");
            
            add(searchFriendLibrary);
            add(new JLabel("Also search my friend's libraries when signed on"), "wrap");
            
            add(moveDownloadedFiles);
            add(new JLabel("Move files I've already downloaded to the botton"));
        }
        
        @Override
        void applyOptions() {
            SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.setValue(groupSimilarResults.isSelected());
        }

        @Override
        boolean hasChanged() {
            return groupSimilarResults.isSelected() != SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue();
        }

        @Override
        void initOptions() {
            groupSimilarResults.setSelected(SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue());
        }
    }
}
