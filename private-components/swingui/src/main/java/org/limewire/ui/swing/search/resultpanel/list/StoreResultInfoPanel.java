package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.StoreController;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Main container for the File Info dialog for store results.
 */
class StoreResultInfoPanel extends JPanel {

    private final StoreController storeController;
    
    /**
     * Constructs a StoreResultInfoPanel using the specified services.
     */
    public StoreResultInfoPanel(StoreController storeController) {
        this.storeController = storeController;
        
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        
        // TODO implement
        
        this.setPreferredSize(new Dimension(360, 480));
    }
    
    /**
     * Displays the File Info dialog for the specified store result.
     */
    public void display(VisualStoreResult vsr) {
        
        // TODO implement to retrieve info for vsr.
        
        // Get main frame.
        Frame owner = GuiUtils.getMainFrame();
        
        // Create dialog.
        JDialog dialog = new LimeJDialog(owner);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setTitle(I18n.tr("{0} Properties", vsr.getFileName()));

        // Set content.
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        
        // Position dialog and display.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
