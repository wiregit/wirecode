package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanelFactory;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GnutellaSharePanel extends GenericSharingPanel {

    public static final String NAME = I18n.tr("LimeWire Network");
    
    @Resource
    protected Icon cancelIcon;
    
    private final SharingFancyPanelFactory sharingFancyPanelFactory;
       
    private final LocalFileList fileList;
    
    private CardLayout overviewCardLayout;
    
    private JPanel nonEmptyPanel;

    MultiButtonTableCellRendererEditor editor;
    MultiButtonTableCellRendererEditor renderer;
    
    @Inject
    public GnutellaSharePanel(ShareListManager libraryManager, SharingEmptyPanel emptyPanel, SharingFancyPanelFactory sharingFancyPanelFactory) {
        GuiUtils.assignResources(this); 
        
        this.sharingFancyPanelFactory = sharingFancyPanelFactory;
        
        this.fileList = libraryManager.getGnutellaShareList();
        this.fileList.getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>(){
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
        emptyPanel.setTransferHandler(new SharingTransferHandler(fileList,false));
    }

    
    private void createTablesPanels() {
        nonEmptyPanel = new JPanel();
        nonEmptyPanel.setLayout(new BorderLayout());
        nonEmptyPanel.setTransferHandler(new SharingTransferHandler(fileList,false));
        
        SharingHeaderPanel headerPanel = new SharingHeaderPanel(I18n.tr("Sharing with the {0}"), NAME);//createHeader(cardPanel);

        FilterList<LocalFileItem> filteredList = GlazedListsFactory.filterList(fileList.getSwingModel(), 
                new TextComponentMatcherEditor<LocalFileItem>(headerPanel.getFilterBox(), new SharingTextFilterer()));
        
//        SharingFancyPanel sharingFancyPanel = new SharingFancyPanel(filteredList, scrollPane, fileList, iconManager, thumbnailManager);
        
        SharingFancyPanel sharingFancyPanel = sharingFancyPanelFactory.create(filteredList, scrollPane, fileList);
        scrollPane.setViewportView(sharingFancyPanel);

        nonEmptyPanel.add(headerPanel, BorderLayout.NORTH);
        nonEmptyPanel.add(scrollPane, BorderLayout.CENTER);
    }
}
