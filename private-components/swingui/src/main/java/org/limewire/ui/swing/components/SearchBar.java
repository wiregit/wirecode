package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.StringTrieSet;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.HistoryAndFriendAutoCompleter;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The search bar component at the top of the UI main window.  This includes
 * the drop-down search category box, search input field, and search button.
 */
@Singleton
public class SearchBar extends JXPanel {

    @Resource private Color searchBorder;
    
    private final LimeComboBox comboBox;
    private final LimePromptTextField searchField;
    private final JButton searchButton;
    
    private final HistoryAndFriendAutoCompleter autoCompleter;
    private final FriendAutoCompleters friendLibraries;
    
    private SearchCategory categoryToSearch; 
    
    private final Action programAction;
    
    @Inject
    public SearchBar(LimeComboBoxFactory comboBoxFactory, 
            final FriendAutoCompleters friendLibraries) {
        super(new MigLayout("ins 0, gapx 0, gapy 0"));
    
        GuiUtils.assignResources(this);

        this.friendLibraries = friendLibraries;
        this.autoCompleter = new HistoryAndFriendAutoCompleter();
        
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
                
        this.searchField = new LimePromptTextField(I18n.tr("Search"), AccentType.BUBBLE, searchBorder);
        this.searchField.setName("SearchBar.searchField");
        
        this.searchButton = new IconButton();
        this.searchButton.setName("SearchBar.searchButton");
        this.searchButton.setFocusPainted(false);
        this.searchButton.setToolTipText(I18n.tr("Search P2P Network"));
        
        this.configureProgramCategory();
        
        final DropDownListAutoCompleteControl autoCompleteControl = DropDownListAutoCompleteControl.install(this.searchField, autoCompleter);
        autoCompleteControl.setAutoComplete(true);
        final AutoCompleteDictionary historyDictionary = new StringTrieSet(true);
        autoCompleter.setHistoryDictionary(historyDictionary);
        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get search text, and add to history if non-empty.
                String searchText = getSearchText();
                if (!searchText.isEmpty()) {
                    historyDictionary.addEntry(searchText);
                }
            }
        });
        // TODO: this setting doesn't exist in options right now
//        SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.addSettingListener(new SettingListener() {
//            @Override
//            public void settingChanged(SettingEvent evt) {
//                SwingUtils.invokeLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        autoCompleteControl.setAutoComplete(SearchSettings.POPULATE_SEARCH_BAR_FRIEND_FILES.getValue());
//                    }
//                });
//            }
//        });
        
        if (actionToSelect != null) {
            autoCompleter.setSuggestionDictionary(friendLibraries.getDictionary(categoryToSearch));
            this.comboBox.setSelectedAction(actionToSelect);
        } else {
            autoCompleter.setSuggestionDictionary(friendLibraries.getDictionary(SearchCategory.ALL));
        }
        
        this.setOpaque(false);
        this.add(this.comboBox);
        this.add(this.searchField, "gap 5");
        this.add(this.searchButton, "gap 5");

        LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SettingListener() {            
            @Override
            public void settingChanged(SettingEvent evt) {
                configureProgramCategory();
            }            
        });
    }
    
    /**
     * Performs additional Guice injection tasks.  This method adds a listener
     * to handle XMPP connection events.
     */
    @Inject
    void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                    searchButton.setToolTipText(I18n.tr("Search P2P Network and Friends"));
                    break;
                default:
                    searchButton.setToolTipText(I18n.tr("Search P2P Network"));
                    break;
                }
            }
        });
    }
    
    private void configureProgramCategory() {
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
                // Get search text, and do search if non-empty.
                String searchText = getSearchText();
                if (!searchText.isEmpty()) {
                    searchHandler.doSearch(DefaultSearchInfo.createKeywordSearch(
                            searchText, categoryToSearch));
                }
            }
        };
        
        this.searchField.addActionListener(listener);
        this.searchButton.addActionListener(listener);
    }
    
    /**
     * Returns the search text with leading and trailing whitespace removed.
     * The method returns an empty string if there are no non-whitespace
     * characters.
     */
    private String getSearchText() {
        // Get search text and trim whitespace.
        String searchText = searchField.getText();
        return ((searchText != null) ? searchText.trim() : "");
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
            autoCompleter.setSuggestionDictionary(friendLibraries.getDictionary(category));
        }
    }
}
