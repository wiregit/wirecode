package org.limewire.ui.swing.sharing.fancy;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FileItem.Category;
import org.limewire.ui.swing.sharing.SharingShortcutPanel;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.sharing.table.SharingFancyAudioTableFormat;
import org.limewire.ui.swing.sharing.table.SharingFancyDefaultTableFormat;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

public class SharingFancyPanel extends JPanel {

    private static final String music = "Music";
    private static final String video = "Videos";
    private static final String image = "Images";
    private static final String doc = "Documents";
    private static final String program = "Program";
    private static final String other = "Other";
    
    private SharingFancyTablePanel musicTable;
    private SharingFancyTablePanel videoTable;
    private SharingFancyListPanel imageList;
    private SharingFancyTablePanel documentTable;
    private SharingFancyTablePanel programTable;
    private SharingFancyTablePanel otherTable;
    //TODO: what to do about programs??
    
    private final JScrollPane scrollPane;
    
    public SharingFancyPanel(EventList<FileItem> eventList, JScrollPane scrollPane, FileList originalList) {

        this.scrollPane = scrollPane;
        
        ShareDropTarget drop = new ShareDropTarget(this, originalList);

        List<EventList<FileItem>> list = new ArrayList<EventList<FileItem>>();
        list.add(new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.AUDIO)));
        list.add(new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.VIDEO)));
        list.add(new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.IMAGE)));
        list.add(new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.DOCUMENT)));
        list.add(new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.PROGRAM)));
        list.add(new FilterList<FileItem>(eventList, new CategoryFilter(FileItem.Category.OTHER)));
        
        musicTable = new SharingFancyTablePanel(music, list.get(0), new SharingFancyAudioTableFormat(), drop.getDropTarget(), originalList);
        videoTable = new SharingFancyTablePanel(video, list.get(1), new SharingFancyDefaultTableFormat(),false, drop.getDropTarget(), originalList);
        imageList = new SharingFancyListPanel(image, list.get(2), drop.getDropTarget(), originalList);
        documentTable = new SharingFancyTablePanel(doc, list.get(3), new SharingFancyDefaultTableFormat(), false, drop.getDropTarget(), originalList);
        programTable = new SharingFancyTablePanel(program, list.get(4), new SharingFancyDefaultTableFormat(), false, drop.getDropTarget(), originalList);
        otherTable = new SharingFancyTablePanel(other, list.get(5), new SharingFancyDefaultTableFormat(), drop.getDropTarget(), originalList);
        
        
        SharingShortcutPanel shortcuts = new SharingShortcutPanel(
                new String[]{music, video, image, doc, program, other},
                new Action[]{new BookmarkJumpAction(this, musicTable),
                             new BookmarkJumpAction(this, videoTable),
                             new BookmarkJumpAction(this, imageList),
                             new BookmarkJumpAction(this, documentTable),
                             new BookmarkJumpAction(this, programTable),
                             new BookmarkJumpAction(this, otherTable)},
                list);

       setLayout(new VerticalLayout());
        
       add(shortcuts);
        
       add(musicTable);
       add(videoTable);
       add(imageList);
       add(documentTable);
       add(programTable);
       add(otherTable);

       this.setDropTarget(drop.getDropTarget());
    }
    
    /**
     * Filter based on a category. Only displays FileItems of 
     * one category type.
     */
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
    
    /**
     * When the action is performed, the scrollpane is repositioned 
     * to display the linkComponent in the ViewPort. If no scrollBar
     * is currently displayed, the action will not be performed.
     */
    private class BookmarkJumpAction extends AbstractAction {

        private JComponent parentContainer;
        private JComponent linkContainer;
        
        public BookmarkJumpAction(JComponent parentComponent, JComponent linkComponent) {
            this.parentContainer = parentComponent;
            this.linkContainer = linkComponent;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if( parentContainer.getVisibleRect().height < parentContainer.getPreferredSize().height ) {
                int position = linkContainer.getLocation().y;
                if(position + parentContainer.getVisibleRect().height > parentContainer.getHeight())
                    position = parentContainer.getSize().height - parentContainer.getVisibleRect().height;

                scrollPane.getViewport().setViewPosition(new Point(parentContainer.getLocation().x, position));
            }
        }
    }
}
