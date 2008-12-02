package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchBar extends JXPanel {

    private final LimeComboBox comboBox;
    private final PromptTextField searchField;
    private final JButton searchButton;
    
    private final DropDownListAutoCompleteControl dropDownListAutoCompleteControl;
    private final FriendAutoCompleters friendLibraries;
    
    private SearchCategory categoryToSearch; 
    
    private final Action programAction;
    
    @Inject
    public SearchBar(LimeComboBoxFactory comboBoxFactory, 
            final FriendAutoCompleters friendLibraries) {
        super(new MigLayout("ins 0, gapx 0, gapy 0"));
    
        GuiUtils.assignResources(this);

        this.friendLibraries = friendLibraries;
        
        this.categoryToSearch = SearchCategory.forId(SearchSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue());
        
        Action actionToSelect = null;
        Action progAction = null;
        
        List<Action> typeActions = new LinkedList<Action>();
        for (SearchCategory cat : SearchCategory.values()) {
            if (cat == SearchCategory.OTHER) {
                continue;
            }

            Action action = new CatagoryAction(cat);
            if (cat == SearchCategory.PROGRAM) {
                progAction = action;
                continue;
            }

            if (cat == this.categoryToSearch) {
                actionToSelect = action;
            }

            typeActions.add(action);
        }
        
        this.programAction = progAction;
        
        this.comboBox = comboBoxFactory.createLightFullComboBox(typeActions);
        this.comboBox.setName("SearchBar.comboBox");
                
        this.searchField = new PromptTextField(I18n.tr("Search"), AccentType.BUBBLE);
        this.searchField.setName("SearchBar.searchField");
        
        this.searchButton = new IconButton();
        this.searchButton.setName("SearchBar.searchButton");
        this.searchButton.setFocusPainted(false);
        
        this.assertProgramCategory();
        
        this.dropDownListAutoCompleteControl = DropDownListAutoCompleteControl.install(this.searchField, 
                friendLibraries.getDictionary(this.categoryToSearch));
        this.dropDownListAutoCompleteControl.setAutoComplete(SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.getValue());
        SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.addSettingListener(new SettingListener() {

            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        dropDownListAutoCompleteControl.setAutoComplete(SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.getValue());
                    }
                });
            }
        });
        
        if (actionToSelect != null)
            this.comboBox.setSelectedAction(actionToSelect);
        
        this.setOpaque(false);
        this.add(this.comboBox);
        this.add(this.searchField, "gap 5");
        this.add(this.searchButton, "gap 5");

        LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SettingListener() {            
            @Override
            public void settingChanged(SettingEvent evt) {
                assertProgramCategory();
            }            
        });
    }
    
    private void assertProgramCategory() {
        if (!LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            this.comboBox.removeAction(this.programAction);
        } else {
            this.comboBox.addAction(this.programAction);
        }
    }
    
    public void setText(String text) {
        this.searchField.setText(text);
    }
    
    public void setSearchHandler(final SearchHandler searchHandler) {
        
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchField.getText().isEmpty()) {
                    return;
                }

                String searchText = searchField.getText();
                searchHandler.doSearch(DefaultSearchInfo.createKeywordSearch(searchText,
                        categoryToSearch));
            }
        };
        
        this.searchField.addActionListener(listener);
        this.searchButton.addActionListener(listener);
    }
    
    private class CatagoryAction extends AbstractAction {

        private final SearchCategory category;
        
        CatagoryAction(SearchCategory category) {
            super(SearchCategoryUtils.getName(category));
            
            this.category = category;
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            categoryToSearch = category;
            
            dropDownListAutoCompleteControl.setDictionary(friendLibraries.getDictionary(category));
        }
    }
}
