package com.limegroup.gnutella.tests;

import junit.framework.*;
import junit.extensions.*;

/**
 * This class tests the public methods of the URNFactory class.
 */
public final class UrnFactoryTest extends TestCase {

	public UrnFactoryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(UrnFactoryTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
