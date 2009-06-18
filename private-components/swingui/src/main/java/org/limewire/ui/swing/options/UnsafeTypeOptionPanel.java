package org.limewire.ui.swing.options;

import java.util.Collection;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileItemFilter;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

public class UnsafeTypeOptionPanel extends OptionPanel {

    private JCheckBox programCheckBox;
    private JCheckBox documentCheckBox;
    private JButton okButton;
    private final LibraryManager libraryManager;
    private final SharedFileListManager shareListManager;
   
    public UnsafeTypeOptionPanel(Action okButtonAction, LibraryManager libraryManager,
            SharedFileListManager shareListManager) {
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;

        setLayout(new MigLayout("gapy 10"));
        
        programCheckBox = new JCheckBox(I18n.tr("Allow me to search for and share programs with the P2P Network and my friends"));
        documentCheckBox = new JCheckBox(I18n.tr("Allow me to share documents with the P2P Network"));
        okButton = new JButton(okButtonAction);
        
        add(new JLabel(I18n.tr("Enabling these settings makes you more prone to viruses and accidently sharing private documents:")), "span 2, wrap");
        
        add(programCheckBox, "split, gapleft 25, wrap");
        add(documentCheckBox, "split, gapbottom 15, gapleft 25, wrap");
        
        add(new JLabel(I18n.tr("By default, LimeWire allows you to share documents with your friends")), "push");
        add(okButton);
    }
    
    @Override
    boolean applyOptions() {
        LibrarySettings.ALLOW_PROGRAMS.setValue(programCheckBox.isSelected());
        LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.setValue(documentCheckBox.isSelected());

        if(!programCheckBox.isSelected()) {
        	Collection<Category> managedCategories = libraryManager.getLibraryData().getManagedCategories();
        	managedCategories.remove(Category.PROGRAM);
        	libraryManager.getLibraryData().setCategoriesToIncludeWhenAddingFolders(managedCategories);
        	BackgroundExecutorService.execute(new Runnable() {
        	   @Override
        	    public void run() {
        	       libraryManager.getLibraryManagedList().removeFiles(new LocalFileItemFilter() {
                       @Override
                        public boolean accept(LocalFileItem localFileItem) {
                            return localFileItem.getCategory() == Category.PROGRAM;
                        } 
                    });
        	    } 
        	});
        }
        
        if (!documentCheckBox.isSelected()) {
            BackgroundExecutorService.execute(new Runnable() {
               @Override
                public void run() {
                   EventList<SharedFileList> shareLists = shareListManager.getModel();
                   shareLists.getReadWriteLock().readLock().lock();
                   try {
                       for(SharedFileList sharedFileList : shareLists) {
                           if(sharedFileList.isPublic()) {
                               sharedFileList.removeFiles(new LocalFileItemFilter() {
                                   @Override
                                   public boolean accept(LocalFileItem localFileItem) {
                                       return localFileItem.getCategory() == Category.DOCUMENT;
                                   } 
                               });
                           }
                       }
                   } finally {
                       shareLists.getReadWriteLock().readLock().unlock();
                   }
                } 
            });
        }
        return false;
    }

    @Override
    boolean hasChanged() {
        return LibrarySettings.ALLOW_PROGRAMS.getValue() != programCheckBox.isSelected() 
                || LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue() != documentCheckBox.isSelected();
    }

    @Override
    public void initOptions() {
        programCheckBox.setSelected(LibrarySettings.ALLOW_PROGRAMS.getValue());
        documentCheckBox.setSelected(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue());
    }
}
