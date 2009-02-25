package org.limewire.ui.swing.search.advanced;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HeaderBar;
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
    private final ButtonDecorator buttonDecorator;
    
    private final Action searchAction = new AbstractAction(I18n.tr("Search")) {
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
    
    private AdvancedPanel visibleComponent = null;
    private final FriendAutoCompleterFactory friendAutoCompleterFactory;

    private final JPanel selectorPanel;
    private final JPanel inputPanel;
    private final CardLayout inputLayout;
    
    @Resource private Font headingFont;

    private final JXButton searchButton = new JXButton(searchAction);
    
    @Inject
    public AdvancedSearchPanel(PropertyDictionary propertyDictionary, FriendAutoCompleterFactory friendAutoCompleterFactory,
            HeaderBarDecorator headerDecorator, ButtonDecorator buttonDecorator) {
        
        this.propertyDictionary = propertyDictionary;
        this.friendAutoCompleterFactory = friendAutoCompleterFactory;
        this.buttonDecorator = buttonDecorator;
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3, nogrid"));
        
        HeaderBar heading = new HeaderBar(I18n.tr("Advanced Search"));
        headerDecorator.decorateBasic(heading);
        JLabel description = new JLabel(I18n.tr("Search for the following type of file:"));
        description.setFont(headingFont);
        add(heading, "dock north");
        add(description, "gapleft 15, gaptop 15, wrap");
        
        selectorPanel = new JPanel(new FlowLayout());
        selectorPanel.setOpaque(false);
        add(selectorPanel, "gapleft 30, gaptop 5, wrap");

        inputLayout = new CardLayout();
        inputPanel = new JPanel(inputLayout);
        //ResizeUtils.forceWidth(inputPanel, 300);
        inputPanel.setOpaque(false);
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        String emptyName = "xad@#$as$#!Xe";
        inputPanel.add(emptyPanel, emptyName);
        inputLayout.show(inputPanel, emptyName);
        add(inputPanel, "gapleft 45, gaptop 15, wrap");
        
        addCategory(SearchCategory.AUDIO, createAudioFields());
        addCategory(SearchCategory.VIDEO, createVideoFields());
        addCategory(SearchCategory.IMAGE, createImageFields());
        addCategory(SearchCategory.DOCUMENT, createDocumentFields());
        addCategory(SearchCategory.PROGRAM, createProgramFields());
        
        buttonDecorator.decorateGreenFullButton(searchButton);
        searchButton.setFont(headingFont);
        searchButton.setVisible(false);
        add(searchButton, "gapleft 345");
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

    private void addCategory(final SearchCategory category, final AdvancedPanel component) {
        JXButton button = new JXButton(new AbstractAction(I18n.tr(category.getCategory().getPluralName())) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                visibleComponent = component;
                inputLayout.show(inputPanel, category.getCategory().toString());
                searchButton.setVisible(true);
            }
        });
        button.setModel(new JToggleButton.ToggleButtonModel());
        buttonDecorator.decorateMiniButton(button);
        button.setFont(headingFont);
        
        selectorPanel.add(button);
        inputPanel.add(component, category.getCategory().toString());
    }

    /** Adds a listener that will be notified when an advanced search is triggered. */
    public void addSearchListener(UiSearchListener uiSearchListener) {
        uiSearchListeners.add(uiSearchListener);
    }

}
