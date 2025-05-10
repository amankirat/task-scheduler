package com.example.taskscheduler;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.taskscheduler.core.TaskSchedulerImpl;
import com.example.taskscheduler.interfaces.Result;
import com.example.taskscheduler.interfaces.Task;
import com.example.taskscheduler.model.OperationType;
import com.example.taskscheduler.sample.SampleTask;

public class TaskSchedulerImplTest {

    private TaskSchedulerImpl scheduler;

    @BeforeEach
    public void setup() {
        scheduler = new TaskSchedulerImpl();
    }

    @Test
    public void testResultRetrievalByUUID() throws InterruptedException {
        SampleTask task = new SampleTask("gid1", OperationType.READ);
        scheduler.submitTask(task);
        Thread.sleep(200);
        Result result = scheduler.getResult(task.getUUID());
        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testConcurrentSubmissionDoesNotBlock() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int index = i;
            pool.submit(() -> {
                scheduler.submitTask(new SampleTask("gid" + index, OperationType.READ));
                latch.countDown();
            });
        }

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(completed);
    }

    @Test
    public void testSameGIDTasksAreSerialized() throws InterruptedException {
        String gid = "group1";
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger running = new AtomicInteger(0);
        AtomicBoolean overlapDetected = new AtomicBoolean(false);

        Task t1 = new SampleTask(gid, OperationType.READ) {
            @Override
            public Result execute() {
                if (running.getAndIncrement() > 0) {
                    overlapDetected.set(true);
                }
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                running.decrementAndGet();
                latch.countDown();
                return super.execute();
            }
        };

        Task t2 = new SampleTask(gid, OperationType.READ) {
            @Override
            public Result execute() {
                if (running.getAndIncrement() > 0) {
                    overlapDetected.set(true);
                }
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                running.decrementAndGet();
                latch.countDown();
                return super.execute();
            }
        };

        scheduler.submitTask(t1);
        scheduler.submitTask(t2);

        latch.await();
        assertFalse(overlapDetected.get());
    }

    @Test
    public void testReadWriteExclusivity() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean overlap = new AtomicBoolean(false);
        AtomicInteger counter = new AtomicInteger(0);

        Task read = new SampleTask("gid1", OperationType.READ) {
            @Override
            public Result execute() {
                if (counter.getAndIncrement() > 0) overlap.set(true);
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                counter.decrementAndGet();
                latch.countDown();
                return super.execute();
            }
        };

        Task write = new SampleTask("gid2", OperationType.WRITE) {
            @Override
            public Result execute() {
                if (counter.getAndIncrement() > 0) overlap.set(true);
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                counter.decrementAndGet();
                latch.countDown();
                return super.execute();
            }
        };

        scheduler.submitTask(read);
        scheduler.submitTask(write);
        latch.await();
        assertFalse(overlap.get());
    }

    @Test
    public void testMultipleReadsCanRunConcurrently() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger parallelReads = new AtomicInteger(0);
        AtomicBoolean ranConcurrently = new AtomicBoolean(false);

        Task r1 = new SampleTask("g1", OperationType.READ) {
            @Override
            public Result execute() {
                if (parallelReads.getAndIncrement() > 0) ranConcurrently.set(true);
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                parallelReads.decrementAndGet();
                latch.countDown();
                return super.execute();
            }
        };

        Task r2 = new SampleTask("g2", OperationType.READ) {
            @Override
            public Result execute() {
                if (parallelReads.getAndIncrement() > 0) ranConcurrently.set(true);
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                parallelReads.decrementAndGet();
                latch.countDown();
                return super.execute();
            }
        };

        scheduler.submitTask(r1);
        scheduler.submitTask(r2);
        latch.await();
        assertTrue(ranConcurrently.get());
    }

    @Test
    public void testFIFOOrderPreserved() throws InterruptedException {
        BlockingQueue<UUID> executionOrder = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            SampleTask task = new SampleTask("gid" + i, OperationType.READ) {
                @Override
                public Result execute() {
                    executionOrder.add(getUUID());
                    latch.countDown();
                    return super.execute();
                }
            };
            scheduler.submitTask(task);
        }

        latch.await();
        UUID first = executionOrder.poll();
        UUID second = executionOrder.poll();
        UUID third = executionOrder.poll();

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(third);
    }
}
