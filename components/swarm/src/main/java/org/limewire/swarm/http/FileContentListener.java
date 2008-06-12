package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.file.WriteJob;
import org.limewire.util.Objects;


class FileContentListener implements ResponseContentListener {
    
    private static final Log LOG = LogFactory.getLog(FileContentListener.class);
    
    private final FileCoordinator fileCoordinator;
    private boolean finished;
    private Range expectedRange;
    private Range actualRange;
    private WriteJob writeJob;

    public FileContentListener(FileCoordinator fileCoordinator, Range range) {
        this.fileCoordinator = Objects.nonNull(fileCoordinator, "fileCoordinator");
        this.expectedRange = Objects.nonNull(range, "range");
    }

    public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        if(finished)
            throw new IOException("Already finished.");
        
        long low = expectedRange.getLow();
        long high = expectedRange.getHigh();
        
        if(writeJob == null) {
            writeJob = fileCoordinator.newWriteJob(low, ioctrl);
        }
        
        long consumed = writeJob.consumeContent(decoder);
        
        // Reposition expectedRange so that we can release ranges
        // if we get cancelled.
        if(consumed > 0) {
            long newLow = low + consumed;
            if (newLow > high) {
                expectedRange = null;
            } else {
                expectedRange = Range.createRange(newLow, high);
            }
        }
        
    }

    public void finished() {
        if(!finished) {
            finished = true;
            if(expectedRange != null) {
                fileCoordinator.unlease(expectedRange);
                expectedRange = null;
            }
        }
    }

    public void initialize(HttpResponse response) throws IOException {
        if(finished)
            throw new IOException("Already finished");
        
        Header contentRange = response.getFirstHeader("Content-Range");
        Header contentLengthHeader = response.getFirstHeader("Content-Length");
        
        if(contentLengthHeader != null) {
            long contentLength = numberFor(contentLengthHeader.getValue());
            if(contentLength < 0)
                throw new IOException("Invalid content length: " + contentLength);
            
            if(contentRange != null) {
                // If a range exists, that's what we want.
                actualRange = rangeForContentRange(contentRange.getValue(), contentLength);
            } else {
                // If no range exists, assume 0 -> contentLength
                actualRange = Range.createRange(0, contentLength-1);
            }
        } else if(contentRange == null) {
            // If no range exists nor content length, assume whole file
            actualRange = Range.createRange(0, fileCoordinator.getSize()-1);
        } else {
            // Fail miserably.
            throw new IOException("No content length, though content range existed.");
        }
        
        validateActualRangeAndShrinkExpectedRange();
    }
    
    private void validateActualRangeAndShrinkExpectedRange() throws IOException {
        if (actualRange == null || expectedRange == null) {
            throw new IOException("No actual or expected range?");
        }

        if (actualRange.getLow() < expectedRange.getLow()
                || actualRange.getHigh() > expectedRange.getHigh()) {
            throw new IOException("Invalid actual range.  Expected: " + expectedRange
                    + ", Actual: " + actualRange);
        }
        
        if (!actualRange.equals(expectedRange)) {
            if (actualRange.getLow() > expectedRange.getLow()) {
                fileCoordinator.unlease(Range.createRange(expectedRange.getLow(),
                                                          actualRange.getLow()));
                expectedRange = Range.createRange(actualRange.getLow(), expectedRange.getHigh());
            }

            if (actualRange.getHigh() < expectedRange.getHigh()) {
                fileCoordinator.unlease(Range.createRange(actualRange.getHigh(),
                                                          expectedRange.getHigh()));
                expectedRange = Range.createRange(expectedRange.getLow(), actualRange.getHigh());
            }
        }
    }
    
    
    private Range rangeForContentRange(String headerValue, long contentLength) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("reading content range: " + headerValue);

        try {
            int start = headerValue.indexOf("bytes") + 6; //skip "bytes " or "bytes="
            int slash = headerValue.indexOf('/');

            //if looks like: "bytes */*" or "bytes */10" -- NOT part of the spec
            if (headerValue.substring(start, slash).equals("*")) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Content-Range like */?, " + headerValue);
                return Range.createRange(0, contentLength-1);
            }

            int dash = headerValue.lastIndexOf("-");
            long numBeforeDash = numberFor(headerValue.substring(start, dash));
            long numBeforeSlash = numberFor(headerValue.substring(dash + 1, slash));

            if (numBeforeSlash < numBeforeDash)
                throw new IOException("invalid range, high (" + numBeforeSlash
                        + ") less than low (" + numBeforeDash + ")");

            return Range.createRange(numBeforeDash, numBeforeSlash);
            
            // TODO: Is it necessary to validate the number after the slash
            //       matches the fileCoordinator's size (or is '*')?
        } catch (IndexOutOfBoundsException e) {
            throw IOUtils.getIOException("Invalid Header: " + headerValue, e);
        }

    }
    
    private static long numberFor(String number) throws IOException {
        try {
            return Long.parseLong(number);
        } catch(NumberFormatException nfe) {
            throw IOUtils.getIOException("Invalid number: " + number, nfe);
        }
    }

}
