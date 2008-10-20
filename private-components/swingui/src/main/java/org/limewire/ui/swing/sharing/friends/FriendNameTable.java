package org.limewire.ui.swing.sharing.friends;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.DropMode;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Table for displaying a list of friends in the shared view.
 * The table consists of a list of names and number of files being shared.
 * The table is sorted based on the first if files are being share or not
 * and then by alphabetical order. 
 */
@Singleton
public class FriendNameTable extends JXTable {

    @Resource
    private Color tableBackgroundColor;
    
    private EventTableModel<FriendItem> tableModel;
    
    @Inject
    public FriendNameTable( RemoteLibraryManager remoteLibraryManager, LibraryManager libraryManager, final ShareListManager shareListManager, Navigator navigator) {
        GuiUtils.assignResources(this);
                
        setBackground(tableBackgroundColor);
        setColumnControlVisible(false);
        setTableHeader(null);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowGrid(false, false);
        setColumnSelectionAllowed(false);

        setTransferHandler(new FriendTransferHandler(shareListManager));
        setDropMode(DropMode.ON);
    }
    
    public void addPopupListener(final TablePopupHandler handler) {
          addMouseListener(new MouseAdapter() {
          
          @Override
          public void mouseReleased(MouseEvent e) {
              maybeShowPopup(e);
          }
    
          @Override
          public void mousePressed(MouseEvent e) {
              maybeShowPopup(e);
          }
    
          private void maybeShowPopup(MouseEvent e) {
              if (e.isPopupTrigger()) {
                  int col = columnAtPoint(e.getPoint());
                  int row = rowAtPoint(e.getPoint());
                  if(row < 0 || row >= getRowCount())
                      return;
                  
                  if(row != getSelectedRow()) {
                      setRowSelectionInterval(row, row);
                  }
                  if (row >= 0 && col >= 0) {
                      handler.maybeShowPopup(
                          e.getComponent(), e.getX(), e.getY());
                  }
              }
          }
      });
    }
    
    public void setTableModel(EventList<FriendItem> eventList, TableFormat<FriendItem> tableFormat) {
        SortedList<FriendItem> friendList = GlazedListsFactory.sortedList(eventList, new FriendComparator());       
        tableModel = new EventTableModel<FriendItem>(friendList, tableFormat);
        
        setModel(tableModel);
        
        getColumn(0).setCellRenderer(new FriendNameRenderer());
        getColumn(1).setCellRenderer(new FriendNameRenderer());
        
        getColumn(0).setPreferredWidth(110);
        getColumn(1).setWidth(30);
        getColumn(1).setPreferredWidth(30);
    }
    
    public EventTableModel<FriendItem> getEventTableModel() {
        return tableModel;
    }
    
    /**
     * Transfer handler to allow dropping files onto names in the friends list.
     * Delegates to SharingTransferHandler to know if it canImport, or importData.
     * On dragging, the row we are on is selected and checked to see if import is possible.
     */
    private final class FriendTransferHandler extends TransferHandler {
        private final ShareListManager shareListManager;

        private final SharingTransferHandler sharingTransferHandler;

        private FriendTransferHandler(ShareListManager shareListManager) {
            this.shareListManager = shareListManager;
            this.sharingTransferHandler = new SharingTransferHandler(null);
        }

        @Override
        public boolean canImport(final TransferSupport support) {
           JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
           int row = dropLocation.getRow();
           
           if(row < tableModel.getRowCount() && row >= 0) {
               if(row != getSelectedRow()) {
                   setRowSelectionInterval(row, row);
               }
               FriendItem friendItem = tableModel.getElementAt(row);
               Friend friend = friendItem.getFriend();
               FriendFileList friendFileList =  shareListManager.getFriendShareList(friend);
               sharingTransferHandler.setModel(friendFileList);   
               boolean canImport = sharingTransferHandler.canImport(support);
               return canImport;
           }

           return false;
        }

        @Override
        public boolean importData(TransferSupport support) {
            JTable.DropLocation dropLocation = (JTable.DropLocation) support.getDropLocation();
            int row = dropLocation.getRow();
            FriendItem friendItem = tableModel.getElementAt(row);
            Friend friend = friendItem.getFriend();
            FriendFileList friendFileList =  shareListManager.getFriendShareList(friend);
            sharingTransferHandler.setModel(friendFileList);
            return sharingTransferHandler.importData(support);
        }
    }

    /**
     * Sorts the FriendNameTable. All items are first sorted on whether
     * they are sharing any files. Those that are sharing are displayed first,
     * those that aren't are displayed second. Then they are sorted based on
     * alphabetical order. This results in two alphabetical sorted lists where
     * the first list is sharing files and the second list isn't sharing files.
     */
    private static class FriendComparator implements Comparator<FriendItem> {
        @Override
        public int compare(FriendItem o1, FriendItem o2) {
            if(o1.getShareListSize() > 0 && o2.getShareListSize() > 0) { 
                return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
            } else if(o1.getShareListSize() > 0 && o2.getShareListSize() <= 0) {
                return -1;
            } else if(o1.getShareListSize() <= 0 && o2.getShareListSize() > 0) {
                return 1;
            } else {
                return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
            }
        }
    }
}
