package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CreateListPanel extends JXPanel {

    private final JTextField nameTextField;
    private final JXButton createButton;
    
    @Inject
    public CreateListPanel(final Provider<SharedFileListManager> shareManager, final Provider<LibraryNavigatorTable> navTable) {
        super(new MigLayout("gap 5, insets 5")); 
        
        nameTextField = new JTextField(30);
        createButton = new JXButton(I18n.tr("Create"));
        createButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nameTextField != null && nameTextField.getText().trim().length() > 0) {
                    String text = nameTextField.getText().trim();
                    final int id = shareManager.get().createNewSharedFileList(text);
                    // select our newly created NavItem
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            navTable.get().selectLibraryNavItem(id);
                        }
                    });
                    hideDialog();
                }
            }
        });
        
        add(nameTextField, "span");
        add(createButton, "skip 1, alignx right");
    }
    
    private void hideDialog() {
        getTopLevelAncestor().setVisible(false);
        nameTextField.setText("");
        if(getTopLevelAncestor() instanceof JDialog) {
            ((JDialog)getTopLevelAncestor()).dispose();
        }
    }
}
