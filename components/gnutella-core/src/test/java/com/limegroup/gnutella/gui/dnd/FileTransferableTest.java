package com.limegroup.gnutella.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.limegroup.gnutella.util.BaseTestCase;

public class FileTransferableTest extends BaseTestCase {

	public FileTransferableTest(String name) {
		super(name);
	}
	
	public void testGetTransferData() throws UnsupportedFlavorException, IOException {
		List<File> files = Arrays.asList(new File("/test/file1"), new File("/test/file2"));
		Transferable transferable = new FileTransferable(files);
		assertEquals(files, transferable.getTransferData(DataFlavor.javaFileListFlavor));
		assertEquals("file:/test/file1\nfile:/test/file2",
				transferable.getTransferData(FileTransferable.URIFlavor));
	}

}
