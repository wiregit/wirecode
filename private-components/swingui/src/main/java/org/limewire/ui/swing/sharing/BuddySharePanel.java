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
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.actions.SharingRemoveTableAction;
import org.limewire.ui.swing.sharing.table.SharingTable;
import org.limewire.ui.swing.sharing.table.SharingTableFormat;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

@Singleton
public class BuddySharePanel extends GenericSharingPanel {
    public static final String NAME = "All Friends";
    
    @Resource
    protected Icon cancelIcon;
    @Resource
    protected Icon sharingIcon;
    
    private ViewSelectionPanel viewSelectionPanel;
       
    private final FileList fileList;
    
    private CardLayout overviewCardLayout;
    private CardLayout viewCardLayout;
    
    private JPanel nonEmptyPanel;

    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;

    @Inject
    public BuddySharePanel(LibraryManager libraryManager, SharingBuddyEmptyPanel emptyPanel) {        
        GuiUtils.assignResources(this); 
        
        this.fileList = libraryManager.getAllBuddyList();
        this.fileList.getModel().addListEventListener(new ListEventListener<FileItem>(){
            @Override
            public void listChanged(ListEvent<FileItem> listChanges) {
                if(listChanges.getSourceList().size() == 0) {
                    overviewCardLayout.show(BuddySharePanel.this,EMPTY);
                } else {
                    overviewCardLayout.show(BuddySharePanel.this,NONEMPTY);
                }
            }
        });
        
        overviewCardLayout = new CardLayout();
        this.setLayout(overviewCardLayout);

        createTablesPanels();
    
        add(emptyPanel, EMPTY);
        add(nonEmptyPanel, NONEMPTY);
        overviewCardLayout.show(this,EMPTY);
    }

    
    private void createTablesPanels() {
        viewCardLayout = new CardLayout();
        nonEmptyPanel = new JPanel();
        nonEmptyPanel.setLayout(new BorderLayout());
        
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(viewCardLayout);
        
        SharingHeaderPanel headerPanel = createHeader(cardPanel);

        createCenterCards(headerPanel, cardPanel);


        nonEmptyPanel.add(headerPanel, BorderLayout.NORTH);
        nonEmptyPanel.add(cardPanel);
    }
    
    private SharingHeaderPanel createHeader(JPanel cardPanel) {
        viewSelectionPanel = new ViewSelectionPanel(new ItemAction(cardPanel, viewCardLayout, LIST), 
                new ItemAction(cardPanel, viewCardLayout, TABLE));
        
        SharingHeaderPanel headerPanel = new SharingHeaderPanel(sharingIcon, "Sharing with the LimeWire Network", viewSelectionPanel);
        return headerPanel;
    }
    
    private void createCenterCards(SharingHeaderPanel headerPanel, JPanel cardPanel) {

        FilterList<FileItem> filteredList = new FilterList<FileItem>(fileList.getModel(), 
                new TextComponentMatcherEditor<FileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
        

        createTable(filteredList);
        
        JScrollPane scrollPane = new JScrollPane();
        SharingFancyPanel sharingFancyPanel = new SharingFancyPanel(filteredList, scrollPane, fileList);
        scrollPane.setViewportView(sharingFancyPanel);
        
        cardPanel.add(new JScrollPane(table),TABLE);
        cardPanel.add(scrollPane, LIST);
        viewCardLayout.show(cardPanel, LIST);
        
    }
    
    private void createTable(FilterList<FileItem> filteredList) {
        table = new SharingTable(filteredList, new SharingTableFormat());
        table.setTransferHandler(new SharingTransferHandler(fileList));
        table.setDropMode(DropMode.ON);
        
        editor = new MultiButtonTableCellRendererEditor(20);
        editor.addActions(createActions());
        renderer = new MultiButtonTableCellRendererEditor(20);
        renderer.addActions(createActions());
        
        //TODO: this needs to be fixed, if rows are columns or rows
        //  are removed this stops working
        TableColumn tc = table.getColumn(6);
        tc.setCellEditor(editor);
        tc.setCellRenderer(renderer);
    }
    
    private List<Action> createActions() {
        List<Action> list = new ArrayList<Action>();
        list.add(new SharingRemoveTableAction(fileList, table, cancelIcon));
        return list;
    }
}
