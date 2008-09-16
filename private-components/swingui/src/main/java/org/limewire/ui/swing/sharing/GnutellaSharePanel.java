package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.dragdrop.ShareDropTarget;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableFormat;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GnutellaSharePanel extends GenericSharingPanel {

    public static final String NAME = "LimeWire Network";
    
    @Resource
    protected Icon cancelIcon;
    @Resource
    protected Icon sharingIcon;
    
    private IconManager iconManager;
    
    private ViewSelectionPanel viewSelectionPanel;
       
    private final LocalFileList fileList;
    
    private CardLayout overviewCardLayout;
    private CardLayout tableCardLayout;
    
    private JPanel nonEmptyPanel;

    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;
    
    @Inject
    public GnutellaSharePanel(LibraryManager libraryManager, IconManager iconManager, SharingEmptyPanel emptyPanel) {
        GuiUtils.assignResources(this); 
        
        this.iconManager = iconManager;
        
        this.fileList = libraryManager.getGnutellaList();
        this.fileList.getModel().addListEventListener(new ListEventListener<LocalFileItem>(){
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                final int size = listChanges.getSourceList().size();
                SwingUtils.invokeLater(new Runnable(){
                    public void run() {
                        if( size == 0) {
                            overviewCardLayout.show(GnutellaSharePanel.this,EMPTY);
                        } else {
                            overviewCardLayout.show(GnutellaSharePanel.this,NONEMPTY);
                        }     
                    }
                });
            }
        });
                
        overviewCardLayout = new CardLayout();
        this.setLayout(overviewCardLayout);

        setupEmptyPanel(emptyPanel);
        createTablesPanels();
    
        add(emptyPanel, EMPTY);
        add(nonEmptyPanel, NONEMPTY);
        overviewCardLayout.show(this,EMPTY);
    }
    
    private void setupEmptyPanel(JPanel emptyPanel) {
        ShareDropTarget drop = new ShareDropTarget(this, fileList, false);
        emptyPanel.setDropTarget(drop.getDropTarget());
    }

    
    private void createTablesPanels() {
        tableCardLayout = new CardLayout();
        nonEmptyPanel = new JPanel();
        nonEmptyPanel.setLayout(new BorderLayout());
        
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(tableCardLayout);
        
        SharingHeaderPanel headerPanel = createHeader(cardPanel);

        createCenterCards(headerPanel, cardPanel);

        nonEmptyPanel.add(headerPanel, BorderLayout.NORTH);
        nonEmptyPanel.add(cardPanel);
    }
    
    private SharingHeaderPanel createHeader(JPanel cardPanel) {
        viewSelectionPanel = new ViewSelectionPanel(new ItemAction(cardPanel, tableCardLayout, ViewSelectionPanel.LIST_SELECTED), 
                new ItemAction(cardPanel, tableCardLayout, ViewSelectionPanel.TABLE_SELECTED));
        
        SharingHeaderPanel headerPanel = new SharingHeaderPanel(sharingIcon, "Sharing with the ", "LimeWire Network", viewSelectionPanel);
        return headerPanel;
    }
    
    private void createCenterCards(SharingHeaderPanel headerPanel, JPanel cardPanel) {

        FilterList<LocalFileItem> filteredList = new FilterList<LocalFileItem>(fileList.getModel(), 
                new TextComponentMatcherEditor<LocalFileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
        
        createTable(filteredList);
        
        SharingFancyPanel sharingFancyPanel = new SharingFancyPanel(filteredList, scrollPane, fileList, iconManager);
        scrollPane.setViewportView(sharingFancyPanel);
        
        cardPanel.add(new JScrollPane(table),ViewSelectionPanel.TABLE_SELECTED);
        cardPanel.add(scrollPane, ViewSelectionPanel.LIST_SELECTED);
        tableCardLayout.show(cardPanel, ViewSelectionPanel.LIST_SELECTED);
    }
    
    private void createTable(FilterList<LocalFileItem> filteredList) {
        table = new SharingTable(filteredList, fileList, new SharingTableFormat());
        table.setTransferHandler(new SharingTransferHandler(fileList));
        table.setDropMode(DropMode.ON);
        
        editor = new MultiButtonTableCellRendererEditor();
        editor.addActions(createActions());
        renderer = new MultiButtonTableCellRendererEditor();
        renderer.addActions(createActions());
        table.setRowHeight(20);
        
        //TODO: this needs to be fixed, if rows are columns or rows
        //  are removed this stops working
        TableColumn tc = table.getColumn(6);
        tc.setCellEditor(editor);
        tc.setCellRenderer(renderer);
        
        tc = table.getColumn(0);
        tc.setCellRenderer(new IconLabelRenderer(iconManager));
    }
    
    private List<Action> createActions() {
        List<Action> list = new ArrayList<Action>();
        list.add(new SharingRemoveTableAction(table, cancelIcon ));
        return list;
    }
}
