package org.limewire.ui.swing.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Application;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.friends.SelfAvailabilityUpdateEvent;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;
import org.limewire.util.URIUtils;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FileMenu extends JMenu {
    private final Navigator navigator;

    @Inject
    public FileMenu(DownloadListManager downloadListManager, Navigator navigator,
            LibraryManager libraryManager) {
        super(I18n.tr("File"));
        this.navigator = navigator;
        add(getFileMenuItem(downloadListManager));
        add(getUrlMenuItem(downloadListManager));
        JMenu recentDownloads = getRecentDownloads();
        add(recentDownloads);
        addSeparator();
        add(getAddFile(libraryManager));
        add(getAddFolder(libraryManager));
        addSeparator();
        add(getLaunchItem());
        add(getLocateFile());
        addSeparator();
        add(new AbstractAction(I18n.tr("Set as available")) {
            // TODO disable dpedning on if logged in or not, make set as
            // avaialble and set as away the same option
            @Override
            public void actionPerformed(ActionEvent e) {
                new SelfAvailabilityUpdateEvent(Mode.available).publish();
            }
        });
        add(new AbstractAction(I18n.tr("Set as away")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SelfAvailabilityUpdateEvent(Mode.away).publish();
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Switch user")) {
            // TODO disable depending on whether signed in or not.
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me");
            }
        });
        add(new AbstractAction(I18n.tr("Sign into Friends/Sign out of Friends")) {
            // TODO show approriate text depending on whether signed in or not.
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Exit")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO this acts funny, might be causing a race condition
                Application.getInstance().exit(e);
            }
        });

    }

    private Action getLocateFile() {

        return new AbstractAction(I18n.tr("Locate file")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO need to be able to get the currently select file from
                // any view
                // File file = null;
                // NativeLaunchUtils.launchExplorer(file);
                throw new UnsupportedOperationException("TODO implement me");
            }
        };
    }

    private Action getLaunchItem() {
        return new AbstractAction(I18n.tr("Launch file")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO need to be able to get the currently select file from
                // any view
                // File file = null;
                // NativeLaunchUtils.launchFile(file);
                throw new UnsupportedOperationException("TODO Implement Me.");
            }
        };
    }

    private Action getAddFolder(final LibraryManager libraryManager) {
        return new AbstractAction(I18n.tr("Add Folder To Library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> folders = FileChooser.getInput(FileMenu.this, I18n.tr("Add Folder(s)"),
                        I18n.tr("Add Folder(s)"), FileChooser.getLastInputDirectory(),
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

    private Action getAddFile(final LibraryManager libraryManager) {
        return new AbstractAction(I18n.tr("Add File To Library")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> files = FileChooser.getInput(FileMenu.this, I18n.tr("Add File(s)"), I18n
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

    private JMenu getRecentDownloads() {
        // TODO need to save recent downloads to a list or a bounded queue
        return new JMenu(I18n.tr("Recent Downloads"));
    }

    private Action getFileMenuItem(final DownloadListManager downloadListManager) {
        return new AbstractAction(I18n.tr("Open File")) {
            @Override
            public void actionPerformed(ActionEvent e) {

                List<File> files = FileChooser.getInput(FileMenu.this, I18n.tr("Open File"), I18n
                        .tr("Open"), FileChooser.getLastInputDirectory(), JFileChooser.FILES_ONLY,
                        JFileChooser.APPROVE_OPTION, true, new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                String extension = FileUtils.getFileExtension(f);
                                return f.isDirectory() || "torrent".equalsIgnoreCase(extension)
                                        || "magnet".equalsIgnoreCase(extension);
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr(".torrent or .magnet files");
                            }
                        });

                if (files != null) {
                    for (File file : files) {
                        try {
                            downloadListManager.addDownload(file);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select();
                        } catch (SaveLocationException e1) {
                            // TODO better user feedback
                            throw new UnsupportedOperationException(e1);
                        }
                    }
                }
            }
        };
    }

    private Action getUrlMenuItem(final DownloadListManager downloadListManager) {
        return new AbstractAction(I18n.tr("Open Link")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final LocationDialogue locationDialogue = new LocationDialogue();
                locationDialogue.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        URI uri = locationDialogue.getURI();
                        if (uri != null) {
                            try {
                                downloadListManager.addDownload(uri);
                                navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                        .select();
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
            JLabel urlLabel = new JLabel(I18n.tr("URL:"));
            urlField = new JTextField(50);
            urlField.setText("http://");

            final JLabel errorLabel = new JLabel(I18n.tr("Invalid URL"));
            errorLabel.setForeground(Color.RED);

            urlField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    URI uri = getURI();
                    // TODO add stricter validation

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
            urlPanel.setLayout(new MigLayout("", "[]5[]", "[]5[]"));
            urlPanel.setPreferredSize(new Dimension(500, 60));
            urlPanel.add(urlLabel);
            urlPanel.add(urlField, "wrap");
            urlPanel.add(openButton, "align right");
            urlPanel.add(cancelButton);
            urlPanel.add(errorLabel);

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

}
