package org.limewire.ui.swing.library;

import javax.swing.Action;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class LibraryPanel extends AbstractFileListPanel {

    public LibraryPanel(LimeHeaderBarFactory headerBarFactory) {
        super(headerBarFactory);
    }
    
    @Override
    protected LimeHeaderBar createHeaderBar(LimeHeaderBarFactory headerBarFactory) {
        return headerBarFactory.createBasic();
    }
    
    @Override
    protected LimePromptTextField createFilterField(String prompt) {
        return new LimePromptTextField(prompt);
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    private static class ButtonSizeListener<T> implements Disposable, ListEventListener<T> {
        private final Category category;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.list = list;
            setText();
        }

        private void setText() {
            action.putValue(Action.NAME, I18n.tr(category.toString()) + " (" + list.size() + ")");
            //disable other category if size is 0
            if(category == Category.OTHER) {
                action.setEnabled(list.size() > 0);
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }
    }        
}
