package com.limegroup.gnutella.util;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RRProcessingQueue extends ProcessingQueue {
	
	private static final Log LOG = LogFactory.getLog(RRProcessingQueue.class);

	private final Map queues = new HashMap();
	private final RoundRobinQueue lists = new RoundRobinQueue();
	
	private int size;
	
	public RRProcessingQueue(String name, boolean managed, int priority) {
		super(name, managed, priority);
	}

	public RRProcessingQueue(String name, boolean managed) {
		super(name, managed);
	}

	public RRProcessingQueue(String name) {
		super(name);
	}

	public synchronized void invokeLater(Runnable runner, Object queueId) {
		NamedQueue queue = (NamedQueue)queues.get(queueId);
		if (queue == null) {
			queue = new NamedQueue(new LinkedList(), queueId);
			queues.put(queueId, queue);
			lists.enqueue(queue);
		}
		queue.list.add(runner);
		size++;
			
		notifyAndStart();
	}
	
	public synchronized void invokeLater(Runnable r) {
		invokeLater(r, this);
	}
	
	protected synchronized boolean moreTasks() {
		return size > 0;
	}
	
	protected synchronized Runnable next() {
		Runnable ret = null;
		while (lists.size() > 0) {
			NamedQueue next = (NamedQueue)lists.next();
			ret = next.next();
			if (ret == null || next.list.isEmpty()) {
				lists.removeAllOccurences(next);
				queues.remove(next.name);
			} 
			
			if (ret != null) {
				size--;
				return ret;
			}
		}
		return null;
	}

	
	public synchronized int size() {
		return size;
	}
	
	public synchronized void clear() {
		if (LOG.isDebugEnabled())
			LOG.debug("removing all "+size +" jobs from "+queues.size()+" queues");
		
		queues.clear();
		lists.clear();
		size = 0;
	}
	
	public synchronized void clear(Object name) {
		NamedQueue toRemove = (NamedQueue)queues.remove(name);
		if (toRemove == null)
			return;
		lists.removeAllOccurences(toRemove);
		
		if (LOG.isDebugEnabled())
			LOG.debug("removing "+toRemove.list.size()+" jobs out of "+size);
		
		size -= toRemove.list.size();
	}
	
	private class NamedQueue {
		final List list;
		final Object name;
		NamedQueue (List list, Object name) {
			this.list = list;
			this.name = name;
		}
		
		Runnable next() {
			return list.isEmpty() ? null : (Runnable) list.remove(0);
		}
	}
}
