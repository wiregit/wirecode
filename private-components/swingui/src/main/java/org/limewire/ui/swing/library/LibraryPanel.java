package org.limewire.ui.swing.library;

import javax.swing.Action;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
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
    protected <T extends FileItem> void addCategorySizeListener(String categoryName,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(categoryName, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    private static class ButtonSizeListener<T> implements Disposable, ListEventListener<T> {
        private final String text;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(String text, Action action, FilterList<T> list) {
            this.text = text;
            this.action = action;
            this.list = list;
            setText();
        }

        private void setText() {
            action.putValue(Action.NAME, I18n.tr(text) + " (" + list.size() + ")");
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
