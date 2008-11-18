package org.limewire.ui.swing.library.image;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.VerticalLayout;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.LibrarySelectable;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GroupingList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;


public class LibraryImagePanel extends JPanel implements ListEventListener<List<LocalFileItem>>, Disposable, LibrarySelectable, Scrollable {
    
    private LibraryImageSubPanelFactory factory;
    private LibrarySharePanel sharePanel;
    
    private final EventList<LocalFileItem> currentEventList;

    private final GroupingList<LocalFileItem> groupingList;

    private final Comparator<LocalFileItem> groupingComparator = new Comparator<LocalFileItem>() {
        @Override
        public int compare(LocalFileItem o1, LocalFileItem o2) {
            if (o1.getFile() != null) {
                return (o2.getFile() != null) ? o1.getFile().getParent().compareTo(o2.getFile().getParent()) : 1;
            } else  {
                return (o2.getFile() != null) ? -1 : 0;
            }
        }
    };
    

    private final Map<String, EventList<LocalFileItem>> listMap;
    private final Map<String, LibraryImageSubPanel> panelMap;

    private final LocalFileList fileList;
    private final LocalFileList currentFriendFileList;

    private final Icon panelIcon;
    
    private final String incomplete = I18n.tr("Incomplete Files");

    private ImageLibraryPopupParams params;
    
    private JScrollPane scrollPane;
    
    public LibraryImagePanel(String name, ImageLibraryPopupParams params, EventList<LocalFileItem> eventList, 
            LocalFileList fileList, Icon panelIcon, JScrollPane scrollPane,
            LibraryImageSubPanelFactory factory,
            LibrarySharePanel sharePanel,
            LocalFileList selectedFriendList) {       
        super(new VerticalLayout());
        
        GuiUtils.assignResources(this); 
        
        this.fileList = fileList;
        this.panelIcon = panelIcon;
        this.currentEventList = eventList;
        this.params = params;
        this.scrollPane = scrollPane;
        this.factory = factory;
        this.sharePanel = sharePanel;
        this.currentFriendFileList = selectedFriendList;
        
        groupingList = GlazedListsFactory.groupingList(eventList, groupingComparator);
        
        groupingList.addListEventListener(this);

        listMap = new ConcurrentHashMap<String, EventList<LocalFileItem>>();
        panelMap = new ConcurrentHashMap<String, LibraryImageSubPanel>();
    
        initList();
    }
    
    public void dispose() {
        for(LibraryImageSubPanel subPanel : panelMap.values()){
            subPanel.dispose();
        }
        panelMap.clear();
        groupingList.removeListEventListener(this);
        groupingList.dispose();
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
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        createSubPanel(parent, newList);
                    }
                });
            } 
        }       
    }
    
    private void initList() {
        for( List<LocalFileItem> fileItemList : groupingList) {
            final String parent = getParent(fileItemList.get(0));
            final EventList<LocalFileItem> newList = GlazedListsFactory.filterList(currentEventList, new DirectoryMatcher(parent));
            listMap.put(parent, newList);
            createSubPanel(parent, newList);
        }
    }
    
    
    private void createSubPanel(String parent, EventList<LocalFileItem> list){
        LibraryImageSubPanel subPanel;
        if(sharePanel != null )
            subPanel = factory.createMyLibraryImageSubPanel(parent, list, fileList, panelIcon, params, sharePanel);
        else {
            subPanel = factory.createSharingLibraryImageSubPanel(parent, list, fileList, panelIcon, params, currentFriendFileList);
        }
        panelMap.put(parent, subPanel);
        add(subPanel);
    }
    
    private String getParent(LocalFileItem localFileItem){
        return localFileItem.isIncomplete() ? incomplete : 
            ((localFileItem.getFile() == null) ? incomplete : localFileItem.getFile().getParent());
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

    @Override
    public void selectAndScroll(Object selectedObject) {
        //TODO selectAndScroll for library image table
        throw new RuntimeException("Implement me");
    }
    /**
     * Overrides getPreferredSize. getPreferredSize is used by scrollable to 
     * determine how big to make the scrollableViewPort. Uses the parent 
     * component to set the appropriate size thats currently visible. This
     * prevents the image list from growing too wide and never shrinking again.
     */
    @Override
    public Dimension getPreferredSize() {
        //ensure viewport is filled so dnd will work
        Dimension dimension = super.getPreferredSize();
        if (getParent() == null)
            return dimension;
        if (dimension.height > scrollPane.getSize().height){
            return new Dimension(scrollPane.getWidth(), dimension.height);
        } else {
            return scrollPane.getSize(); 
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }
    
    /**
     * Scrolls to the appropriate location based on the height of a thumbnail image.
     */
    private int getPosition(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL)
            currentPosition = visibleRect.x;
        else
            currentPosition = visibleRect.y;
    
        //TODO: read this from ImageList again
        int height = 134;//imageList.getList().getFixedCellHeight();
        
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / height) * height;
            return (newPosition == 0) ? height : newPosition;
        } else {
            return ((currentPosition / height) + 1) * height - currentPosition;
        }
    }
    
}