package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ContentSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Security Option View
 */
public class SecurityOptionPanel extends OptionPanel {

    private WarningMessagesPanel warningMessagesPanel;
    private UnsafeTypesPanel unsafeTypesPanel;
    private FilteringPanel filteringPanel;
    
    public SecurityOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getWarningMessagesPanel(), "pushx, growx");
        add(getUnsafeTypesPanel(), "pushx, growx");
        add(getFilteringPanel(), "pushx, growx");
    }
    
    private OptionPanel getWarningMessagesPanel() {
        if(warningMessagesPanel == null) {
            warningMessagesPanel = new WarningMessagesPanel();
        }
        return warningMessagesPanel;
    }
    
    private OptionPanel getUnsafeTypesPanel() {
        if(unsafeTypesPanel == null) {
            unsafeTypesPanel = new UnsafeTypesPanel();
        }
        return unsafeTypesPanel;
    }
    
    private OptionPanel getFilteringPanel() {
        if(filteringPanel == null) {
            filteringPanel = new FilteringPanel();
        }
        return filteringPanel;
    }

    @Override
    void applyOptions() {
        getWarningMessagesPanel().applyOptions();
        getUnsafeTypesPanel().applyOptions();
        getFilteringPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getWarningMessagesPanel().hasChanged() || getUnsafeTypesPanel().hasChanged() || getFilteringPanel().hasChanged();
    }

    @Override
    void initOptions() {
        getWarningMessagesPanel().initOptions();
        getUnsafeTypesPanel().initOptions();
        getFilteringPanel().initOptions();
    }
    
    private class WarningMessagesPanel extends OptionPanel {

        private JButton settingsButton;
        
        public WarningMessagesPanel() {
            super(I18n.tr("Warning Messages"));
            
            settingsButton = new JButton(I18n.tr("Settings"));
            
            add(new JLabel("Choose which warning messages you want to see when using LimeWire"), "push");
            add(settingsButton);
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {
        }
    }
    
    private class UnsafeTypesPanel extends OptionPanel {

        private JButton configureButton;
        
        public UnsafeTypesPanel() {
            super(I18n.tr("Unsafe types"));
            
            configureButton = new JButton(I18n.tr("Configure"));
            
            add(new JLabel("For your safety, LimeWire disables you from:"),"wrap");
            add(new JLabel("-Searching for and sharing Programs with anyone"), "gapleft 25, wrap");
            add(new JLabel("-Searching for and sharing Documents with the LimeWire Network"), "gapleft 25, wrap");
            
            add(new JLabel("We strongly recommend you do not enable these settings"), "push");
            add(configureButton);
        }
        
        @Override
        void applyOptions() {
        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        void initOptions() {
        }
    }
    
    private class FilteringPanel extends OptionPanel {

        private JCheckBox unlicensedCheckBox;
        private JCheckBox adultContentCheckBox;
        private JButton filterFileExtensionsButton;
        private JButton filterKeywordsButton;
        
        public FilteringPanel() {
            super(I18n.tr("Filtering"));
            
            unlicensedCheckBox = new JCheckBox();
            adultContentCheckBox = new JCheckBox();
            filterFileExtensionsButton = new JButton(I18n.tr("Filter file extensions"));
            filterKeywordsButton = new JButton(I18n.tr("Filter keywords"));
            
            add(new JLabel("In search results..."), "wrap");
            
            add(unlicensedCheckBox, "split, gapleft 20");
            add(new JLabel("Don't show unlicensed or unathorized files"), "wrap");
            
            add(adultContentCheckBox, "split, gapleft 20");
            add(new JLabel("Don't show adult content"), "wrap");
            
            add(filterFileExtensionsButton, "split");
            add(filterKeywordsButton);
        }
        
        @Override
        void applyOptions() {
            ContentSettings.USER_WANTS_MANAGEMENTS.setValue(unlicensedCheckBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return ContentSettings.USER_WANTS_MANAGEMENTS.getValue() != unlicensedCheckBox.isSelected();
        }

        @Override
        void initOptions() {
            unlicensedCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());
        }
    }
}
