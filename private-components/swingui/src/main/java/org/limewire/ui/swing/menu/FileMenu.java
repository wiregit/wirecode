package org.limewire.ui.swing.menu;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        // TODO no longer a singleton, build on demand, or figure out how to
        // update items
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
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me");
            }
        });
        add(new AbstractAction(I18n.tr("Sign into Friends/Sign out of Friends")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Exit")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Application.getInstance().exit(e);
            }
        });

    }

    private Action getLocateFile() {

        return new AbstractAction(I18n.tr("Locate file")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO
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
                // TODO
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
        // TODO instead of adding downloadListManager as part of the location
        // dialogue, add listening capabilities to the location dialogue
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
            //TODO validate input while typing, disable the open button until there is a valid url.
            
            openButton = new JButton(I18n.tr("Open"));
            openButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    LocationDialogue.this.dispose();
                }
            });

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

            setContentPane(urlPanel);
            pack();
        }

        void addActionListener(ActionListener actionListener) {
            openButton.addActionListener(actionListener);
        }

        void removeActionListener(ActionListener actionListener) {
            openButton.removeActionListener(actionListener);
        }

        public synchronized URI getURI() {
            try {
                return new URI(urlField.getText());
            } catch (URISyntaxException e) {
                // should not happen, will validate while typing, like the eclipse control
                return null;
            }
        }
    }

}
