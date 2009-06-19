package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Security Option View.
 */
public class SecurityOptionPanel extends OptionPanel {
    
    private WarningMessagesPanel warningMessagesPanel;
    private UnsafeTypesPanel unsafeTypesPanel;
    private final LibraryManager libraryManager;
    private final SharedFileListManager shareListManager;
    
    @Inject
    public SecurityOptionPanel(LibraryManager libraryManager, SharedFileListManager shareListManager) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getWarningMessagesPanel(), "pushx, growx");
        add(getUnsafeTypesPanel(), "pushx, growx");
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
    

    @Override
    boolean applyOptions() {
        boolean restart = getWarningMessagesPanel().applyOptions();
        restart |= getUnsafeTypesPanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getWarningMessagesPanel().hasChanged() || getUnsafeTypesPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getWarningMessagesPanel().initOptions();
        getUnsafeTypesPanel().initOptions();
    }
    
    private class WarningMessagesPanel extends OptionPanel {

        private WarningMessagesOptionPanel warningMessagesPanel;
        private JButton settingsButton;
        
        public WarningMessagesPanel() {
            super(I18n.tr("Warning Messages"));
            
            warningMessagesPanel = new WarningMessagesOptionPanel(new OKDialogAction(), new CancelDialogAction());
            warningMessagesPanel.validate();
            
            settingsButton = new JButton(new DialogDisplayAction(SecurityOptionPanel.this ,
                    warningMessagesPanel, I18n.tr("Warning Messages"),
                    I18n.tr("Settings..."), I18n.tr("Choose which warning messags to display")));
            
            add(new JLabel(I18n.tr("Choose which warning messages you want to see when using LimeWire")), "push");
            add(settingsButton);
        }
        
        @Override
        boolean applyOptions() {
            return warningMessagesPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return warningMessagesPanel.hasChanged();
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
            super(I18n.tr("Unsafe Categories"));
            
            unsafeOptionPanel = new UnsafeTypeOptionPanel(new OKDialogAction(), libraryManager, shareListManager);
            
            configureButton = new JButton(new DialogDisplayAction( SecurityOptionPanel.this,
                    unsafeOptionPanel, I18n.tr("Unsafe Categories"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe categories")));
            
            setLayout(new MigLayout());
            add(new JLabel(I18n.tr("For your safety, LimeWire disables you from:")),"wrap, gaptop 10");
            add(new JLabel(I18n.tr("- Searching for and sharing programs with anyone")), "gapleft 25, wrap");
            add(new JLabel(I18n.tr("- Sharing documents with the P2P Network")), "gapleft 25, wrap");
            
            add(new JLabel("We strongly recommend you do not enable these settings"), "gaptop 10, push");
            add(configureButton, "gaptop 10");
        }
        
        @Override
        boolean applyOptions() {
            return unsafeOptionPanel.applyOptions();
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
    
}
