package org.limewire.ui.swing.menu;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FileMenu extends JMenu {
    private final Navigator navigator;

    @Inject
    public FileMenu(DownloadListManager downloadListManager, Navigator navigator) {
        super(I18n.tr("File"));
        this.navigator = navigator;
        JMenuItem fileItem = getFileMenuItem(downloadListManager);
        add(fileItem);

        JMenuItem urlItem = getUrlMenuItem(downloadListManager);
        add(urlItem);
    }

    private JMenuItem getFileMenuItem(final DownloadListManager downloadListManager) {
        // TODO this was just added for testing purposes, need to talk to mike
        // to see the behavior we will use for real
        JMenuItem fileItem = new JMenuItem(I18n.tr("Open File"));
        fileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFrame jFrame = new JFrame("open test");
                jFrame.setBounds(new Rectangle(600, 300));
                JPanel filePanel = new JPanel();
                JLabel fileLabel = new JLabel(I18n.tr("File:"));
                final JTextField fileField = new JTextField(50);
                fileField.setText("/home/pvertenten/Desktop/Parted Magic 3.1 rc1 [LXDE].torrent");
                JButton openButton = new JButton(I18n.tr("Open"));
                openButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        String fileString = fileField.getText();
                        try {
                            File file = new File(fileString);
                            downloadListManager.addDownload(file);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select();
                            jFrame.dispose();
                        } catch (SaveLocationException e) {
                            // TODO implement good user feedback
                            throw new UnsupportedOperationException(
                                    "Need to implement good user feedback for this.");
                        }
                    }
                });
                filePanel.add(fileLabel);
                filePanel.add(fileField);
                filePanel.add(openButton);

                jFrame.setContentPane(filePanel);

                jFrame.setVisible(true);

            }
        });
        return fileItem;
    }

    private JMenuItem getUrlMenuItem(final DownloadListManager downloadListManager) {
        // TODO this was just added for testing purposes, need to talk to mike
        // to see the behavior we will use for real
        JMenuItem urlItem = new JMenuItem(I18n.tr("Open URL"));
        urlItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFrame jFrame = new JFrame("open test");
                jFrame.setBounds(new Rectangle(600, 300));
                JPanel urlPanel = new JPanel();
                JLabel urlLabel = new JLabel(I18n.tr("URL:"));
                final JTextField urlField = new JTextField(50);
                urlField
                        .setText("http://linuxtracker.org/download.php?id=b732059d98a6ba8cec113fc129a2e344495f9ca2&f=Parted%20Magic%203.1%20rc1%20%5BLXDE%5D.torrent");

                JButton openButton = new JButton(I18n.tr("Open"));
                openButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        String uriString = urlField.getText();
                        try {
                            URI uri = new URI(uriString);
                            downloadListManager.addDownload(uri);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select();
                            jFrame.dispose();
                        } catch (URISyntaxException e1) {
                            // TODO implement good user feedback
                            throw new UnsupportedOperationException(
                                    "Need to implement good user feedback for this.");
                        } catch (SaveLocationException e) {
                            // TODO implement good user feedback
                            throw new UnsupportedOperationException(
                                    "Need to implement good user feedback for this.");
                        }
                    }
                });
                urlPanel.add(urlLabel);
                urlPanel.add(urlField);
                urlPanel.add(openButton);

                jFrame.setContentPane(urlPanel);

                jFrame.setVisible(true);

            }
        });
        return urlItem;
    }

}
