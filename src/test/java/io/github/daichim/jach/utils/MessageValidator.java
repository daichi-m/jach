package io.github.daichim.jach.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageValidator {

    private final AtomicInteger counter;
    private final Set<Integer> presenceSet;

    public MessageValidator() {
        counter = new AtomicInteger(0);
        presenceSet = Collections.synchronizedSet(new HashSet<>(5000, 0.75f));
    }

    public int newMessage() {
        return counter.incrementAndGet();
    }

    public boolean verify(int msg) {
        if (msg >= counter.get()) {
            return  false;
        }
        if (presenceSet.contains(msg)) {
            return false;
        }
        return presenceSet.add(msg);
    }



}
