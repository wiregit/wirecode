package org.limewire.ui.swing.search.advanced;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.UiSearchListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/** The container for advanced searching. */
public class AdvancedSearchPanel extends JXPanel {
    
    public static final String NAME = "ADVANCED_SEARCH";
    
    private final List<UiSearchListener> uiSearchListeners = new ArrayList<UiSearchListener>();
    private final PropertyDictionary propertyDictionary;
    
    private final Action searchAction;
    private AdvancedPanel visibleComponent = null;
    private final FriendAutoCompleterFactory friendAutoCompleterFactory;

    @Resource private Font headingFont;
    
    @Inject
    public AdvancedSearchPanel(PropertyDictionary propertyDictionary, FriendAutoCompleterFactory friendAutoCompleterFactory,
            HeaderBarDecorator headerDecorator, ButtonDecorator buttonDecorator) {
        this.propertyDictionary = propertyDictionary;
                this.friendAutoCompleterFactory = friendAutoCompleterFactory;
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, nogrid"));
        
        HeaderBar heading = new HeaderBar(I18n.tr("Advanced Search"));
        headerDecorator.decorateBasic(heading);
        
        JLabel description = new JLabel(I18n.tr("Search for the following type of file:"));
        description.setFont(headingFont);

        add(heading, "dock north");
        add(description, "gapleft 15, gaptop 15, wrap");
        addCategory(SearchCategory.AUDIO, createAudioFields());
        addCategory(SearchCategory.VIDEO, createVideoFields());
        addCategory(SearchCategory.IMAGE, createImageFields());
        addCategory(SearchCategory.DOCUMENT, createDocumentFields());
        addCategory(SearchCategory.PROGRAM, createProgramFields());
        searchAction = new AbstractAction(I18n.tr("Search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(visibleComponent != null) {
                    SearchInfo info = visibleComponent.getSearchInfo();
                    if(info != null) {
                        for(UiSearchListener uiSearchListener : uiSearchListeners) {
                            uiSearchListener.searchTriggered(info);
                        }
                    }
                }
            }
        };
        
        JXButton searchButton = new JXButton(searchAction);
        buttonDecorator.decorateGreenFullButton(searchButton);
        searchButton.setFont(headingFont);
        
        add(searchButton, "gapbefore push");
    }
    
    private AdvancedPanel createProgramFields() {
        return new AdvancedProgramPanel(friendAutoCompleterFactory);
    }

    private AdvancedPanel createDocumentFields() {
        return new AdvancedDocumentPanel(friendAutoCompleterFactory);
    }

    private AdvancedPanel createImageFields() {
        return new AdvancedImagePanel(friendAutoCompleterFactory);
    }

    private AdvancedPanel createVideoFields() {
        return new AdvancedVideoPanel(propertyDictionary, friendAutoCompleterFactory);
    }

    private AdvancedPanel createAudioFields() {
        return new AdvancedAudioPanel(propertyDictionary, friendAutoCompleterFactory);
    }

    private void addCategory(SearchCategory category, final AdvancedPanel component) {
        HyperlinkButton button = new HyperlinkButton(new AbstractAction(I18n.tr(category.getCategory().getPluralName())) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if(visibleComponent != component) {
                    if(visibleComponent != null) {
                        visibleComponent.setVisible(false);
                    }
                    visibleComponent = component;
                    component.setVisible(true);
                } else {
                    visibleComponent.setVisible(false);
                    visibleComponent = null;
                }
            }
        });
        add(button, "gapleft 30, gaptop 15, wrap");
        add(component, "wmin 200, growx, gapleft 30, gaptop 6, gapbottom 6, wrap");
        component.setVisible(false);
    }

    /** Adds a listener that will be notified when an advanced search is triggered. */
    public void addSearchListener(UiSearchListener uiSearchListener) {
        uiSearchListeners.add(uiSearchListener);
    }

}
