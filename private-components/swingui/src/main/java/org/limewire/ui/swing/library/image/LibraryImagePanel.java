package org.limewire.ui.swing.library.image;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.jdesktop.swingx.VerticalLayout;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;


public class LibraryImagePanel  extends JPanel implements ListEventListener<List<LocalFileItem>> {

    
 
    private final EventList<LocalFileItem> currentEventList;

    private final GroupingList<LocalFileItem> groupingList;

    private final Comparator<LocalFileItem> groupingComparator = new Comparator<LocalFileItem>() {
        @Override
        public int compare(LocalFileItem o1, LocalFileItem o2) {
            return o1.getFile().getParent().compareTo(o2.getFile().getParent());
        }
    };
    

    private final Map<String, EventList<LocalFileItem>> listMap;
    private final Map<String, LibraryImageSubPanel> panelMap;

    private final LocalFileList fileList;

    private final Icon panelIcon;

    private final ThumbnailManager thumbnailManager;
    
    private final String incomplete = I18n.tr("Incomplete Files");

    private ImageLibraryPopupParams params;
    
    
    public LibraryImagePanel(String name, ImageLibraryPopupParams params, EventList<LocalFileItem> eventList, LocalFileList fileList, Icon panelIcon, ThumbnailManager thumbnailManager) {       
        super(new VerticalLayout());
        GuiUtils.assignResources(this); 
        this.fileList = fileList;
        this.panelIcon = panelIcon;
        this.thumbnailManager = thumbnailManager;
        this.currentEventList = eventList;
        this.params = params;
        groupingList = GlazedListsFactory.groupingList(eventList, groupingComparator);
        
        groupingList.addListEventListener(this);

        listMap = new ConcurrentHashMap<String, EventList<LocalFileItem>>();
        panelMap = new ConcurrentHashMap<String, LibraryImageSubPanel>();
        
    }
    public void dispose() {
        // TODO Auto-generated method stub
        
    }

    //TODO: thread safety
    @Override
    public void listChanged(ListEvent<List<LocalFileItem>> listChanges) {
        while (listChanges.next()){
            if (listChanges.getType() == ListEvent.INSERT){
                //INSERT adds sublist to the GroupingList - UPDATEs are also fired for each file added to the sublist
                final String parent = getParent(listChanges.getSourceList().get(listChanges.getIndex()).get(0));
                final EventList<LocalFileItem> newList = GlazedListsFactory.filterList(currentEventList, new DirectoryMatcher(parent));
                listMap.put(parent, newList);
//                System.out.println("INSERT " + parent + " : " + newList);
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        LibraryImageSubPanel subPanel = new LibraryImageSubPanel(parent, newList, 
                                fileList, panelIcon, thumbnailManager, params);
                        panelMap.put(parent, subPanel);
                        add(subPanel);
                    }
                });
            } //else if (listChanges.getType() == ListEvent.DELETE){
//                //TODO - delete row
//            } else if (listChanges.getType() == ListEvent.UPDATE){
//                List<LocalFileItem> subList = listChanges.getSourceList().get(listChanges.getIndex());
//                Iterator<LocalFileItem>  iterator = subList.iterator();
//                while (iterator.hasNext()) {
//                    LocalFileItem fileItem = iterator.next();
//                    System.out.println("UPDATE LIST - " + fileItem);
//                    panelMap.get(getParent(fileItem)).getModel().addFileItem(fileItem);
//                }
//            }
        }
        
    }
    
    
    
    private String getParent(LocalFileItem localFileItem){
      //  System.out.println(localFileItem + " parent is  " + (localFileItem.isIncomplete()? incomplete : localFileItem.getFile().getParent()));
        return localFileItem.isIncomplete()? incomplete : localFileItem.getFile().getParent();
    }
    
    private class DirectoryMatcher implements Matcher<LocalFileItem>{
        
        private String parentDirectory;

        public DirectoryMatcher(String parentDirectory){
            this.parentDirectory = parentDirectory;
        }

        @Override
        public boolean matches(LocalFileItem item) {
            return getParent(item).equals(parentDirectory);
        }
        
    }
    
}