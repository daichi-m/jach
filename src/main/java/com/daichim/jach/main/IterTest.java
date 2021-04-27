package com.daichim.jach.main;

import com.daichim.jach.exception.ClosedChannelException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IterTest {

    final int reader_count = 3;
    final MyDataStructure dataStructure = new MyDataStructure();
    final CountDownLatch latch = new CountDownLatch(reader_count);
    final ExecutorService svc = Executors.newCachedThreadPool();

    public void writer() throws Exception {
        for (int i=0; i<50; i++) {
            dataStructure.write(i);
        }
    }

    public void readers() throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);
        for (int i=0; i<reader_count; i++) {

            Runnable r = () -> {
                try {
                    long id = Thread.currentThread().getId();
                    System.out.println("Thread started: " + id);
                    for (int x : dataStructure) {
                        System.out.printf("Thread id: %d, value: %d \n", id, x);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                } finally {
                    latch.countDown();
                }
            };
            svc.submit(r);
        }
    }

    public static void main(String[] args) throws Exception {
        IterTest test = new IterTest();
        test.readers();
        test.writer();

        test.svc.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) { }
            test.dataStructure.close();
        });

        test.latch.await();
        test.svc.shutdown();


    }

}

class MyDataStructure implements Iterable<Integer> {
    BlockingQueue<Integer> queue;
    volatile boolean close = false;

    public MyDataStructure() {
        this.queue = new ArrayBlockingQueue<>(20);
    }

    public void write(int val) throws Exception {
        if (close) {
            throw new ClosedChannelException();
        }
        boolean succ = this.queue.offer(val, 100, TimeUnit.MILLISECONDS);
        if (!succ) {
            this.write(val);
        }
    }

    public int read() throws Exception {
        if (close) {
            throw new ClosedChannelException();
        }
        Integer x = this.queue.poll(100, TimeUnit.MILLISECONDS);
        if (x == null) {
            return read();
        }
        return x;
    }

    public void close() {
        this.close = true;
    }

    public Iterator<Integer> iterator() {
        return new MyDSIter(this);
    }
}

class MyDSIter implements Iterator<Integer> {

    MyDataStructure parent;

    public MyDSIter(MyDataStructure ds) {
        this.parent = ds;
    }

    @Override
    public boolean hasNext() {
        return !parent.close;
    }

    @Override
    public Integer next() {
        try {
            return parent.read();
        } catch (ClosedChannelException ex) {
            System.err.println("No Such Element due to ClosedChannelException");
            throw new NoSuchElementException();
        } catch (Exception ex) {
            throw new IllegalStateException();
        }
    }
}
