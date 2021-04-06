package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.List;

public class TreadUnionImpl implements ThreadUnion {
    private static final String WORKER = "%s-worker-%s";
    private final List<Thread> threads = new ArrayList<>();
    private final List<FinishedThreadResult> results = new ArrayList<>();
    private final String name;
    private boolean shutdown;
    private int totalSize;

    public TreadUnionImpl(String name) {
        this.name = name;
    }

    @Override
    public int totalSize() {
        return totalSize;
    }

    @Override
    public int activeSize() {
        return (int) threads.stream()
                .filter(Thread::isAlive)
                .count();
    }

    @Override
    public void shutdown() {
        shutdown = true;
        threads.forEach(Thread::interrupt);
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void awaitTermination() {
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public boolean isFinished() {
        return shutdown && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return results;
    }

    @Override
    public Thread newThread(Runnable r) {
        if (isShutdown()) {
            throw new IllegalStateException("Shutdown is true!!!");
        }

        Thread thread = new Thread(r) {
            @Override
            public void run() {
                super.run();
                synchronized (results) {
                    if (notContainsTread(this)) {
                        results.add(new FinishedThreadResult(this.getName()));
                    }
                }
            }
        };

        thread.setName(String.format(WORKER, name, totalSize++));
        thread.setUncaughtExceptionHandler((t, e) -> {
            synchronized (results) {
                results.add(new FinishedThreadResult(t.getName(), e));
            }
        });

        threads.add(thread);
        return thread;
    }

    private boolean notContainsTread(Thread thread) {
        return results.stream()
                .noneMatch(finishedThreadResult -> finishedThreadResult.getThreadName().equals(thread.getName()));
    }
}