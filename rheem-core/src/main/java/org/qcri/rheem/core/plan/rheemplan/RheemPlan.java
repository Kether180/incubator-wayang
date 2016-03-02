package org.qcri.rheem.core.plan.rheemplan;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A Rheem plan consists of a set of {@link Operator}s.
 */
public class RheemPlan {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Collection<Operator> sinks = new LinkedList<>();

    private boolean isLoopsIsolated = false;

    private boolean isPruned = false;


    /**
     * Prepares this {@link RheemPlan} for the optimization process if not already done.
     */
    public void prepare() {
        this.prune();
        LoopIsolator.isolateLoops(this);
    }

    /**
     * Creates a new instance and does some preprocessing (such as loop isolation).
     *
     * @param sinks the sinks of the new instance
     * @return the prepared instance
     */
    public RheemPlan(Operator... sinks) {
        for (Operator sink : sinks) {
            this.addSink(sink);
        }
    }

    /**
     * @deprecated Use {@link RheemPlan#RheemPlan(Operator...)}.
     */
    @Deprecated
    public void addSink(Operator sink) {
        if (this.isLoopsIsolated || this.isPruned) {
            throw new IllegalStateException("Too late to add more sinks.");
        }
        Validate.isTrue(sink.isSink(), "%s is not a sink.", sink);
        Validate.isTrue(sink.getParent() == null, "%s is nested.", sink);
        this.sinks.add(sink);
    }

    /**
     * Replaces an {@code oldSink} with a {@code newSink}. As with all modifications to this instance,
     * it is up to the caller to ensure that this does not destroy the integrity of this instance.
     */
    public void replaceSink(Operator oldSink, Operator newSink) {
        if (oldSink == newSink) return;
        assert newSink.isSink();
        assert oldSink.isSink();
        if (this.sinks.remove(oldSink)) {
            assert newSink.getParent() == null;
            this.sinks.add(newSink);
        }
    }

    public Collection<Operator> getSinks() {
        return this.sinks;
    }

    /**
     * Find the source {@link Operator}s that are reachable from the sinks.
     *
     * @return the reachable sources, only top-level operators are considered
     * @see #getSinks()
     */
    public Collection<Operator> collectReachableTopLevelSources() {
        return PlanTraversal.upstream().traverse(this.sinks).getTraversedNodesWith(Operator::isSource);
    }

    /**
     * Prunes {@link Operator}s that do not (indirectly) contribute to a sink.
     */
    public void prune() {
        if (this.isPruned) return;

        final Set<Operator> reachableOperators = new HashSet<>();
        PlanTraversal.upstream()
                .withCallback((operator, input, output) -> {
                    reachableOperators.add(operator);
                    if (!operator.isElementary()) {
                        this.logger.warn("Not yet considering nested operators during Rheem plan pruning.");
                    }
                })
                .traverse(this.sinks);

        PlanTraversal.upstream()
                .withCallback(operator -> this.pruneUnreachableSuccessors(operator, reachableOperators))
                .traverse(this.sinks);

        this.isPruned = true;
    }

    /**
     * Prune any successor {@link Operator}s of the {@code baseOperator} that are not reachable/
     */
    private void pruneUnreachableSuccessors(Operator baseOperator, Set<Operator> reachableOperators) {
        for (int outputIndex = 0; outputIndex < baseOperator.getNumOutputs(); outputIndex++) {
            final OutputSlot<?> output = baseOperator.getOutput(outputIndex);
            new ArrayList<>(output.getOccupiedSlots()).stream()
                    .filter(occupiedInput -> !reachableOperators.contains(occupiedInput.getOwner()))
                    .forEach(occupiedInput -> {
                        this.logger.warn("Pruning unreachable {} from Rheem plan.", occupiedInput.getOwner());
                        output.unchecked().disconnectFrom(occupiedInput.unchecked());
                    });
        }

    }

    /**
     * Tells whether potential loops within this instance have been isolated.
     */
    public boolean isLoopsIsolated() {
        return this.isLoopsIsolated;
    }

    /**
     * Tells that potential loops within this instance have been isolated.
     */
    public void setLoopsIsolated() {
        this.isLoopsIsolated = true;
    }
}
