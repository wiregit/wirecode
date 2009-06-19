package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.AllTableFormat;
import org.limewire.ui.swing.library.table.AudioTableFormat;
import org.limewire.ui.swing.library.table.DocumentTableFormat;
import org.limewire.ui.swing.library.table.ImageTableFormat;
import org.limewire.ui.swing.library.table.OtherTableFormat;
import org.limewire.ui.swing.library.table.ProgramTableFormat;
import org.limewire.ui.swing.library.table.VideoTableFormat;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryTableComboBox extends LimeComboBox {
    
    private final CategoryIconManager categoryIconManager;
    
    @Inject
    public LibraryTableComboBox(Provider<AllTableFormat<LocalFileItem>> allFormat, 
            Provider<AudioTableFormat<LocalFileItem>> audioFormat,
            Provider<VideoTableFormat<LocalFileItem>> videoFormat,
            Provider<ImageTableFormat<LocalFileItem>> imageFormat,
            Provider<DocumentTableFormat<LocalFileItem>> documentFormat,
            Provider<ProgramTableFormat<LocalFileItem>> programFormat,
            Provider<OtherTableFormat<LocalFileItem>> otherFormat,
            CategoryIconManager categoryIconManager) {
        
        this.categoryIconManager = categoryIconManager;
        
        addAction(new ComboBoxAction(I18n.tr("All"), null,  allFormat));
        addAction(new ComboBoxAction(I18n.tr("Audio"), Category.AUDIO,audioFormat));
        addAction(new ComboBoxAction(I18n.tr("Video"), Category.VIDEO, videoFormat));
        addAction(new ComboBoxAction(I18n.tr("Image"), Category.IMAGE, imageFormat));
        addAction(new ComboBoxAction(I18n.tr("Document"), Category.DOCUMENT, documentFormat));
        addAction(new ComboBoxAction(I18n.tr("Programs"), Category.PROGRAM, programFormat));
        addAction(new ComboBoxAction(I18n.tr("Other"), Category.OTHER, otherFormat));
    }
    
    public AbstractLibraryFormat<LocalFileItem> getSelectedTableFormat() {
        ComboBoxAction action = (ComboBoxAction) getSelectedAction();
        return action.getTableFormat();
    }
    
    public Category getSelectedCategory() {
        ComboBoxAction action = (ComboBoxAction) getSelectedAction();
        return action.getCategory();
    }
    
    private class ComboBoxAction extends AbstractAction {

        private final Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat;
        private final Category category;
        
        public ComboBoxAction(String displayName, Category category,
                Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat) {
            super(displayName);
            if(category != null)
                putValue(SMALL_ICON, categoryIconManager.getIcon(category));
            
            this.category =  category;
            this.tableFormat = tableFormat;
        }
        
        public AbstractLibraryFormat<LocalFileItem> getTableFormat() {
            return tableFormat.get();
        }
        
        public Category getCategory() {
            return category;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
        }        
    }
}
