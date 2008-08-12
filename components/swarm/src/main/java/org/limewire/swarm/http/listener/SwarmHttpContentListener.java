package org.limewire.swarm.http.listener;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;
import org.limewire.swarm.http.SwarmHttpContentImpl;
import org.limewire.swarm.http.SwarmHttpUtils;
import org.limewire.util.Objects;

public class SwarmHttpContentListener implements ResponseContentListener {
    
    private final SwarmCoordinator fileCoordinator;

    private boolean finished;

    private Range leaseRange;

    private SwarmWriteJob writeJob;

    private final SwarmFile swarmFile;

    public SwarmHttpContentListener(SwarmCoordinator fileCoordinator, SwarmFile swarmFile,
            Range range) {
        this.fileCoordinator = Objects.nonNull(fileCoordinator, "fileCoordinator");
        this.leaseRange = Objects.nonNull(range, "range");
        this.swarmFile = swarmFile;
    }

    public void contentAvailable(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        if (finished) {
            throw new IOException("Already finished.");
        }

        if (!decoder.isCompleted()) {
            if (writeJob == null) {
                writeJob = fileCoordinator.createWriteJob(leaseRange, createControl(ioctrl));
            }
            writeJob.write(new SwarmHttpContentImpl(decoder));
        }
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
            if (leaseRange != null) {
                fileCoordinator.unlease(leaseRange);
                leaseRange = null;
            }
        }
    }

    public void initialize(HttpResponse response) throws IOException {
        if (finished) {
            throw new IOException("Already finished");
        }

        Range actualRange = SwarmHttpUtils.parseContentRange(response);
        long startByte = swarmFile.getStartByte();

        actualRange = Range.createRange(startByte + actualRange.getLow(), startByte
                + actualRange.getHigh());
        validateActualRangeAndShrinkExpectedRange(actualRange);
    }

    private void validateActualRangeAndShrinkExpectedRange(Range actualRange) throws IOException {
        if (actualRange == null || leaseRange == null) {
            throw new IOException("No actual or expected range?");
        }

        if (actualRange.getLow() < leaseRange.getLow()
                || actualRange.getHigh() > leaseRange.getHigh()) {
            // TODO handle off ranges
            // this exception gets eaten inside the httpnio code, so we need
            // better logging/handling on our end
            throw new IOException("Invalid actual range.  Expected: " + leaseRange
                    + ", Actual: " + actualRange);
        }

        if (!actualRange.equals(leaseRange)) {
            // TODO double check this logic
            leaseRange = fileCoordinator.renewLease(leaseRange, actualRange);
        }
    }
}
