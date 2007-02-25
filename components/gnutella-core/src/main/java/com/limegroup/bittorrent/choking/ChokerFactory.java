package com.limegroup.bittorrent.choking;

import java.util.List;

import org.limewire.collection.NECallable;
import org.limewire.concurrent.SchedulingThreadPool;

import com.limegroup.bittorrent.Chokable;

public class ChokerFactory {
	private static ChokerFactory instance;
	public static ChokerFactory instance() {
		if (instance == null)
			instance = new ChokerFactory();
		return instance;
	}
	
	protected ChokerFactory() {}
	
	public Choker getChoker(NECallable<List<? extends Chokable>> chokables,
			SchedulingThreadPool invoker,
			boolean seed) {
		return seed ? new SeedChoker(chokables, invoker) : new LeechChoker(chokables, invoker);
	}
}