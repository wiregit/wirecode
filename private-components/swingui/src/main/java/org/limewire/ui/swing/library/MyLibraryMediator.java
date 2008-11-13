package org.limewire.ui.swing.library;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MyLibraryMediator extends BaseLibraryMediator {

    private MyLibraryFactory factory;
    
    @Inject
    public MyLibraryMediator(MyLibraryFactory factory, FriendComboBox comboBox) {
        super();
        this.factory = factory;
        
        comboBox.setBasePanel(this);
    }
    
    public void setMainCardEventList(EventList<LocalFileItem> eventList) {
        setMainCard(factory.createMyLibrary(eventList));
    }
}
