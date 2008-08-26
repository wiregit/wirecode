package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.components.Circle;
import org.limewire.ui.swing.components.NumberedHyperLinkButton;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SharingShortcutPanel extends JPanel {

    private String[] names;
     
    public SharingShortcutPanel(String[] names, Action[] actions, List<EventList<FileItem>> models) {
        this.names = names;
        
        createComponents(actions, models);
    }
    
    private void createComponents(Action[] actions, List<EventList<FileItem>> models) {
        for(int i = 0; i < names.length; i++) {
            final NumberedHyperLinkButton button = new NumberedHyperLinkButton(names[i], actions[i]);
            button.setForegroundColor(Color.BLUE);
            button.setMouseOverColor(Color.GREEN);
            button.setDisplayNumber(0);
            models.get(i).addListEventListener(new ListEventListener<FileItem>(){
                @Override
                public void listChanged(ListEvent<FileItem> listChanges) {
                    final int size = listChanges.getSourceList().size();
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            button.setDisplayNumber(size);                                
                        }
                    });
                }
            });
            add(button);
            if(i + 1 < names.length)
                add(new Circle(8));
        }
    }
}