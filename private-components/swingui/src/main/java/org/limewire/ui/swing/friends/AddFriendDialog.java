package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.limewire.util.Objects;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPException;

public class AddFriendDialog extends JDialog {

    public AddFriendDialog(JComponent parent, final XMPPConnection connection) {
        super(SwingUtilities.getWindowAncestor(parent), tr("Add Friend"));
        // The dialog can only be popped up when the user is signed in, so
        // connection should never be null
        Objects.nonNull(connection, "XMPP connection");
        setModalityType(ModalityType.MODELESS);
        setLayout(new MigLayout());

        final JLabel usernameLabel = new JLabel(tr("Friend's username:"));
        final JLabel nicknameLabel = new JLabel(tr("Nickname to display:"));
        final JTextField usernameField = new JTextField(18);
        final JTextField nicknameField = new JTextField(18);

        JButton ok = new JButton(tr("Add friend"));
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Validate that the username field has text
                String user = usernameField.getText().trim();
                if (user.equals("")) {
                    usernameLabel.setForeground(Color.RED);
                    return;
                }
                // If the user didn't enter a domain, use the service name
                if(user.indexOf('@') == -1)
                    user += "@" + connection.getConfiguration().getServiceName();
                // If the user didn't enter a nickname, use the username
                String nick = nicknameField.getText().trim();
                if(nick.equals(""))
                    nick = usernameField.getText();
                setVisible(false);
                dispose();
                try {
                    connection.addUser(user, nick);
                } catch(XMPPException ignored) {}
            }
        });

        JButton cancel = new JButton(tr("Cancel"));
        cancel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        add(usernameLabel);
        add(usernameField, "span, wrap");
        add(nicknameLabel);
        add(nicknameField, "span, wrap");
        add(ok, "cell 1 2");
        add(cancel, "cell 2 2, wrap");

        pack();
        setVisible(true);
    }
}