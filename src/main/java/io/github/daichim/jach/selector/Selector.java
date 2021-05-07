package io.github.daichim.jach.selector;

import io.github.daichim.jach.Channel;
import io.github.daichim.jach.UnbufferedChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Selector {

    private final Object syncObject;
    private final Channel<String> selectorChannel;
    private Map<String, ChannelAction> channelActions;

    public Selector(ChannelAction... actions) {
        this.channelActions = Collections.synchronizedMap(new HashMap<>());
        this.channelActions.putAll(Arrays.stream(actions)
            .collect(Collectors.toMap(ca -> ca.getChannel().toString(), Function.identity())));
        this.syncObject = new Object();
        this.selectorChannel = new UnbufferedChannel<>(String.class);
    }

    public static Selector selector(ChannelAction... actions) {
        Selector selector = new Selector(actions);
        return selector;
    }

    public void select() {
        String chan = this.selectorChannel.read();
        ChannelAction chanAct = this.channelActions.get(chan);
        if (chanAct == null) {
            select();
        }
        synchronized (syncObject) {
            Object obj = chanAct.getChannel().read();
            if (obj == null) {
                throw new NullPointerException("Could not read object");
            }
            chanAct.action.accept(obj);
        }
    }


}
