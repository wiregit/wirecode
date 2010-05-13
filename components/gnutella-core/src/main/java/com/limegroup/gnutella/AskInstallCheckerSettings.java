package com.limegroup.gnutella;

import java.io.File;

/**
 * Accessor class for the toolbar file. This is a seperate class to
 * enable testing easier by mocking out the result file.
 */
class AskInstallCheckerSettings {

    public File getFile() {
        File file = new File(".",  "toolbarResult");
        return file;
    }
}
