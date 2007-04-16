package com.limegroup.gnutella.gui.upload;

import junit.framework.TestCase;

import com.limegroup.gnutella.gui.upload.UploadProgressBarRenderer.UploadProgressBarData;

public class UploadProgressBarRendererTest extends TestCase {

    public void testGetBarStatus() {
        UploadProgressBarRenderer renderer = new UploadProgressBarRenderer();
        UploadProgressBarData data = new UploadProgressBarData(5949295, false);
        data.totalUploaded = data.totalSize;
        assertEquals(100, renderer.getBarStatus(data));
    }

}
