package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;

/**
 * This class offloads the construction of <tt>FileDesc</tt> instances
 * onto another thread, due to the heavy load of calculating SHA1 
 * values.
 */
public final class FileDescLoader implements Runnable {
	
	/**
	 * <tt>List</tt> of <tt>FileDesc</tt> instances to be loaded.
	 */
	private final List FILE_QUEUE;
 
	/**
	 * <tt>Thread</tt> instance for constructing the <tt>FileDesc</tt>
	 * instances.
	 */
	private Thread _loadThread;

	/**
	 * Handle to the <tt>FileManager</tt> instance.
	 */
	private final FileManager FILE_MANAGER;

	/**
	 * Constructs a new <tt>FileLoader</tt> instance for loading 
	 * <tt>FileDesc</tt> instances.
	 */
	public FileDescLoader(final FileManager FILE_MANAGER) {
		FILE_QUEUE = new ArrayList();
		this.FILE_MANAGER = FILE_MANAGER;
	}

	/**
	 * Creates a <tt>FileDesc</tt> instance and returns it.  This is
	 * the preferred method for creating <tt>FileDesc</tt>s, as it
	 * also handles any necessary background processing (such as
	 * SHA1 calculation).
	 *
	 * @param file the file to use for the <tt>FileDesc</tt> instance
	 * @param fileIndex the index of the file in the <tt>FileManager</tt>
	 * @return a new <tt>FileDesc</tt> instance
	 */
	public FileDesc createFileDesc(File file, int fileIndex) {
		Collection urns = UrnCache.instance().getUrns(file);
		FileDesc fileDesc = new FileDesc(file, fileIndex, urns);
		this.loadFileDesc(fileDesc);
		return fileDesc;
	}

	/**
	 * Loads a <tt>FileDesc</tt> instance for URN calculation.  This
	 * does nothing if the <tt>FileDesc</tt> already has a valid
	 * set of urns.
	 *
	 * @param fileDesc the <tt>FileDesc</tt> instance to add to the
	 *  pending list of <tt>FileDesc</tt> whose URNs still need
	 *  calculating
	 */	
	public void loadFileDesc(FileDesc fileDesc) {
		
		// if the FileDesc has an up to date urn index, don't recalculate
		if(fileDesc.shouldCalculateUrns()) {
			return;
		}
		// put the file on the queue for processing
		FILE_QUEUE.add(fileDesc);
		if((_loadThread == null) || (!_loadThread.isAlive())) {
			_loadThread = new Thread(this);
			_loadThread.setPriority(Thread.currentThread().getPriority()-1);
			_loadThread.start();
		}
	}

	/**
	 * Takes files off of the processing queue and constructs new 
	 * <tt>FileDesc</tt> instances from the <tt>File</tt> instances
	 * on the queue.
	 */
	public void run() {
		while(!FILE_QUEUE.isEmpty()) {
			FileDesc fd = (FileDesc)FILE_QUEUE.remove(0);
			fd.calculateUrns();
			UrnCache.instance().persistUrns(fd);
			FILE_MANAGER.updateUrnIndex(fd);
		}		
	}	
}
