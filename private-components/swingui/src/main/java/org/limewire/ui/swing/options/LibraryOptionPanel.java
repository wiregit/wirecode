package org.limewire.ui.swing.options;

import java.awt.Dimension;

import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Library Option View
 */
public class LibraryOptionPanel extends OptionPanel {

    private LibraryManagementPanel libraryManagerPanel;
    
    private LibraryManager libraryManager;
    
    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getLibraryManagerPanel(), "pushx, growx");
    }
    
    private OptionPanel getLibraryManagerPanel() {
        if(libraryManagerPanel == null) {
            libraryManagerPanel = new LibraryManagementPanel();
        }
        return libraryManagerPanel;
    }

    @Override
    void applyOptions() {
        getLibraryManagerPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getLibraryManagerPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getLibraryManagerPanel().initOptions();
    }
    
    /**
     * 
     */
    private class LibraryManagementPanel extends OptionPanel {
        
        private LibraryManagerOptionPanel libraryOptionPanel;
        private JButton manageLibraryButton;
        
        public LibraryManagementPanel() {
            super(I18n.tr("Library Management"));
            
            libraryOptionPanel = new LibraryManagerOptionPanel(new OKDialogAction(), new CancelDialogAction(), libraryManager.getLibraryData());
            libraryOptionPanel.setPreferredSize(new Dimension(500, 500));
            
            manageLibraryButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    libraryOptionPanel,I18n.tr("Manage Library"), I18n.tr("Manage Library"), 
                    I18n.tr("Manage what folders are loaded into your library")));
            
            add(new MultiLineLabel(I18n.tr("Your Library is a central location to view, share and unshare your files with the P2P Network and your friends."), 700), "wrap");
            add(new MultiLineLabel(I18n.tr("LimeWire will scan folders and look for files to add to your Library. Scanning folders into your Library will not automatically share them."), 600), "wrap");
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
}
