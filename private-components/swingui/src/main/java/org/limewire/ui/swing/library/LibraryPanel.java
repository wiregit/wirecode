package org.limewire.ui.swing.library;

import javax.swing.Action;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.TextFieldDecorator;

import ca.odell.glazedlists.FilterList;

class LibraryPanel extends AbstractFileListPanel {

    public LibraryPanel(LimeHeaderBarFactory headerBarFactory, TextFieldDecorator textFieldDecorator) {
        super(headerBarFactory, textFieldDecorator);
    }
    
    @Override
    protected LimeHeaderBar createHeaderBar(LimeHeaderBarFactory headerBarFactory) {
        return headerBarFactory.createBasic();
    }
    
    @Override
    protected LimePromptTextField createFilterField(TextFieldDecorator decorator, String prompt) {
        LimePromptTextField field = new LimePromptTextField(prompt);
        decorator.decorateClearablePromptField(field, AccentType.SHADOW);
        return field;
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category, Action action,
            FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
    }
}
