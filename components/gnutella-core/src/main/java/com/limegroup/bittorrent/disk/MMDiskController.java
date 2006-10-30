package com.limegroup.bittorrent.disk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Experimental disk controller that uses Memory Mapped files
 * for disk io.  If a file cannot be mapped for some reason, it 
 * falls back to the RAFDiskController behavior.
 */
public class MMDiskController<F extends File> extends RAFDiskController<F> {

	private static final Log LOG = LogFactory.getLog(MMDiskController.class);
	
	/*
	 * Note: because of Bug ID:4724038 System.gc() is being called
	 * explicitly in a few places.
	 */
	
	private Map<RandomAccessFile, MappedByteBuffer> bufMap;

	@Override
	public List<F> open(List<F> files, boolean complete, boolean isVerifying) throws IOException {
		List<F> ret =  super.open(files, complete, isVerifying);
		
		Map<RandomAccessFile, MappedByteBuffer> bMap = 
			new HashMap<RandomAccessFile, MappedByteBuffer>(files.size());
		
		for (int i = 0; i < _fos.length; i++) {
			
			if (_files.get(i).length() >= Integer.MAX_VALUE)
				continue; // not even in 64 bit jvms
			
			RandomAccessFile raf;
			synchronized(this) {
				raf = _fos[i];
			}
			try {
				bMap.put(raf,
						raf.getChannel().map(
								complete ? MapMode.READ_ONLY : MapMode.READ_WRITE, 
										0, 
										_files.get(i).length()));
				if (LOG.isDebugEnabled())
					LOG.debug("mapped "+_files.get(i));
			} catch (IOException mapFailed) {
				// fallback to regular.
				if (LOG.isDebugEnabled())
					LOG.debug("didn't map "+_files.get(i), mapFailed);
			} 
		}
		
		synchronized(this) {
			bufMap = bMap;
		}
		
		return ret;
	}

	@Override
	protected void writeImpl(RandomAccessFile f, long fileOffset, byte[] data, int offset, int length) throws IOException {
		MappedByteBuffer buf = getBuf(f);
		if (buf == null) {
			super.writeImpl(f, fileOffset, data, offset, length);
			return;
		}
		buf.position((int)fileOffset);
		buf.put(data, offset, length);
	}

	@Override
	protected int readImpl(RandomAccessFile raf, long fileOffset, byte[] dst, int offset, int length) throws IOException {
		MappedByteBuffer buf = getBuf(raf);
		if (buf == null) 
			return super.readImpl(raf, fileOffset, dst, offset, length);
		
		buf.position((int)fileOffset);
		buf.get(dst, offset, length);
		return buf.position() - (int)fileOffset;
	}
	
	private synchronized MappedByteBuffer getBuf(RandomAccessFile f) {
		return bufMap.get(f);
	}
	
	public void close() {
		LOG.debug("closing...");
		Collection<MappedByteBuffer> bufs = new ArrayList<MappedByteBuffer>();
		synchronized(this) {
			bufs.addAll(bufMap.values());
			bufMap.clear();
		}
		boolean allCleaned = false;
		try {
			boolean cleaned = true;
			for (MappedByteBuffer mbb : bufs) {
				safeForce(mbb);
				cleaned = cleaned && clean(mbb);
			}
			allCleaned = cleaned;
		} finally {
			bufs.clear();
			if (!allCleaned)
				System.gc(); 
		}
		super.close();
	}

	@Override
	public void setReadOnly(RandomAccessFile raf, String path) throws IOException {
		MappedByteBuffer buf;
		synchronized(this) {
			buf = bufMap.remove(raf);
		}
		try {
			safeForce(buf);
		} finally {
			if (!clean(buf)) {
				buf = null;
				System.gc();
			}
		}
		super.setReadOnly(raf, path);
		buf = raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length());
		synchronized(this) {
			bufMap.put(raf, buf);
		}
	}
	
	private void safeForce(MappedByteBuffer buf) {
		try {
			buf.force();
		} catch (Exception iox) {
			if (iox instanceof IOException) 
				return; // BugID 5074836
			RuntimeException r;
			if (iox instanceof RuntimeException) 
				r = (RuntimeException)iox;
			else
				r = new RuntimeException(iox);
			throw r;
		}
	}
	
	public static boolean clean(final Object buffer) {

		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

			public Boolean run() {
 				try {
					Method getCleanerMethod = buffer.getClass().getMethod("cleaner",new Class[0]);
					if (getCleanerMethod == null)
						return false;

					getCleanerMethod.setAccessible(true);

					sun.misc.Cleaner cleaner =
						(sun.misc.Cleaner)getCleanerMethod.invoke(buffer,new Object[0]);

					if (cleaner == null)
						return false;
					cleaner.clean();
					return true;

				} catch(SecurityException e) {
                    LOG.warn("security", e);
                } catch (InvocationTargetException e) {
                    LOG.warn("invocation", e);
                } catch (NoSuchMethodException e) {
                    LOG.warn("no method", e);
                } catch (IllegalAccessException e) {
                    LOG.warn("illegal access", e);
                }
				return false;
			}

		});

	}

}

