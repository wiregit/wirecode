package org.limewire.core.impl.inspections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.api.Invocation;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inspection.Inspector;
import org.limewire.collection.SortedList;

/**
 * Unit tests to test:
 * 
 * - test inspections communicator scheduling inspections
 */
public class InspectionsSchedulingTest extends LimeTestCase {
    
    private Mockery context;

    public InspectionsSchedulingTest(String name) {
        super(name);
    }


    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    // test that the Inspections Communicator performs the 
    // inspections as specified in the InspectionsSpec objects
    // (3 seconds initial delay, 5 second repeating interval)
    // Confirm by checking time stamp of inspections results.
    //
    public void testScheduledRepeatingInspections() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        final IncrementingAction count = new IncrementingAction(4);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("sample data", true);
            will(count);
        }});
        ScheduledExecutorService scheduler = getScheduler(15, 1);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);

        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.singletonList("sample data");
        long timeToStartInsp = 3L;
        long interval = 5L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        ic.initInspectionSpecs(inspSpecs);
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        
        assertEquals(3, inspData.size());
        assertEquals(1, inspData.get(0).getResultCount());
        assertEquals(4, inspData.get(0).getData("sample data"));
        assertEquals(1, inspData.get(1).getResultCount());
        assertEquals(5, inspData.get(1).getData("sample data"));
        assertEquals(1, inspData.get(2).getResultCount());
        assertEquals(6, inspData.get(2).getData("sample data"));
        assertNull(inspData.get(2).getData("no data"));
    }

    // test that the Inspections Communicator performs the 
    // inspections as specified in the InspectionsSpec objects
    // (3 seconds initial delay, no repeating, one time inspection only)
    // Confirm by checking time stamp of inspections results.
    //
    public void testScheduledNonRepeatingInspections() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("sample data", true);
            will(returnValue(15));
        }});
        ScheduledExecutorService scheduler = getScheduler(15, 1);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);    
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.singletonList("sample data");
        long timeToStartInsp = 3L;
        long interval = 0L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        ic.initInspectionSpecs(inspSpecs);
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        
        assertEquals(1, inspData.size());
        assertEquals(1, inspData.get(0).getResultCount());
        assertEquals(15, inspData.get(0).getData("sample data"));
        assertNull(inspData.get(0).getData("sample data two not there"));
    }
    
    // test inspections communicator with one InspectionsSpec 
    // with no inspections
    public void testNoInspectionsInOneInspectionsSpec() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        ScheduledExecutorService scheduler = getScheduler(15, 1);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);    
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.emptyList();
        long timeToStartInsp = 3L;
        long interval = 0L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        ic.initInspectionSpecs(inspSpecs);
        assertEquals(0, inspectionsProcessor.getInspectionResults().size());
    }
    
    // test inspections communicator with one InspectionsSpec 
    // with multiple inspections
    public void testMultipleInspectionsInOneInspectionsSpec() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        final IncrementingAction count = new IncrementingAction(4);
        final IncrementingAction count2 = new IncrementingAction(12);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("sample data", true);
            will(count);
            allowing(inspector).inspect("more stuff", true);
            will(count2);
        }});
        ScheduledExecutorService scheduler = getScheduler(15, 1);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);    
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Arrays.asList("sample data", "more stuff");
        long timeToStartInsp = 3L;
        long interval = 5L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        ic.initInspectionSpecs(inspSpecs);
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        
        assertEquals(3, inspData.size());
        assertEquals(4, inspData.get(0).getData("sample data"));
        assertEquals(12, inspData.get(0).getData("more stuff"));
        assertEquals(2, inspData.get(0).getResultCount());
        assertEquals(5, inspData.get(1).getData("sample data"));
        assertEquals(13, inspData.get(1).getData("more stuff"));
        assertEquals(2, inspData.get(1).getResultCount());
        assertEquals(6, inspData.get(2).getData("sample data"));
        assertEquals(14, inspData.get(2).getData("more stuff"));
        assertEquals(2, inspData.get(2).getResultCount());
    }
    
    // test inspections communicator with multiple InspectionsSpec 
    // with multiple inspections
    public void testMultipleInspectionsInMultipleInspectionsSpecs() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        final IncrementingAction count = new IncrementingAction(8);
        final IncrementingAction count2 = new IncrementingAction(15);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("first data", true);
            will(count);
            allowing(inspector).inspect("three insp", true);
            will(count);
            allowing(inspector).inspect("second data", true);
            will(count2);
            allowing(inspector).inspect("two insp", true);
            will(count2);
        }});
        ScheduledExecutorService scheduler = getScheduler(15, 2);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        inspSpecs.add(new InspectionsSpec(Arrays.asList("first data", "three insp"), 2L, 6L));
        inspSpecs.add(new InspectionsSpec(Arrays.asList("second data", "two insp"), 9L, 4L));
        ic.initInspectionSpecs(inspSpecs);
        
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        assertEquals(5, inspData.size());
        assertEquals(2, inspData.get(0).getResultCount());
        assertEquals(8, inspData.get(0).getData("first data"));
        assertEquals(9, inspData.get(0).getData("three insp"));
        assertEquals(2, inspData.get(1).getResultCount());
        assertEquals(10, inspData.get(1).getData("first data"));
        assertEquals(11, inspData.get(1).getData("three insp"));
        assertEquals(2, inspData.get(2).getResultCount());
        assertEquals(15, inspData.get(2).getData("second data"));
        assertEquals(16, inspData.get(2).getData("two insp"));
        assertEquals(2, inspData.get(3).getResultCount());
        assertEquals(17, inspData.get(3).getData("second data"));
        assertEquals(18, inspData.get(3).getData("two insp"));
        assertEquals(2, inspData.get(4).getResultCount());
        assertEquals(12, inspData.get(4).getData("first data"));
        assertEquals(13, inspData.get(4).getData("three insp"));
    }

    /**
     * Instead of sending to inspections server, for the unit test 
     * all this {@link InspectionsResultProcessor} impl does is put 
     * inspection results in a list
     */
    static class QueuingInspectionsResultProcessor implements InspectionsResultProcessor {
        final List<InspectionDataContainer> inspectionResults = new ArrayList<InspectionDataContainer>();
        
        public List<InspectionDataContainer> getInspectionResults() {
            return inspectionResults;
        }

        @Override
        public void inspectionsPerformed(InspectionsSpec spec, InspectionDataContainer insps){
            inspectionResults.add(insps);
        }

        @Override public void stopped() {}
    }

    /**
     * Mocks up a {@link ScheduledExecutorService} which does not actually schedule anything
     * to execute at any particular time, but instead keeps the scheduled tasks in 
     * order, and executes all the tasks in order as soon as all expected inspection specs 
     * have been scheduled.
     * 
     * @param secondsToRun Duration of time under test
     * @param numInspSpecs number of inspections specs. Assumes each spec is scheduled once
     * @return ScheduledExecutorService
     */
    private ScheduledExecutorService getScheduler(final int secondsToRun, final int numInspSpecs) {
        final ScheduledExecutorService scheduler = context.mock(ScheduledExecutorService.class);
       
        
        final InspectionCustomAction action = 
                new InspectionCustomAction("inspection scheduler", secondsToRun, numInspSpecs);
        
        context.checking(new Expectations() {{
            exactly(numInspSpecs).of(scheduler).schedule(with(any(Runnable.class)), with(any(Long.class)), with(any(TimeUnit.class)));
            will(action);
            exactly(numInspSpecs).of(scheduler).scheduleWithFixedDelay(with(any(Runnable.class)), with(any(Long.class)), 
                    with(any(Long.class)), with(any(TimeUnit.class)));
            will(action);
        }});
        
        return scheduler;
    }

    /**
     * A jmock {@link CustomAction} which keeps an ordered list
     * of tasks as they are being scheduled. It then executes all
     * the tasks in order after all expected tasks have been scheduled.
     * 
     */
    private class InspectionCustomAction extends CustomAction {
        private final SortedList<RunnableWrapper> runnables = new SortedList<RunnableWrapper>();
        private final int secondsToRun;
        private int numInspSpecsExpected;

        public InspectionCustomAction(String s, int secondsToRun, int numInspSpecsExpected) {
            super(s);
            this.secondsToRun = secondsToRun;
            this.numInspSpecsExpected = numInspSpecsExpected;
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            boolean isPeriodic = invocation.getInvokedMethod().getName().equals("scheduleWithFixedDelay");
            Runnable runnable = (Runnable)invocation.getParameter(0);
            long initDelay = ((Long) invocation.getParameter(1));
            long delay = isPeriodic ? ((Long) invocation.getParameter(2)) : 0;
            addRunnables(runnable, initDelay, delay);
            numInspSpecsExpected--;
            
            if (numInspSpecsExpected == 0) {
                // all inspection tasks have been added in order; run them now
                for (Runnable addedRunnable : runnables) {
                    addedRunnable.run();
                }
            }
            return null;
        }
        
        private void addRunnables(Runnable runnable, long initDelay, long delay) {
            long currSec = initDelay;
            while (currSec < secondsToRun) {
                runnables.add(new RunnableWrapper(runnable, currSec));
                if (delay <= 0) {
                    break;
                }
                currSec += delay;
            }
        }
    }

    /**
     * A wrapper class which contains info on when the Runnable WOULD BE
     * executed IF a real ScheduledExecutorService was used.
     */
    private class RunnableWrapper implements Comparable<RunnableWrapper>, Runnable {
        Runnable runnable;
        long seconds;

        RunnableWrapper(Runnable runnable, long seconds) {
            this.runnable = runnable;
            this.seconds = seconds;
        }
        @Override public int compareTo(RunnableWrapper o) {
            return (int)seconds - (int)o.seconds;
        }
        @Override public void run() {
            runnable.run();
        }
    }

    /**
     * A jmock {@link CustomAction} which increments a count member variable
     * each time it is invoked.
     */
    private class IncrementingAction extends CustomAction {
        private int value;
        
        IncrementingAction(int startingValue) {
            super("Incrementing Action");
            this.value = startingValue;
        }
        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            return value++; 
        }
    }
}
