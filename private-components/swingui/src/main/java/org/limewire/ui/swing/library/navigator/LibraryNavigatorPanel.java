package org.limewire.ui.swing.library.navigator;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.library.CreatePlayListAction;
import org.limewire.ui.swing.library.popup.LibraryNavPopupHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorPanel extends JXPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    
    private final LibraryNavigatorTable table;
    private final CreatePlayListAction createAction;
    private final SharedFileListManager sharedFileListManager;
    
    private HyperlinkButton createListButton;
    
    @Inject
    public LibraryNavigatorPanel(LibraryNavigatorTable table, LibraryNavTableRenderer renderer,
            LibraryNavPopupHandler popupHandler, LibraryManager libraryManager, CreatePlayListAction createAction,
            SharedFileListManager sharedFileListManager) {
        super(new MigLayout("insets 0, gap 0, fill", "[125!]", ""));
        
        this.table = table;
        this.sharedFileListManager = sharedFileListManager;
        this.createAction = createAction;
        
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.setPopupHandler(popupHandler);
        
        add(scrollPane, "growx, dock north");

        createCreateListButton();
        add(createListButton, "wrap, grow");
        
        initData();
    }
    
    private void initData() {
        table.addLibraryNavItem(null, I18n.tr("Library"));

        EventList<SharedFileList> playLists = sharedFileListManager.getModel();

        // TODO: this needs to be in a different thread.
        playLists.getReadWriteLock().readLock().lock();
        try {
            for(SharedFileList fileList : playLists) {
                table.addLibraryNavItem(fileList.getCollectionName(), fileList.getCollectionName());
            }
        } catch(Exception e) {
            playLists.getReadWriteLock().readLock().unlock();
        }
        
        table.getSelectionModel().setSelectionInterval(0, 0);
    }
    
    private void createCreateListButton() {
        createListButton = new HyperlinkButton("Create List", createAction);
    }
    
    public void addTableSelectionListener(ListSelectionListener listener) {
        table.getSelectionModel().addListSelectionListener(listener);
    }
    
    public int getSelectedRow() {
        return table.getSelectedRow();
    }
    
    public LibraryNavItem getSelectedNavItem() {
        return table.getSelectedItem();
    }
}
