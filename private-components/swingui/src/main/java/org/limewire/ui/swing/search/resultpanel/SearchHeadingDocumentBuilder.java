package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.model.BasicDownloadState;

public interface SearchHeadingDocumentBuilder {

    String getHeadingDocument(String heading, BasicDownloadState downloadState, boolean mouseOver);

}