package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    private JTextField nameTextField;
    private JXButton createButton;
    
    @Inject
    public CreateListPanel(final Provider<LibraryNavigatorTable> table, final Provider<SharedFileListManager> shareManager) {
        super(new MigLayout("gap 5, insets 5")); 
        
        nameTextField = new JTextField(30);
        createButton = new JXButton(I18n.tr("Create"));
        createButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(nameTextField != null && nameTextField.getText().trim().length() > 0) {

                    //TODO: move to executor
                    Thread t = new Thread(new Runnable(){
                        public void run() {
                          shareManager.get().createNewSharedFileList(nameTextField.getText());
                          //TODO: this should be called here but createNewSharedFileList never returns
//                          SwingUtilities.invokeLater(new Runnable(){
//                              public void run() {
//                                  table.get().addLibraryNavItem(nameTextField.getText(), nameTextField.getText());
//                                  table.get().repaint();
//                              }
//                          });
                        }
                    });
                    t.start();
                    table.get().addLibraryNavItem(nameTextField.getText(), nameTextField.getText());
                    table.get().repaint();
                }
            }
        });
        
        add(nameTextField, "span");
        add(createButton, "skip 1, alignx right");
    }
}
