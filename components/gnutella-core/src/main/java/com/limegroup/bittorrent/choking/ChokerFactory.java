package com.limegroup.bittorrent.choking;

import java.util.List;

import com.limegroup.bittorrent.Chokable;
import com.limegroup.gnutella.util.NECallable;
import com.limegroup.gnutella.util.SchedulingThreadPool;

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