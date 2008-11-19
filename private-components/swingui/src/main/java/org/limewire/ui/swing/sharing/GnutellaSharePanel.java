package org.limewire.ui.swing.sharing;

import java.awt.BorderLayout;
import java.awt.CardLayout;

import javax.swing.JPanel;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanel;
import org.limewire.ui.swing.sharing.fancy.SharingFancyPanelFactory;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The LimeWire Network sharing panel. This panel displays all the files
 * being shared with Gnutella.
 */
@Singleton
public class GnutellaSharePanel extends GenericSharingPanel {

    public static final String NAME = I18n.tr("LimeWire Network");
       
    private final SharingFancyPanelFactory sharingFancyPanelFactory;
    
    private final JPanel sharingHeaderPanel;
       
    private final LocalFileList fileList;
    
    private CardLayout overviewCardLayout;
    
    private JPanel nonEmptyPanel;
    private final SharingEmptyPanel emptyPanel;
    
    private final PromptTextField filterBox;
    
    @Inject
    public GnutellaSharePanel(ShareListManager libraryManager, SharingEmptyPanel sharingEmptyPanel,
            SharingFancyPanelFactory sharingFancyPanelFactory, LimeHeaderBarFactory headerBarFactory) {       
        this.sharingFancyPanelFactory = sharingFancyPanelFactory;
        this.sharingHeaderPanel = headerBarFactory.createBasic(I18n.tr("Sharing with the LimeWire Network"));
        this.emptyPanel = sharingEmptyPanel;
        
        this.filterBox = new PromptTextField(I18n.tr("Filter..."));
        this.sharingHeaderPanel.add(this.filterBox);
        
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

        createNonEmptyPanel();
        addTransferHandlers();
        
        //add cards to the layout
        add(sharingEmptyPanel, EMPTY);
        add(nonEmptyPanel, NONEMPTY);
        overviewCardLayout.show(this,EMPTY);
    }
    
    /**
     * Adds transfer handlers to the panels to support dropping files.
     */
    private void addTransferHandlers() {
        SharingTransferHandler transferHandler = new SharingTransferHandler(fileList,false);
        emptyPanel.setTransferHandler(transferHandler);
        nonEmptyPanel.setTransferHandler(transferHandler);
    }
    
    /**
     * Create the main view panel
     */
    private void createNonEmptyPanel() {
        nonEmptyPanel = new JPanel();
        nonEmptyPanel.setLayout(new BorderLayout());

        FilterList<LocalFileItem> filteredList = GlazedListsFactory.filterList(fileList.getSwingModel(), 
                new TextComponentMatcherEditor<LocalFileItem>(this.filterBox, new SharingTextFilterer()));
        
        SharingFancyPanel sharingFancyPanel = sharingFancyPanelFactory.create(filteredList, scrollPane, fileList);
        scrollPane.setViewportView(sharingFancyPanel);

        nonEmptyPanel.add(sharingHeaderPanel, BorderLayout.NORTH);
        nonEmptyPanel.add(scrollPane, BorderLayout.CENTER);
    }
}
