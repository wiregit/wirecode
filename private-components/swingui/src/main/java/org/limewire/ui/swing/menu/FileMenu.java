package org.limewire.ui.swing.menu;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Application;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.mainframe.MainPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandlerImpl;
import org.limewire.util.FileUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileMenu extends JMenu {
    private final Navigator navigator;

    @Inject
    public FileMenu(DownloadListManager downloadListManager, Navigator navigator,
            LibraryManager libraryManager, final MainPanel mainPanel,
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        super(I18n.tr("File"));
        this.navigator = navigator;
        add(buildOpenFileAction(downloadListManager, mainPanel, saveLocationExceptionHandler));
        add(buildOpenLinkAction(downloadListManager, mainPanel));
        add(getRecentDownloadsMenu(downloadListManager));
        addSeparator();
        add(getAddFileAction(libraryManager, mainPanel));
        add(getAddFolderAction(libraryManager, mainPanel));
        addSeparator();
        add(new SignInOutAction());
        addSeparator();
        add(new AbstractAction(I18n.tr("E&xit")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Application.getInstance().exit(e);
            }
        });
    }

    private Action getAddFolderAction(final LibraryManager libraryManager, final MainPanel mainPanel) {
        return new AbstractAction(I18n.tr("Add Folder To Library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> folders = FileChooser.getInput(mainPanel, I18n.tr("Add Folder(s)"), I18n
                        .tr("Add Folder(s)"), FileChooser.getLastInputDirectory(),
                        JFileChooser.DIRECTORIES_ONLY, JFileChooser.APPROVE_OPTION, true,
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                return f.isDirectory();
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr("All Folders");
                            }
                        });

                if (folders != null) {
                    for (File folder : folders) {
                        libraryManager.getLibraryManagedList().addFolder(folder);
                    }
                }

            }
        };
    }

    private Action getAddFileAction(final LibraryManager libraryManager, final MainPanel mainPanel) {
        return new AbstractAction(I18n.tr("Add File To Library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> files = FileChooser.getInput(mainPanel, I18n.tr("Add File(s)"), I18n
                        .tr("Add Files"), FileChooser.getLastInputDirectory(),
                        JFileChooser.FILES_ONLY, JFileChooser.APPROVE_OPTION, true,
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                return f.isDirectory()
                                        || libraryManager.getLibraryData().isFileManageable(f);
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr("Valid Files");
                            }
                        });

                if (files != null) {
                    for (File file : files) {
                        libraryManager.getLibraryManagedList().addFile(file);
                    }
                }
            }
        };
    }

    private JMenu getRecentDownloadsMenu(DownloadListManager downloadListManager) {
        final JMenu recentDownloads = new JMenu(I18n.tr("Recent Downloads"));
        new AbstractListEventListener<DownloadItem>() {
            @Override
            protected void itemAdded(final DownloadItem item) {
                recentDownloads.add(new AbstractAction(item.getFileName() + " - "
                        + item.getPercentComplete() + "%") {
                    {
                        putValue("DOWNLOAD", item);
                        item.addPropertyChangeListener(new PropertyChangeListener() {
                            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                                putValue(Action.NAME, item.getFileName() + " - "
                                        + item.getPercentComplete() + "%");
                            }
                        });
                    }

                    public void actionPerformed(ActionEvent e) {
                        navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(
                                SimpleNavSelectable.create(item));
                    }
                });
            }

            @Override
            protected void itemRemoved(DownloadItem item) {
                for (Component component : recentDownloads.getMenuComponents()) {
                    if (component instanceof JMenuItem) {
                        if (item.equals(((JMenuItem) component).getAction().getValue("DOWNLOAD"))) {
                            recentDownloads.remove(component);
                            break;
                        }
                    }
                }
            }

            @Override
            protected void itemUpdated(DownloadItem item) {
            }
        }.install(downloadListManager.getSwingThreadSafeDownloads());
        return recentDownloads;
    }

    private Action buildOpenFileAction(final DownloadListManager downloadListManager,
            final MainPanel mainPanel,
            final SaveLocationExceptionHandler saveLocationExceptionHandler) {
        return new AbstractAction(I18n.tr("&Open File")) {
            @Override
            public void actionPerformed(ActionEvent e) {

                List<File> files = FileChooser.getInput(mainPanel, I18n.tr("Open File"), I18n
                        .tr("Open"), FileChooser.getLastInputDirectory(), JFileChooser.FILES_ONLY,
                        JFileChooser.APPROVE_OPTION, true, new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                String extension = FileUtils.getFileExtension(f);
                                return f.isDirectory() || "torrent".equalsIgnoreCase(extension);
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr(".torrent files");
                            }
                        });

                if (files != null) {
                    for (final File file : files) {
                        try {
                            // TODO handle other magnet options, not jsut
                            // download
                            DownloadItem item = downloadListManager.addDownload(file);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select(SimpleNavSelectable.create(item));
                        } catch (SaveLocationException sle) {
                            saveLocationExceptionHandler.handleSaveLocationException(
                                    new SaveLocationExceptionHandlerImpl.DownLoadAction() {
                                        @Override
                                        public void download(File saveFile, boolean overwrite)
                                                throws SaveLocationException {
                                            DownloadItem item = downloadListManager.addDownload(
                                                    file, saveFile, overwrite);
                                            navigator.getNavItem(NavCategory.DOWNLOAD,
                                                    MainDownloadPanel.NAME).select(
                                                    SimpleNavSelectable.create(item));
                                        }
                                    }, sle, false, mainPanel);
                        }
                    }
                }
            }
        };
    }

    private Action buildOpenLinkAction(final DownloadListManager downloadListManager,
            final MainPanel mainPanel) {
        return new AbstractAction(I18n.tr("Open &Link")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final LocationDialogue locationDialogue = new LocationDialogue();
                locationDialogue.setLocationRelativeTo(mainPanel);
                locationDialogue.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        URI uri = locationDialogue.getURI();
                        if (uri != null) {
                            try {
                                // TODO handle other magnet options, not jsut
                                // download
                                DownloadItem item = downloadListManager.addDownload(uri);
                                navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                        .select(SimpleNavSelectable.create(item));
                            } catch (SaveLocationException e1) {
                                // TODO implement good user feedback
                                throw new UnsupportedOperationException(
                                        "Need to implement good user feedback for this.");
                            }
                        }
                    }
                });

                locationDialogue.setVisible(true);

            }
        };
    }

    private class LocationDialogue extends JDialog {
        private JButton openButton = null;

        private JTextField urlField = null;

        public LocationDialogue() {
            super();
            setModalityType(ModalityType.APPLICATION_MODAL);
            JPanel urlPanel = new JPanel();
            JLabel urlLabel = new JLabel(I18n.tr("Link:"));
            urlField = new JTextField(30);
            urlField.setText("http://");

            final JLabel errorLabel = new JLabel(I18n.tr("Invalid Link!"));
            errorLabel.setForeground(Color.RED);

            urlField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    URI uri = getURI();
                    // TODO add stricter validation
                    // TODO allow magnet files

                    if (uri == null || uri.getPath() == null || uri.getPath().trim().length() == 0) {
                        errorLabel.setVisible(true);
                        openButton.setEnabled(false);
                    } else {
                        errorLabel.setVisible(false);
                        openButton.setEnabled(true);
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (openButton.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                        openButton.doClick();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {

                }
            });

            openButton = new JButton(I18n.tr("Open"));
            openButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    LocationDialogue.this.dispose();
                }
            });
            openButton.setEnabled(false);

            JButton cancelButton = new JButton(I18n.tr("Cancel"));
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LocationDialogue.this.dispose();
                }
            });
            urlPanel.setLayout(new MigLayout("", "[]5[]5[]", "[]5[]"));
            urlPanel.add(new JLabel(I18n.tr("Open magnet or torrent link.")), "span 3, wrap");
            urlPanel.add(urlLabel, "alignx right");
            urlPanel.add(urlField, "span 2");
            urlPanel.add(errorLabel, "wrap");
            urlPanel.add(openButton, "skip 2, alignx right");
            urlPanel.add(cancelButton, "alignx right");

            setContentPane(urlPanel);
            pack();
        }

        void addActionListener(ActionListener actionListener) {
            openButton.addActionListener(actionListener);
        }

        void removeActionListener(ActionListener actionListener) {
            openButton.removeActionListener(actionListener);
        }

        /**
         * Returns a uri typed into this dialogue. Will return null if the last
         * uri typed into the dialogue was invalid.
         */
        public synchronized URI getURI() {
            try {
                return URIUtils.toURI(urlField.getText());
            } catch (URISyntaxException e) {
                // eating exception and returning null for bad uris
                return null;
            }
        }
    }

    public static class SignInOutAction extends AbstractAction {
        private static final String SIGN_INTO_FRIENDS_TEXT = I18n.tr("&Sign into Friends");

        private static final String SIGN_OUT_OF_FRIENDS_TEXT = I18n.tr("&Sign out of Friends");

        boolean signedIn = false;

        public SignInOutAction() {
            super(SIGN_INTO_FRIENDS_TEXT);
            EventAnnotationProcessor.subscribe(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!signedIn) {
                new DisplayFriendsToggleEvent(true).publish();
            } else {
                new SignoffEvent().publish();
            }
        }

        @EventSubscriber
        public void handleSignon(XMPPConnectionEstablishedEvent event) {
            putValue(Action.NAME, SIGN_OUT_OF_FRIENDS_TEXT);
            signedIn = true;
        }

        @EventSubscriber
        public void handleSignoff(SignoffEvent event) {
            putValue(Action.NAME, SIGN_INTO_FRIENDS_TEXT);
            signedIn = false;
        }
    }
}
