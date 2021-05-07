package io.github.daichim.jach.selector;

import com.google.common.annotations.VisibleForTesting;
import io.github.daichim.jach.BufferedChannel;
import io.github.daichim.jach.Channel;

import java.util.HashMap;
import java.util.Map;

public class Selector {

    private final static int CHAN_SIZE = 2048;
    private Map<String, ChannelAction> channelActions;
    private Channel<String> selectorChannel;

    @VisibleForTesting
    Selector() {
    }

    public static Selector select(ChannelAction... actions) {
        Selector selector = new Selector();
        selector.channelActions = new HashMap<>(actions.length);
        selector.selectorChannel = new BufferedChannel<>(CHAN_SIZE, String.class);
        for (ChannelAction ca : actions) {
            selector.channelActions.put(ca.getChannel().getId(), ca);
        }


        return selector;
    }

}
