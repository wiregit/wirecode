package org.limewire.ui.swing.sharing.fancy;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.sharing.SharingShortcutPanel;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.sharing.table.CategoryFilter;
import org.limewire.ui.swing.sharing.table.SharingFancyAudioTableFormat;
import org.limewire.ui.swing.sharing.table.SharingFancyDefaultTableFormat;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;

public class SharingFancyPanel extends JPanel {

    @Resource
    private Icon audioIcon;
    @Resource
    private Icon videoIcon;
    @Resource
    private Icon documentIcon;
    @Resource
    private Icon appIcon;
    @Resource
    private Icon imageIcon;
    
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
    
    private SharingShortcutPanel shortcuts;
    
    private final JScrollPane scrollPane;
    
    public SharingFancyPanel(EventList<LocalFileItem> eventList, JScrollPane scrollPane, LocalFileList originalList) {

        GuiUtils.assignResources(this); 
        
        this.scrollPane = scrollPane;
        
        ShareDropTarget drop = new ShareDropTarget(this, originalList);

        List<EventList<LocalFileItem>> list = new ArrayList<EventList<LocalFileItem>>();
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.AUDIO)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.VIDEO)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.IMAGE)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.DOCUMENT)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.PROGRAM)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.OTHER)));
        
        musicTable = new SharingFancyTablePanel(music, list.get(0), new SharingFancyAudioTableFormat(), drop.getDropTarget(), originalList, audioIcon);
        videoTable = new SharingFancyTablePanel(video, list.get(1), new SharingFancyDefaultTableFormat(),false, drop.getDropTarget(), originalList, videoIcon);
        imageList = new SharingFancyListPanel(image, list.get(2), drop.getDropTarget(), originalList, imageIcon);
        documentTable = new SharingFancyTablePanel(doc, list.get(3), new SharingFancyDefaultTableFormat(), false, drop.getDropTarget(), originalList, documentIcon);
        programTable = new SharingFancyTablePanel(program, list.get(4), new SharingFancyDefaultTableFormat(), false, drop.getDropTarget(), originalList, appIcon);
        otherTable = new SharingFancyTablePanel(other, list.get(5), new SharingFancyDefaultTableFormat(), drop.getDropTarget(), originalList, appIcon);
        
        
        shortcuts = new SharingShortcutPanel(
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
    
    public void setModel(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        //TODO: these need to be lazily created and stored somewhere, FileList?? so we don't keep recreating them
        //		GlazedLists.multiMap would be perfect if they supported EventLists instead of Lists
        List<EventList<LocalFileItem>> list = new ArrayList<EventList<LocalFileItem>>();
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.AUDIO)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.VIDEO)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.IMAGE)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.DOCUMENT)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.PROGRAM)));
        list.add(new FilterList<LocalFileItem>(eventList, new CategoryFilter(FileItem.Category.OTHER)));
        
        musicTable.setModel(list.get(0), fileList);
        videoTable.setModel(list.get(1), fileList);
        imageList.setModel(list.get(2), fileList);
        documentTable.setModel(list.get(3), fileList);
        programTable.setModel(list.get(4), fileList);
        otherTable.setModel(list.get(5), fileList);
        
        shortcuts.setModel(list);
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
