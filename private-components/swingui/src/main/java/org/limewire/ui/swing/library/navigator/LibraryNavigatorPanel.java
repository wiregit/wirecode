package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorPanel extends JXPanel {

    @Resource private Color backgroundColor;
    @Resource private Color borderColor;
    @Resource private Font createListFont;
    
    private final LibraryNavigatorTable table;
    private final CreateListAction createAction;
    private final SharedFileListManager sharedFileListManager;
    
    private HyperlinkButton createListButton;
    
    @Inject
    public LibraryNavigatorPanel(LibraryNavigatorTable table, LibraryNavTableRenderer renderer,
            LibraryNavTableEditor editor,
            LibraryNavPopupHandler popupHandler, 
            CreateListAction createAction,
            SharedFileListManager sharedFileListManager, GhostDragGlassPane ghostGlassPane) {
        super(new MigLayout("insets 0, gap 0, fillx", "[150!]", ""));
        
        this.table = table;
        this.sharedFileListManager = sharedFileListManager;
        this.createAction = createAction;
        
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        setBorder(BorderFactory.createMatteBorder(0,0,0,1, borderColor));
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);
        table.getColumnModel().getColumn(0).setCellEditor(editor);
        table.setPopupHandler(popupHandler);
        
        add(scrollPane, "growx, growy, wrap");

        createCreateListButton();
        add(createListButton, "aligny top, gaptop 5, alignx center, wrap");

        
//        try {
//            table.getDropTarget().addDropTargetListener(new GhostDropTargetListener(table, ghostGlassPane));
//        } catch (TooManyListenersException e) {
//        }

        initData();
    }
    
    @Inject
    void register(LifeCycleManager lifecycleManager) {
        final EventList<SharedFileList> listsModel = sharedFileListManager.getModel();
        if(lifecycleManager.isStarted() && listsModel.size() == 1) {
            createPrivateShareList();
        } else {
            lifecycleManager.addListener(new EventListener<LifeCycleEvent>() {
                @SwingEDTEvent
                public void handleEvent(LifeCycleEvent event) {
                    switch(event) {
                    case STARTED:
                        if(listsModel.size() == 1) {
                            createPrivateShareList();
                        }
                    }
                }
            });            
        }
    }
    
    /**
     * If we haven't created the Private Shared list yet, create it
     */
    private void createPrivateShareList() {
        sharedFileListManager.createNewSharedFileList(I18n.tr("Private Shared"));
    }
    
    private void initData() {
        table.getSelectionModel().setSelectionInterval(0, 0);
    }
    
    private void createCreateListButton() {
        createListButton = new HyperlinkButton(I18n.tr("Create List"), createAction);
        createListButton.setFont(createListFont);
    }
    
    public void selectLocalFileList(LocalFileList localFileList) {
        table.selectLibraryNavItem(localFileList);
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
