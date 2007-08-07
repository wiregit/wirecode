package com.limegroup.bittorrent.choking;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.collection.NECallable;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.Chokable;

@Singleton
public class ChokerFactory {
    
	public Choker getChoker(NECallable<List<? extends Chokable>> chokables,
			ScheduledExecutorService invoker,
			boolean seed) {
		return seed ? new SeedChoker(chokables, invoker) : new LeechChoker(chokables, invoker);
	}
}