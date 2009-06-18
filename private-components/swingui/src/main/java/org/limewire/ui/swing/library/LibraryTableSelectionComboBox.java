package org.limewire.ui.swing.library;

import javax.swing.JComboBox;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.AllTableFormat;
import org.limewire.ui.swing.library.table.AudioTableFormat;
import org.limewire.ui.swing.library.table.DocumentTableFormat;
import org.limewire.ui.swing.library.table.ImageTableFormat;
import org.limewire.ui.swing.library.table.OtherTableFormat;
import org.limewire.ui.swing.library.table.ProgramTableFormat;
import org.limewire.ui.swing.library.table.VideoTableFormat;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class LibraryTableSelectionComboBox extends JComboBox {

    @Inject
    public LibraryTableSelectionComboBox(Provider<AllTableFormat<LocalFileItem>> allFormat, 
            Provider<AudioTableFormat<LocalFileItem>> audioFormat,
            Provider<VideoTableFormat<LocalFileItem>> videoFormat,
            Provider<ImageTableFormat<LocalFileItem>> imageFormat,
            Provider<DocumentTableFormat<LocalFileItem>> documentFormat,
            Provider<ProgramTableFormat<LocalFileItem>> programFormat,
            Provider<OtherTableFormat<LocalFileItem>> otherFormat) {
        
            addItem(new ComboBoxItem(I18n.tr("All"), null, allFormat));
            addItem(new ComboBoxItem(I18n.tr("Audio"), Category.AUDIO, audioFormat));
            addItem(new ComboBoxItem(I18n.tr("Video"), Category.VIDEO, videoFormat));
            addItem(new ComboBoxItem(I18n.tr("Image"), Category.IMAGE, imageFormat));
            addItem(new ComboBoxItem(I18n.tr("Document"), Category.DOCUMENT, documentFormat));
            addItem(new ComboBoxItem(I18n.tr("Programs"), Category.PROGRAM, programFormat));
            addItem(new ComboBoxItem(I18n.tr("Other"), Category.OTHER, otherFormat));
    }
    
    public AbstractLibraryFormat<LocalFileItem> getSelectedTableFormat() {
        ComboBoxItem item = (ComboBoxItem) getSelectedItem();
        return item.getTableFormat();
    }
    
    public Category getSelectedCategory() {
        ComboBoxItem item = (ComboBoxItem) getSelectedItem();
        return item.getCategory();
    }
    
    
    private class ComboBoxItem {
        private final String displayText;
        private final Category category;
        private final Provider<? extends AbstractLibraryFormat<LocalFileItem>> providerTableFormat;
        
        private AbstractLibraryFormat<LocalFileItem> tableFormat;
        
        public ComboBoxItem(String text, Category category, Provider<? extends AbstractLibraryFormat<LocalFileItem>> tableFormat) {
            this.displayText = text;
            this.category = category;
            this.providerTableFormat = tableFormat;
        }
        
        public Category getCategory() {
            return category;
        }
        
        public AbstractLibraryFormat<LocalFileItem> getTableFormat() {
            if(tableFormat == null)
                tableFormat = providerTableFormat.get();
            return tableFormat;
        }
        
        @Override
        public String toString() {
            return displayText;
        }
    }
}
