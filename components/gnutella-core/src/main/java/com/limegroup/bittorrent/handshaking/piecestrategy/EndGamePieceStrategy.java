package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.BitField;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.MultiIterable;
import org.limewire.collection.Range;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.disk.BlockRangeMap;

public class EndGamePieceStrategy implements PieceStrategy {
    private static final Log LOG = LogFactory.getLog(EndGamePieceStrategy.class);
    
    private final BitField bs;

    private final Set<BTInterval> exclude;

    //TODO refactor out logic drive nby this variable into two seperate Piece Strategies
    private final boolean endgame;

    private final BTMetaInfo btMetaInfo;

    private final BlockRangeMap partialBlocks;

    private final BlockRangeMap pendingRanges;

    private final BlockRangeMap requestedRanges;


    public EndGamePieceStrategy(BTMetaInfo btMetaInfo, BitField bs, Set<BTInterval> exclude,
            boolean endgame, BlockRangeMap partialBlocks, BlockRangeMap pendingRanges,
            BlockRangeMap requestedRanges) {
        this.btMetaInfo = btMetaInfo;
        this.bs = bs;
        this.exclude = exclude;
        this.endgame = endgame;
        this.partialBlocks = partialBlocks;
        this.pendingRanges = pendingRanges;
        this.requestedRanges = requestedRanges;
    }

    public List<BTInterval> getNextPieces() {
        List<BTInterval> nextPieces = new ArrayList<BTInterval>();
        BTInterval nextPiece = assignEndgame(bs, exclude, endgame);
        if (nextPiece != null) {
            nextPieces.add(nextPiece);
        }
        return nextPieces;
    }

    /**
     * Picks an interval that is already requested by another connection. This
     * is referred to as "Endgame mode" and is done when there are no other
     * pieces to request.
     */
    private BTInterval assignEndgame(BitField bs, Set<BTInterval> exclude, boolean endgame) {

        BTInterval ret = null;

        /**
         * A view of the blocks the requested and partial blocks.
         */
        Iterable<Integer> requestedAndPartial = new MultiIterable<Integer>(partialBlocks.keySet(),
                requestedRanges.keySet());

        // prepare a list of partial or requested blocks the remote host has
        Collection<Integer> available = null;
        for (int requested : requestedAndPartial) {
            if (!bs.get(requested) || (!endgame && btMetaInfo.isCompleteBlock(requested, requestedRanges))
                    || btMetaInfo.isCompleteBlock(requested, partialBlocks)) // during
                // verification
                // of block
                continue;

            if (available == null)
                available = new HashSet<Integer>(requestedRanges.size() + partialBlocks.size());
            available.add(requested);
        }

        if (available == null)
            return null;

        if (LOG.isDebugEnabled())
            LOG.debug("available partial and requested blocks to attempt: " + available);

        available = new ArrayList<Integer>(available);
        Collections.shuffle((List<Integer>) available);

        // go through and find a block that we can request something from.
        for (Iterator<Integer> iterator = available.iterator(); iterator.hasNext() && ret == null;) {
            int block = iterator.next();

            // figure out which parts of the chunks we need.
            IntervalSet needed = new IntervalSet();
            needed = needed.invert(btMetaInfo.getPieceSize(block));

            IntervalSet partial = partialBlocks.get(block);
            IntervalSet pending = pendingRanges.get(block);
            IntervalSet requested = requestedRanges.get(block);

            // get the parts of the block we're missing
            if (partial != null)
                needed.delete(partial);

            // don't request any parts pending write
            if (pending != null)
                needed.delete(pending);

            // exclude any specified intervals
            if (exclude != null) {
                for (Range excluded : exclude) {
                    needed.delete(excluded);
                }
            }
            // try not to request any parts that are already requested
            if (requested != null) {
                needed.delete(requested);

                // now, if we still have some parts of the chunk, get one of
                // them
                // if not and this is the last partial chunk, doubly-assign some
                // part of it (a.k.a. endgame?)
                if (endgame && needed.isEmpty() && !iterator.hasNext()) {
                    LOG.debug("endgame");
                    needed = requested.clone();

                    // exclude the specified intervals again
                    if (exclude != null) {
                        for (Range excluded : exclude)
                            needed.delete(excluded);
                    }
                }
            }

            if (needed.isEmpty())
                continue;

            ret = new BTInterval(needed.getFirst(), block);
            if (LOG.isDebugEnabled()) {
                LOG.debug("selected partial/requested interval " + ret + " with partial "
                        + partialBlocks.get(ret.getId()) + " requested "
                        + requestedRanges.get(ret.getId()) + " pending "
                        + pendingRanges.get(ret.getId()));
            }
        }

        // couldn't find anything to assign.
        return ret;
    }

}
