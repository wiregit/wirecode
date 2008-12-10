package org.limewire.ui.swing.library;

import java.io.File;

public interface LibraryTraversable {
    File getNextItem(File file);
    File getPreviousItem(File file);
}
