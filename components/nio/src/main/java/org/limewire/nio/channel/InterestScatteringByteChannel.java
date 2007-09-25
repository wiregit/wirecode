package org.limewire.nio.channel;

import java.nio.channels.ScatteringByteChannel;

/**
 * An interface that is an interest read channel but also supports
 * the scattering reads.
 */
public interface InterestScatteringByteChannel extends InterestReadableByteChannel,
ScatteringByteChannel{}
