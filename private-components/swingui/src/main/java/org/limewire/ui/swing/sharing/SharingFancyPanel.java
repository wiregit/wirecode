package org.limewire.ui.swing.sharing;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Category;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

public class SharingFancyPanel extends JPanel {

    private SharingFancyTablePanel musicTable;
    private SharingFancyTablePanel videoTable;
    private SharingFancyTablePanel imageTable;
    private SharingFancyTablePanel documentTable;
    private SharingFancyTablePanel otherTable;
    //TODO: what to do about programs??
    
    public SharingFancyPanel(EventList<FileItem> eventList) {
        musicTable = new SharingFancyTablePanel("Music", new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.AUDIO)));
        videoTable = new SharingFancyTablePanel("Videos", new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.VIDEO)), false);
        imageTable = new SharingFancyTablePanel("Images", new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.IMAGE)));
        documentTable = new SharingFancyTablePanel("Documents", new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.DOCUMENT)), false);
        otherTable = new SharingFancyTablePanel("Other", new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.OTHER)));
        
        setLayout(new VerticalLayout());
        
        add(musicTable);
        add(videoTable);
        add(imageTable);
        add(documentTable);
        add(otherTable);
    }
    
    private class CategoryFilter implements Matcher<FileItem>{
        private Category category;
        
        public CategoryFilter(Category category) {
            this.category = category;
        }

        @Override
        public boolean matches(FileItem item) {
            if(item == null) return false;
            if(category == null) return true;
            
            return item.getCategory().equals(category);
        }
    }
    
    public static void main(String argsp[]) {
        JFrame f = new JFrame();
        f.setSize(500,500);
        f.add(new SharingFancyPanel(null));
        f.setDefaultCloseOperation(2);
        f.setVisible(true);
    }
}
