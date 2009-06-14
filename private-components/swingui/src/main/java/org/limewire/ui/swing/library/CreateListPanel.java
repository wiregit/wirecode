package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CreateListPanel extends JXPanel {

    private JTextField nameTextField;
    private JXButton createButton;
    
    @Inject
    public CreateListPanel(final Provider<SharedFileListManager> shareManager) {
        super(new MigLayout("gap 5, insets 5")); 
        
        nameTextField = new JTextField(30);
        createButton = new JXButton(I18n.tr("Create"));
        createButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(nameTextField != null && nameTextField.getText().trim().length() > 0) {
                    BackgroundExecutorService.execute(new Runnable() {
                        @Override
                        public void run() {
                          shareManager.get().createNewSharedFileList(nameTextField.getText());
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
