package org.limewire.ui.swing.sharing;

import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Keys;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.TextFilterator;

public abstract class GenericSharingPanel extends JPanel {

    protected static final String EMPTY = "EMPTY";
    protected static final String NONEMPTY = "NONEMPTY";
    protected static final String TABLE = "TABLE";
    protected static final String LIST = "LIST";
    
    protected JXTable table;

    public GenericSharingPanel() {
        GuiUtils.assignResources(this); 
    }
    
    protected class ItemAction implements ItemListener {

        private final JComponent component;
        private final CardLayout cardLayout;
        private final String cardName;
        
        public ItemAction(JComponent component, CardLayout cardLayout, String cardName) {
            this.component = component;
            this.cardLayout = cardLayout;
            this.cardName = cardName;
        }
        
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                cardLayout.show(component, cardName);
            }
        } 
    }
    
    protected class SharingTextFilterer implements TextFilterator<FileItem> {
        @Override
        public void getFilterStrings(List<String> baseList, FileItem element) {
           baseList.add(element.getName());
           baseList.add((String)element.getProperty(Keys.ALBUM));
           baseList.add((String)element.getProperty(Keys.TITLE));
           //TODO: finish linking properties here
        }
    }

}
