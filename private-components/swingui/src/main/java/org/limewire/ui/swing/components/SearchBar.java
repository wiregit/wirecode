package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchBar extends JXPanel {

    private final LimeComboBox comboBox;
    private final PromptTextField searchField;
    private final JXButton searchButton;
    
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
        for ( SearchCategory cat : SearchCategory.values() ) {
            if (cat == SearchCategory.OTHER)  continue;
            
            Action action = new CatagoryAction(cat);
            if (cat == SearchCategory.PROGRAM) {
                progAction = action;
                continue;
            }
            
            if (cat == this.categoryToSearch) actionToSelect = action;

            typeActions.add(action);
        }
        
        this.programAction = progAction;
        
        this.comboBox = comboBoxFactory.createLightFullComboBox(typeActions);
        this.searchField = new PromptTextField(I18n.tr("Search"));
        this.searchButton = new JXButton("S");
        
        this.assertProgramCategory();
        
        this.dropDownListAutoCompleteControl = DropDownListAutoCompleteControl.install(this.searchField, 
                friendLibraries.getDictionary(this.categoryToSearch));
        
        if (actionToSelect != null)
            this.comboBox.setSelectedAction(actionToSelect);
        
        this.setOpaque(false);
        this.add(this.comboBox, "gap 30");
        this.add(this.searchField, "gap 15");
        this.add(this.searchButton, "gap 15");

        this.searchField.setFont(this.searchField.getFont().deriveFont(12));
        
        LibrarySettings.PROGRAM_SHARING_ENABLED.addSettingListener(new SettingListener() {
            
            @Override
            public void settingChanged(SettingEvent evt) {
                assertProgramCategory();
            }
            
        });
    }
    
    private void assertProgramCategory() {
        if (!LibrarySettings.PROGRAM_SHARING_ENABLED.getValue()) {
            this.comboBox.removeAction(this.programAction);
        } else {
            this.comboBox.addAction(this.programAction);
        }
    }
    
    public void setSearchHandler(final SearchHandler searchHandler) {
        
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = searchField.getText();
                searchHandler.doSearch(new DefaultSearchInfo(searchText, categoryToSearch));
            }
        };
        
        this.searchField.addActionListener(listener);
        this.searchButton.addActionListener(listener);
    }
    
    private static String getName(SearchCategory category) {
        switch(category) {
        case ALL:      return I18n.tr("All");
        case AUDIO:    return I18n.tr("Music"); 
        case DOCUMENT: return I18n.tr("Documents"); 
        case IMAGE:    return I18n.tr("Images"); 
        case PROGRAM:  return I18n.tr("Programs"); 
        case VIDEO:    return I18n.tr("Videos"); 
        case OTHER: 
        default:
            return I18n.tr("Other");
             
        }
    }
    
    private class CatagoryAction extends AbstractAction {

        private final SearchCategory category;
        
        CatagoryAction(SearchCategory category) {
            super(getName(category));
            
            this.category = category;
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            categoryToSearch = category;
            
            dropDownListAutoCompleteControl.setDictionary(friendLibraries.getDictionary(category));
        }
    }
}
