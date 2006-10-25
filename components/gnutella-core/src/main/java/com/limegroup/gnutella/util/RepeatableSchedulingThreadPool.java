package com.limegroup.gnutella.util;

import java.util.concurrent.Future;

public interface RepeatableSchedulingThreadPool extends SchedulingThreadPool {
    public Future invokeLater(Runnable r, long delay, long period);
}
