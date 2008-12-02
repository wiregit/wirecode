package org.limewire.ui.swing.upload;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.upload.table.UploadTable;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UploadPanel extends JXPanel{
    
    public static final String NAME = "UploadPanel";
    private JXButton clearAllButton;
    private LimeHeaderBar header;
    private LimeHeaderBarFactory headerBarFactory;
    private EventList<UploadItem> uploadItems;
    
    private final Action clearAction = new AbstractAction(I18n.tr("Clear finished")) {
        @Override
        public void actionPerformed(ActionEvent e) {
           clearFinished();
        }
    };
    
    private ButtonDecorator buttonDecorator;
    
    @Inject
    public UploadPanel(UploadListManager listManager, LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator, CategoryIconManager categoryIconManager, LimeProgressBarFactory progressBarFactory){
        super(new BorderLayout());
        
        this.buttonDecorator = buttonDecorator;
        this.headerBarFactory = headerBarFactory;
        this.uploadItems = listManager.getSwingThreadSafeUploads();
        

        UploadTable table = new UploadTable(uploadItems, categoryIconManager, progressBarFactory);
        initHeader();
        
        add(header, BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
    }
    
    private void clearFinished() {
        uploadItems.getReadWriteLock().writeLock().lock();
        try {
            for(UploadItem item : uploadItems){
                if(item.getState() == UploadState.DONE){
                    uploadItems.remove(item);
                }
            }
        } finally {
            uploadItems.getReadWriteLock().writeLock().unlock();
        }
        
    }

    private void initHeader() {
        header = headerBarFactory.createBasic(I18n.tr("Uploads"));
        
        clearAllButton = new JXButton(clearAction);  
        buttonDecorator.decorateDarkFullButton(clearAllButton);     
 
        header.setLayout(new MigLayout("insets 0, fillx, filly","push[][]"));
        header.add(clearAllButton, "gapafter 10");
    }

}
