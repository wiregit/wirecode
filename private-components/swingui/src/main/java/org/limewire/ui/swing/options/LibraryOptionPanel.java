package org.limewire.ui.swing.options;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Library Option View
 */
@Singleton
public class LibraryOptionPanel extends OptionPanel {

    private LibraryManagementPanel libraryManagerPanel;
    private SmartSharingPanel smartSharingPanel;
    
    @Inject
    public LibraryOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getLibraryManagerPanel(), "pushx, growx");
        add(getSmartSharingPane(), "pushx, growx");
    }
    
    private OptionPanel getLibraryManagerPanel() {
        if(libraryManagerPanel == null) {
            libraryManagerPanel = new LibraryManagementPanel();
        }
        return libraryManagerPanel;
    }
    
    private OptionPanel getSmartSharingPane() {
        if(smartSharingPanel == null) {
            smartSharingPanel = new SmartSharingPanel();
        }
        return smartSharingPanel;
    }   

    @Override
    void applyOptions() {
        getLibraryManagerPanel().applyOptions();
        getSmartSharingPane().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getLibraryManagerPanel().hasChanged() || getSmartSharingPane().hasChanged();
    }

    @Override
    void initOptions() {
        getLibraryManagerPanel().initOptions();
        getSmartSharingPane().initOptions();
    }
    
    /**
     * 
     */
    private class LibraryManagementPanel extends OptionPanel {
        private JButton manageLibraryButton;
        
        public LibraryManagementPanel() {
            super(I18n.tr("Library Management"));
            
            manageLibraryButton = new JButton(I18n.tr("Manage Library"));
            
            add(new MultiLineLabel(I18n.tr("Your library is a central location to view, share and unshare your files with the LimeWire Network and your friends."), 700), "wrap");
            add(new MultiLineLabel(I18n.tr("LimeWire will scan folders and look for files to add to your library. Scanning folders into your Library will no automatically share them."), 700), "wrap");
            add(manageLibraryButton, "align right");
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
    
    /**
     * 
     */
    private class SmartSharingPanel extends OptionPanel {
        private JCheckBox smartSharingCheckBox;
        
        public SmartSharingPanel() {
            super(I18n.tr("Smart Sharing"));
            
            smartSharingCheckBox = new JCheckBox();
            
            add(smartSharingCheckBox);
            add(new JLabel(I18n.tr("When I share all files of a type, Smart Share new files added of that type.")));
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
}
