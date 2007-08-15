/**
 * 
 */
package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.http.AltLocTracker;


class MockHTTPUploader extends HTTPUploader {
    private MockAltLocTracker tracker = new MockAltLocTracker();

    public MockHTTPUploader() {
        super(null, null);
    }

    @Override
    public AltLocTracker getAltLocTracker() {
        return tracker;
    }
}