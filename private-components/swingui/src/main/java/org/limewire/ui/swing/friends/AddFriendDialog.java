package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.util.Objects;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPException;

public class AddFriendDialog extends LimeJDialog {

    public AddFriendDialog(JComponent parent, final XMPPConnection connection) {
        super(SwingUtilities.getWindowAncestor(parent), tr("Add Friend"));
        setLocationRelativeTo(parent);
        // The dialog can only be popped up when the user is signed in, so
        // connection should never be null
        Objects.nonNull(connection, "XMPP connection");
        setModalityType(ModalityType.MODELESS);
        setLayout(new MigLayout());

        final JLabel usernameLabel = new JLabel(tr("Username:"));
        final JLabel nicknameLabel = new JLabel(tr("Nickname:"));
        final JTextField usernameField = new JTextField(18);
        final JTextField nicknameField = new JTextField(18);

        final JButton ok = new JButton(tr("Add friend"));
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // If the user didn't enter a domain, use the service name
                String user = usernameField.getText().trim();
                if(user.indexOf('@') == -1)
                    user += "@" + connection.getConfiguration().getServiceName();
                final String username = user;
                // If the user didn't enter a nickname, use the username
                String nick = nicknameField.getText().trim();
                if(nick.equals(""))
                    nick = usernameField.getText();
                final String nickname = nick;
                setVisible(false);
                dispose();
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            connection.addUser(username, nickname);
                        } catch(XMPPException ignored) {}
                    }
                });
            }
        });
        ok.setEnabled(false); // Disable until a username is entered

        final JButton cancel = new JButton(tr("Cancel"));
        cancel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        // Disable the OK button when the username field is empty
        usernameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                checkWhetherEmpty();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkWhetherEmpty();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                checkWhetherEmpty();
            }
            
            private void checkWhetherEmpty() {
                ok.setEnabled(!usernameField.getText().trim().equals(""));
            }
        });

        add(usernameLabel);
        add(usernameField, "span, wrap");
        add(nicknameLabel);
        add(nicknameField, "span, wrap");
        add(ok, "cell 1 2");
        add(cancel, "cell 2 2");

        pack();
        setVisible(true);
    }
}