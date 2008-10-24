package org.limewire.ui.swing.options;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
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
    public void initOptions() {
        getLibraryManagerPanel().initOptions();
        getSmartSharingPane().initOptions();
    }
    
    /**
     * 
     */
    private class LibraryManagementPanel extends OptionPanel {
        
        private LibraryManagerOptionPanel libraryOptionPanel;
        private JButton manageLibraryButton;
        
        public LibraryManagementPanel() {
            super(I18n.tr("Library Management"));
            
            libraryOptionPanel = new LibraryManagerOptionPanel(new OKDialogAction(), new CancelDialogAction());
            libraryOptionPanel.setPreferredSize(new Dimension(500, 500));
            
            manageLibraryButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    libraryOptionPanel,I18n.tr("Unsafe types"), I18n.tr("Manage Library"), 
                    I18n.tr("Manage what folders are loaded into your library")));
            
            add(new MultiLineLabel(I18n.tr("Your library is a central location to view, share and unshare your files with the LimeWire Network and your friends."), 700), "wrap");
            add(new MultiLineLabel(I18n.tr("LimeWire will scan folders and look for files to add to your library. Scanning folders into your Library will no automatically share them."), 600), "wrap");
            add(manageLibraryButton, "align right");
        }
        
        @Override
        void applyOptions() {
            libraryOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return libraryOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            libraryOptionPanel.initOptions();
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
        public void initOptions() {
        }
    }
}
