package org.limewire.swarm.http.listener;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;
import org.limewire.swarm.http.SwarmHttpContentDecoderImpl;
import org.limewire.util.Objects;

/**
 * The content listener is registered to receive
 * 
 * 
 */
public class FileContentListener implements ResponseContentListener {

    private static final Log LOG = LogFactory.getLog(FileContentListener.class);

    private final SwarmCoordinator fileCoordinator;

    private boolean finished;

    private Range range;

    private IOControl ioControl = null;

    private SwarmWriteJob writeJob = null;

    public FileContentListener(SwarmCoordinator fileCoordinator, Range range) {
        this.fileCoordinator = Objects.nonNull(fileCoordinator, "fileCoordinator");
        this.range = Objects.nonNull(range, "range");
    }

    public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        if (finished)
            throw new IOException("Already finished.");

        this.ioControl = ioctrl;

        SwarmContent content = new SwarmHttpContentDecoderImpl(decoder);

        SwarmWriteJobControl control = createControl(ioctrl);
        control.pause();

        if (writeJob == null) {
            writeJob = fileCoordinator.createWriteJob(range, control);
        }

        writeJob.write(content);
    }

    private SwarmWriteJobControl createControl(final IOControl finalIOControl) {
        SwarmWriteJobControl callBack = new SwarmWriteJobControl() {
            public void pause() {
                finalIOControl.suspendInput();
                finalIOControl.suspendOutput();
            }

            public void resume() {
                finalIOControl.requestOutput();
                finalIOControl.requestInput();
            }
        };
        return callBack;
    }

    public void finished() {
        if (!finished) {
            finished = true;
            if (range != null) {
                fileCoordinator.unlease(range);
                range = null;
            }
            try {
                if (ioControl != null) {
                    ioControl.shutdown();
                }
            } catch (IOException e) {
                // TODO handle me!
            }
        }
    }

    public void initialize(HttpResponse response) throws IOException {
        if (finished)
            throw new IOException("Already finished");

        Header contentRange = response.getFirstHeader("Content-Range");
        Header contentLengthHeader = response.getFirstHeader("Content-Length");
        Range actualRange;

        if (contentLengthHeader != null) {
            long contentLength = numberFor(contentLengthHeader.getValue());
            if (contentLength < 0)
                throw new IOException("Invalid content length: " + contentLength);

            if (contentRange != null) {
                // If a range exists, that's what we want.
                actualRange = rangeForContentRange(contentRange.getValue(), contentLength);
            } else {
                // If no range exists, assume 0 -> contentLength
                actualRange = Range.createRange(0, contentLength - 1);
            }
        } else if (contentRange == null) {
            // Fail miserably.
            throw new IOException("No Content Range Found.");
        } else {
            // Fail miserably.
            throw new IOException("No content length, though content range existed.");
        }

        validateActualRangeAndShrinkExpectedRange(actualRange);
    }

    private void validateActualRangeAndShrinkExpectedRange(Range actualRange) throws IOException {
        if (actualRange == null || range == null) {
            throw new IOException("No actual or expected range?");
        }

        if (actualRange.getLow() < range.getLow() || actualRange.getHigh() > range.getHigh()) {
            throw new IOException("Invalid actual range.  Expected: " + range + ", Actual: "
                    + actualRange);
        }

        if (!actualRange.equals(range)) {
            if (actualRange.getLow() > range.getLow()) {
                fileCoordinator
                        .unlease(Range.createRange(range.getLow(), actualRange.getLow() - 1));
                range = Range.createRange(actualRange.getLow(), range.getHigh());
            }

            if (actualRange.getHigh() < range.getHigh()) {
                fileCoordinator.unlease(Range.createRange(actualRange.getHigh() + 1, range
                        .getHigh()));
                range = Range.createRange(range.getLow(), actualRange.getHigh());
            }
        }
    }

    private Range rangeForContentRange(String headerValue, long contentLength) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("reading content range: " + headerValue);

        try {
            int start = headerValue.indexOf("bytes") + 6; // skip "bytes " or
            // "bytes="
            int slash = headerValue.indexOf('/');

            // if looks like: "bytes */*" or "bytes */10" -- NOT part of the
            // spec
            if (headerValue.substring(start, slash).equals("*")) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Content-Range like */?, " + headerValue);
                return Range.createRange(0, contentLength - 1);
            }

            int dash = headerValue.lastIndexOf("-");
            long numBeforeDash = numberFor(headerValue.substring(start, dash));
            long numBeforeSlash = numberFor(headerValue.substring(dash + 1, slash));

            if (numBeforeSlash < numBeforeDash)
                throw new IOException("invalid range, high (" + numBeforeSlash
                        + ") less than low (" + numBeforeDash + ")");

            return Range.createRange(numBeforeDash, numBeforeSlash);

            // TODO: Is it necessary to validate the number after the slash
            // matches the fileCoordinator's size (or is '*')?
        } catch (IndexOutOfBoundsException e) {
            throw IOUtils.getIOException("Invalid Header: " + headerValue, e);
        }

    }

    private static long numberFor(String number) throws IOException {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException nfe) {
            throw IOUtils.getIOException("Invalid number: " + number, nfe);
        }
    }

}
