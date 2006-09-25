package com.limegroup.gnutella.util;

import java.util.concurrent.Future;

public interface SchedulingThreadPool extends ThreadPool {
	public Future invokeLater(Runnable r, long delay);
}
