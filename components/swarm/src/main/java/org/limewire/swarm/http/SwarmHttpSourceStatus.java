package org.limewire.swarm.http;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.limewire.swarm.SwarmStatus;

public class SwarmHttpSourceStatus implements SwarmStatus {

    private final StatusLine statusLine;

    public SwarmHttpSourceStatus(StatusLine statusLine) {
        assert statusLine != null;
        this.statusLine = statusLine;
    }

    public boolean isError() {
        return !isOk();
    }

    public boolean isOk() {
        int status = statusLine.getStatusCode();
        return status == HttpStatus.SC_PARTIAL_CONTENT || status == HttpStatus.SC_OK;
    }

    @Override
    public String toString() {
        return statusLine.getReasonPhrase() + " code: " + statusLine.getStatusCode();
    }

    public boolean isFinished() {
        return false;
    }

}
