package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

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
        // TODO no longer a singleton build on demand, or figure out how to
        // update items
        JMenuItem fileItem = getFileMenuItem(downloadListManager);
        add(fileItem);
        JMenuItem linkItem = getUrlMenuItem(downloadListManager);
        add(linkItem);
        JMenu recentDownloads = getRecentDownloads();
        add(recentDownloads);
        addSeparator();
        JMenuItem addFile = getAddFile(libraryManager);
        add(addFile);
        JMenuItem addFolder = getAddFolder(libraryManager);
        add(addFolder);
        addSeparator();
        JMenuItem launchFile = getLaunchItem();
        add(launchFile);
        JMenuItem locateFile = getLocateFile();
        add(locateFile);
        addSeparator();
        add(new JMenuItem(I18n.tr("Set as available")));
        add(new JMenuItem(I18n.tr("Set as away")));
        addSeparator();
        add(new JMenuItem(I18n.tr("Switch user")));
        add(new JMenuItem(I18n.tr("Sign into Friends/Sign out of Friends")));
        addSeparator();
        add(new JMenuItem(I18n.tr("Exit")));

    }

    private JMenuItem getLocateFile() {
        return new JMenuItem(I18n.tr("Locate file"));
    }

    private JMenuItem getLaunchItem() {
        return new JMenuItem(I18n.tr("Launch file"));
    }

    private JMenuItem getAddFolder(final LibraryManager libraryManager) {
        JMenuItem addFolder = new JMenuItem(I18n.tr("Add Folder"));
        addFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> folders = FileChooser.getInput(FileMenu.this, I18n.tr("Add Folder(s)"),
                        I18n.tr("Add Folder(s)"), FileChooser.getLastInputDirectory(),
                        JFileChooser.DIRECTORIES_ONLY, JFileChooser.APPROVE_OPTION, true,
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                String extension = FileUtils.getFileExtension(f);
                                return f.isDirectory();
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr("TODO get better description");
                            }
                        });

                if (folders != null) {
                    for (File folder : folders) {
                        // TODO run in background thread
                        libraryManager.getLibraryManagedList().addFolder(folder);
                    }
                }

            }
        });
        return addFolder;
    }

    private JMenuItem getAddFile(final LibraryManager libraryManager) {
        JMenuItem addFile = new JMenuItem(I18n.tr("Add File"));
        addFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<File> files = FileChooser.getInput(FileMenu.this, I18n.tr("Add File(s)"), I18n
                        .tr("Add Files"), FileChooser.getLastInputDirectory(),
                        JFileChooser.FILES_ONLY, JFileChooser.APPROVE_OPTION, true,
                        new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                String extension = FileUtils.getFileExtension(f);
                                // TODO filter extensions enabled in options
                                return f.isDirectory() || f.isFile();
                            }

                            @Override
                            public String getDescription() {
                                return I18n.tr("TODO get a descripotion");
                            }
                        });
                if (files != null) {
                    for (File file : files) {
                        // TODO run in background thread
                        libraryManager.getLibraryManagedList().addFile(file);
                    }
                }
            }
        });
        return addFile;
    }

    private JMenu getRecentDownloads() {
        return new JMenu(I18n.tr("Recent Downloads"));
    }

    private JMenuItem getFileMenuItem(final DownloadListManager downloadListManager) {
        JMenuItem fileItem = new JMenuItem(I18n.tr("Open File"));
        fileItem.addActionListener(new ActionListener() {
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
                                return I18n.tr(".torrent|.magnet");
                            }
                        });

                if (files == null) {
                    return;
                }

                for (File file : files) {
                    try {
                        downloadListManager.addDownload(file);
                        navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select();
                    } catch (SaveLocationException e1) {
                        // TODO better user feedback
                        throw new UnsupportedOperationException(e1);
                    }

                }
            }
        });

        return fileItem;
    }

    private JMenuItem getUrlMenuItem(final DownloadListManager downloadListManager) {
        // TODO instead of adding downloadListManager a part of the location
        // dialogue, add listening capabilities to the location dialogue
        JMenuItem linkItem = new JMenuItem(I18n.tr("Open Link"));
        linkItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LocationDialogue locationDialogue = new LocationDialogue(downloadListManager);
                locationDialogue.setVisible(true);
            }
        });
        return linkItem;
    }

    private class LocationDialogue extends JDialog {
        public LocationDialogue(final DownloadListManager downloadListManager) {
            super();
            JPanel urlPanel = new JPanel();
            JLabel urlLabel = new JLabel(I18n.tr("URL:"));
            final JTextField urlField = new JTextField(50);
            urlField.setText("http://");

            JButton openButton = new JButton(I18n.tr("Open"));
            openButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    String uriString = urlField.getText();
                    try {
                        URI uri = new URI(uriString);
                        downloadListManager.addDownload(uri);
                        navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select();
                        dispose();
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

            setContentPane(urlPanel);
        }
    }

}
