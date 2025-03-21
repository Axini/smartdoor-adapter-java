package com.axini.smartdooradapter.generic;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// This class QThread is a more-or-less direct translation from the
// corresponding QThread class in the C++ adapter.

/**
 * Interface for processing items in the queue.
 * @param <T> the type of items to process
 */
interface ItemProcessor<T> {
    /**
     * Process a single item from the queue.
     * @param item the item to process
     */
    void processItem(T item);
}

/**
 * A thread that processes items from a queue.
 * @param <T> the type of items in the queue
 */
public class QThread<T> {

    private final Queue<T> queue;   // regular queue
    private final Thread qt;        // Thread that processes items from the queue
    private final Lock mutex;       // Lock to make the queue thread-safe
    private final Condition cond;   // Condition for notify/wait for the thread
    private final ItemProcessor<T> processor;   // processor to handle items from the queue

    /**
     * Creates a new QThread with the given item processor.
     * @param processor the processor to handle each item
     */
    public QThread(ItemProcessor<T> processor) {
        this.queue = new LinkedList<T>();
        this.mutex = new ReentrantLock();
        this.cond = mutex.newCondition();
        this.processor = processor;
        this.qt = new Thread(this::worker);

        // Start the thread
        this.qt.start();
    }

    /**
     * Adds an item to the queue.
     * @param item the item to add
     */
    public void add(T item) {
        mutex.lock();
        try {
            queue.add(item);
            cond.signal();
        } finally {
            mutex.unlock();
        }
    }

    // Clears all items from the queue.
    public void clear_queue() {
        mutex.lock();
        try {
            queue.clear();
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Waits for the worker thread to finish.
     * Due to the worker's while(true)-loop, this method never terminates!
     */
    public void join() {
        try {
            qt.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Removes and returns the next item from the queue.
     * Waits if the queue is empty.
     * @return the next item
     */
    private T pop() {
        mutex.lock();
        try {
            while (queue.isEmpty()) {
                try {
                    cond.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return queue.remove();
        } finally {
            mutex.unlock();
        }
    }

    // The worker method that continuously processes items from the queue.
    private void worker() {
        while (true) {
            T item = pop();
            processor.processItem(item);
        }
    }
}
