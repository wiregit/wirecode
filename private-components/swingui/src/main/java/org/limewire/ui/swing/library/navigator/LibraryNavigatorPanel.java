package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Font;

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
import org.limewire.ui.swing.library.actions.CreateListAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.popup.LibraryNavPopupHandler;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorPanel extends JXPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    @Resource private Font createListFont;
    
    private final LibraryNavigatorTable table;
    private final CreateListAction createAction;
    private final SharedFileListManager sharedFileListManager;
    private final LibraryManager libraryManager;
    
    private HyperlinkButton createListButton;
    
    @Inject
    public LibraryNavigatorPanel(LibraryNavigatorTable table, LibraryNavTableRenderer renderer,
            LibraryNavPopupHandler popupHandler, LibraryManager libraryManager, CreateListAction createAction,
            SharedFileListManager sharedFileListManager) {
        super(new MigLayout("insets 0, gap 0, fillx", "[150!]", ""));
        
        this.table = table;
        this.sharedFileListManager = sharedFileListManager;
        this.libraryManager = libraryManager;
        this.createAction = createAction;
        
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.setPopupHandler(popupHandler);
        
        add(scrollPane, "growx, growy, wrap");

        createCreateListButton();
        add(createListButton, "aligny top, gaptop 5, alignx center, wrap");
        
        initData();
    }
    
    @Inject
    void register() {
        sharedFileListManager.getModel().addListEventListener(new ListEventListener<SharedFileList>(){
            @Override
            public void listChanged(ListEvent<SharedFileList> listChanges) {
                while(listChanges.next()) {
                    SharedFileList list = listChanges.getSourceList().get(listChanges.getIndex());
                    if(listChanges.getType() == ListEvent.INSERT) {
                        table.addLibraryNavItem(list.getCollectionName(), list.getCollectionName(), list, NavType.LIST);
                    } else if(listChanges.getType() == ListEvent.DELETE){
                        table.removeLibraryNavItem(list.getCollectionName());
                    }
                }
            }
        });
    }
    
    private void initData() {
        table.addLibraryNavItem(null, I18n.tr("Library"), libraryManager.getLibraryManagedList(), NavType.LIBRARY);
        table.getSelectionModel().setSelectionInterval(0, 0);

        final EventList<SharedFileList> playLists = sharedFileListManager.getModel();

        BackgroundExecutorService.execute(new Runnable(){
            public void run() {
                // TODO: this needs to be in a different thread.
                playLists.getReadWriteLock().readLock().lock();
                try {
                    for(final SharedFileList fileList : playLists) {
                        SwingUtils.invokeLater(new Runnable(){
                            public void run() {
                                if(!fileList.isNameChangeAllowed())
                                    table.addLibraryNavItem(fileList.getCollectionName(), fileList.getCollectionName(), fileList,  NavType.PUBLIC_SHARED);
                                else
                                    table.addLibraryNavItem(fileList.getCollectionName(), fileList.getCollectionName(), fileList, NavType.LIST);
                            }
                        });
                    }
                } finally {
                    playLists.getReadWriteLock().readLock().unlock();
                }
            }
        });
    }
    
    private void createCreateListButton() {
        createListButton = new HyperlinkButton("Create List", createAction);
        createListButton.setFont(createListFont);
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
