package org.limewire.ui.swing.options;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ContentSettings;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Security Option View
 */
@Singleton
public class SecurityOptionPanel extends OptionPanel {
    
    private WarningMessagesPanel warningMessagesPanel;
    private UnsafeTypesPanel unsafeTypesPanel;
    private FilteringPanel filteringPanel;
    
    @Inject
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
    public void initOptions() {
        getWarningMessagesPanel().initOptions();
        getUnsafeTypesPanel().initOptions();
        getFilteringPanel().initOptions();
    }
    
    private class WarningMessagesPanel extends OptionPanel {

        private WarningMessagesOptionPanel warningMessagesPanel;
        private JButton settingsButton;
        
        public WarningMessagesPanel() {
            super(I18n.tr("Warning Messages"));
            
            warningMessagesPanel = new WarningMessagesOptionPanel(new OKDialogAction(), new CancelDialogAction());
            warningMessagesPanel.setPreferredSize(new Dimension(400,130));
            
            settingsButton = new JButton(new DialogDisplayAction(SecurityOptionPanel.this ,
                    warningMessagesPanel, I18n.tr("Warning Messages"),
                    I18n.tr("Settings"), I18n.tr("Choose which warning messags to display")));
            
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
        public void initOptions() {
            warningMessagesPanel.initOptions();
        }
    }
    
    private class UnsafeTypesPanel extends OptionPanel {
        private UnsafeTypeOptionPanel unsafeOptionPanel;
        private JButton configureButton;
        
        public UnsafeTypesPanel() {
            super(I18n.tr("Unsafe types"));
            
          unsafeOptionPanel = new UnsafeTypeOptionPanel(new OKDialogAction());
          unsafeOptionPanel.setPreferredSize(new Dimension(550, 160));
            
            configureButton = new JButton(new DialogDisplayAction( SecurityOptionPanel.this,
                    unsafeOptionPanel, I18n.tr("Configure"),
                    I18n.tr("Unsafe types"), I18n.tr("Configure Documents and Programs")));
            
            add(new JLabel("For your safety, LimeWire disables you from:"),"wrap");
            add(new JLabel("-Searching for and sharing Programs with anyone"), "gapleft 25, wrap");
            add(new JLabel("-Searching for and sharing Documents with the LimeWire Network"), "gapleft 25, wrap");
            
            add(new JLabel("We strongly recommend you do not enable these settings"), "push");
            add(configureButton);
        }
        
        @Override
        void applyOptions() {
            unsafeOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return unsafeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            unsafeOptionPanel.initOptions();
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
            unlicensedCheckBox.setContentAreaFilled(false);
            adultContentCheckBox = new JCheckBox();
            adultContentCheckBox.setContentAreaFilled(false);
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
        public void initOptions() {
            unlicensedCheckBox.setSelected(ContentSettings.USER_WANTS_MANAGEMENTS.getValue());
        }
    }
}
