package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

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

    private SearchBarPanel searchBarPanel;

    @Inject
    public SearchOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        add(getSearchBarPanel(), "pushx, growx");
    }

    private OptionPanel getSearchBarPanel() {
        if (searchBarPanel == null) {
            searchBarPanel = new SearchBarPanel();
        }
        return searchBarPanel;
    }

    @Override
    void applyOptions() {
        getSearchBarPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getSearchBarPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getSearchBarPanel().initOptions();
    }

    private class SearchBarPanel extends OptionPanel {

        private JComboBox defaultSearchSpinner;

        private JCheckBox searchTabNumberCheckBox;

        private JCheckBox groupSimilarResults;

        private JButton clearNowButton;

        public SearchBarPanel() {
            super(I18n.tr("Searching"));

            createComponents();

            add(new JLabel(I18n.tr("By default, search for")), "split");
            add(defaultSearchSpinner, "wrap");

            add(searchTabNumberCheckBox, "split 4, push");

            add(clearNowButton, "wrap, alignx right");

            add(groupSimilarResults, "split 4");
        }

        private void createComponents() {
            defaultSearchSpinner = new JComboBox(SearchCategory.values());
            defaultSearchSpinner.removeItem(SearchCategory.OTHER);

            LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SearchSettingListener(
                    LibrarySettings.ALLOW_PROGRAMS, SearchCategory.PROGRAM,
                    defaultSearchSpinner));

            searchTabNumberCheckBox = new JCheckBox(I18n.tr("Remember my most recent searches"));
            searchTabNumberCheckBox.setContentAreaFilled(false);
            
            groupSimilarResults = new JCheckBox(I18n.tr("Group similar search results"));
            groupSimilarResults.setContentAreaFilled(false);

            clearNowButton = new JButton(I18n.tr("Clear Now"));
            clearNowButton.setBorderPainted(false);
        }

        @Override
        void applyOptions() {
            SearchSettings.DEFAULT_SEARCH_CATEGORY_ID
                    .setValue(((SearchCategory) defaultSearchSpinner.getSelectedItem()).getId());
            SearchSettings.REMEMBER_OLD_SEARCHES_SEARCH_BAR.setValue(searchTabNumberCheckBox
                    .isSelected());
            SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.setValue(groupSimilarResults.isSelected());
        }

        @Override
        boolean hasChanged() {
            return SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue() != ((SearchCategory) defaultSearchSpinner
                    .getSelectedItem()).getId()
                    || SearchSettings.REMEMBER_OLD_SEARCHES_SEARCH_BAR.getValue() != searchTabNumberCheckBox
                            .isSelected()
                    || groupSimilarResults.isSelected() != SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED
                            .getValue();
        }

        @Override
        public void initOptions() {
            defaultSearchSpinner.setSelectedItem(SearchCategory
                    .forId(SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue()));
            // TODO: this setting isn't linked to anything
            searchTabNumberCheckBox.setSelected(SearchSettings.REMEMBER_OLD_SEARCHES_SEARCH_BAR
                    .getValue());
            groupSimilarResults
                    .setSelected(SearchSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue());
        }
    }
}
