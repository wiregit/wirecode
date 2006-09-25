package com.limegroup.gnutella.io;

import java.nio.channels.ScatteringByteChannel;

/**
 * An interface that is an interest read channel but also supports
 * the scattering reads.
 */
public interface InterestScatteringByteChannel extends InterestReadChannel,
ScatteringByteChannel{}
