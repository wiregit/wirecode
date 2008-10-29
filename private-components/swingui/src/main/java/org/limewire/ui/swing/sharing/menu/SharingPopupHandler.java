package org.limewire.ui.swing.sharing.menu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.images.ImageListModel;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.sharing.table.SharingTableModel;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

public class SharingPopupHandler implements TablePopupHandler {

    private int popupRow = -1;
    
    protected JPopupMenu popupMenu;
    protected JMenuItem unshareMenuItem;
    protected JMenuItem locateFileItem;
    protected JMenuItem propertiesItem;
    
    private JTable table;
    private JList list;
    
    private final SharingActionHandler actionHandler;
    
    protected final MenuListener menuListener;
    private final PropertiesFactory<LocalFileItem> localFileItemPropsFactory;
    
    private LocalFileItem fileItem;
    private LocalFileList fileList;
    
    public SharingPopupHandler(JList list, SharingActionHandler handler, PropertiesFactory<LocalFileItem> localFileItemPropsFactory) {
        this.list = list;
        this.localFileItemPropsFactory = localFileItemPropsFactory;
        this.actionHandler = handler;
        this.menuListener = new MenuListener();
        
        initialize();
    }
    
    public SharingPopupHandler(JTable table, SharingActionHandler handler, PropertiesFactory<LocalFileItem> localFileItemPropsFactory) {
        this.table = table;
        this.localFileItemPropsFactory = localFileItemPropsFactory;
        this.actionHandler = handler;
        this.menuListener = new MenuListener();
        
        initialize();
    }
    
    protected void initialize() {
        popupMenu = new JPopupMenu();
        
        unshareMenuItem = new JMenuItem(I18n.tr("Unshare"));
        unshareMenuItem.setActionCommand(SharingActionHandler.UNSHARE);
        unshareMenuItem.addActionListener(menuListener);
        
        locateFileItem = new JMenuItem(I18n.tr("Locate"));
        locateFileItem.setActionCommand(SharingActionHandler.LOCATE);
        locateFileItem.addActionListener(menuListener);
        
        propertiesItem = new JMenuItem(I18n.tr("Properties"));
        propertiesItem.setActionCommand(SharingActionHandler.PROPERTIES);
        propertiesItem.addActionListener(menuListener);
        
        popupMenu.add(unshareMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(locateFileItem);
        popupMenu.add(propertiesItem);
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        if(table != null) {
            popupRow = table.rowAtPoint(new Point(x, y));
            fileItem = ((SharingTableModel) table.getModel()).getElementAt(popupRow);
            fileList = ((SharingTableModel) table.getModel()).getFileList();
        } else {
            popupRow = list.locationToIndex(new Point(x,y));
            fileItem = ((ImageListModel) list.getModel()).getFileItem(popupRow);
            fileList = ((ImageListModel)list.getModel()).getFileList();
        }
        popupMenu.show(component, x, y);
    }

    private class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), fileList, fileItem, localFileItemPropsFactory);
        }
    }
}
