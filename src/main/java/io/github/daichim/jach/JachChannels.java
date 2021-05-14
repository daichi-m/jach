package io.github.daichim.jach;

import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.UnbufferedChannel;
import io.github.daichim.jach.channel.copier.KryoCopier;
import io.github.daichim.jach.channel.copier.RefCopier;
import io.github.daichim.jach.channel.selector.ChannelAction;
import io.github.daichim.jach.channel.selector.Selector;
import io.github.daichim.jach.exception.TooManySelectorException;
import io.github.daichim.jach.routines.Routines;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * {@link JachChannels} is a utility class to access most of the common features without the
 * explicit need for calling complex constructors or methods.
 */
public class JachChannels {

    private static ExecutorService executor;

    static {
        executor = ForkJoinPool.commonPool();
    }

    /**
     * Create a {@link BufferedChannel} that stores strings with the given capacity. It uses {@link
     * RefCopier} to copy the messages to the channel.
     *
     * @param capacity The buffer capacity of the channel.
     *
     * @return A {@link BufferedChannel} of the given capacity to pass String messages.
     */
    public static BufferedChannel<String> makeStr(int capacity) {
        return new BufferedChannel<>(capacity, String.class, new RefCopier<>());
    }

    /**
     * Create a {@link UnbufferedChannel} that stores strings. It uses {@link RefCopier} to copy the
     * messages to the channel.
     *
     * @return A {@link UnbufferedChannel} to pass String messages.
     */
    public static UnbufferedChannel<String> makeStr() {
        return new UnbufferedChannel<>(String.class, new RefCopier<>());
    }

    /**
     * Create a {@link BufferedChannel} that stores integers with the given capacity. It uses {@link
     * RefCopier} to copy the messages to the channel.
     *
     * @param capacity The buffer capacity of the channel.
     *
     * @return A {@link BufferedChannel} of the given capacity to pass integer messages.
     */
    public static BufferedChannel<Integer> makeInt(int capacity) {
        return new BufferedChannel<>(capacity, Integer.class, new RefCopier<>());
    }

    /**
     * Create a {@link UnbufferedChannel} that stores integers. It uses {@link RefCopier} to copy
     * the messages to the channel.
     *
     * @return A {@link UnbufferedChannel} to pass integer messages.
     */
    public static UnbufferedChannel<Integer> makeInt() {
        return new UnbufferedChannel<>(Integer.class, new RefCopier<>());
    }

    /**
     * Create a generic {@link BufferedChannel} of the given capacity. It uses {@link KryoCopier} to
     * copy the messages to the channel.
     *
     * @param type     The class type of the messages that the channel will pass.
     * @param capacity The buffer capacity of the channel.
     *
     * @return A {@link BufferedChannel} of the given capacity to pass messages of given type.
     */
    public static <T> BufferedChannel<T> make(Class<T> type, int capacity) {
        return new BufferedChannel<>(capacity, type, new KryoCopier<>(type));
    }

    /**
     * Create a generic {@link UnbufferedChannel}. It uses {@link KryoCopier} to copy the messages
     * to the channel.
     *
     * @param type The class type of the messages that the channel will pass.
     *
     * @return A {@link UnbufferedChannel} to pass messages of given type.
     */
    public static <T> UnbufferedChannel<T> make(Class<T> type) {
        return new UnbufferedChannel<>(type);
    }

    /**
     * Create a {@link ChannelAction} for the given {@link Channel} with a given {@link Consumer}
     * action that is going to be executed for each message in the {@link Channel}.
     *
     * @param channel The {@link Channel} from which the message is read in.
     * @param action  The {@link Consumer} that will be executed for each message in the channel.
     * @param <T>     The type of the data that is stored in the {@link Channel} and will be passed
     *                on to the {@link Consumer}.
     *
     * @return The new {@link ChannelAction} object for this {@link Channel} and {@link Consumer}
     *     combination.
     */
    public static <T> ChannelAction<T> selectCase(Channel<T> channel, Consumer<T> action) {
        return new ChannelAction<>(channel, action);
    }

    /**
     * Convenience method to create a new {@link Selector} from the given set of {@link
     * ChannelAction}s.
     *
     * @param actions The list of {@link ChannelAction}s that this {@link Selector} would select
     *                from.
     *
     * @return A new {@link Selector} object.
     */
    public static Selector selector(ChannelAction... actions)
        throws TooManySelectorException, NullPointerException {
        return Selector.of(actions);
    }

    /**
     * Set the {@link ExecutorService} which will be used to run routines for JaCh
     *
     * @param executor The {@link ExecutorService} to use for running routines in JaCh.
     */
    public static void setGlobalExecutor(ExecutorService executor) {
        JachChannels.executor = executor;
    }

    /**
     * Run a routine on JaCh's executor.
     */
    public static void go(Routines.Routine0 routine) {
        executor.execute(routine::run);
    }

    /**
     * Run a routine on JaCh's executor.
     */
    public static <X> void go(Routines.Routine1<X> routine, X x) {
        executor.execute(() -> {
            routine.run(x);
        });
    }

    public static <X1, X2> void go(Routines.Routine2<X1, X2> routine, X1 x1, X2 x2) {
        executor.execute(() -> {
            routine.run(x1, x2);
        });
    }

    public static <X1, X2, X3> void go(Routines.Routine3<X1, X2, X3> routine,
                                       X1 x1, X2 x2, X3 x3) {
        executor.execute(() -> {
            routine.run(x1, x2, x3);
        });
    }

    public static <X1, X2, X3, X4> void go(Routines.Routine4<X1, X2, X3, X4> routine,
                                           X1 x1, X2 x2, X3 x3, X4 x4) {
        executor.execute(() -> {
            routine.run(x1, x2, x3, x4);
        });
    }


    public static <X1, X2, X3, X4, X5> void go(Routines.Routine5<X1, X2, X3, X4, X5> routine,
                                               X1 x1, X2 x2, X3 x3, X4 x4, X5 x5) {
        executor.execute(() -> {
            routine.run(x1, x2, x3, x4, x5);
        });
    }

    public static <X1, X2, X3, X4, X5, X6> void go(
        Routines.Routine6<X1, X2, X3, X4, X5, X6> routine,
        X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6) {
        executor.execute(() -> {
            routine.run(x1, x2, x3, x4, x5, x6);
        });
    }

    public static <X1, X2, X3, X4, X5, X6, X7> void go(
        Routines.Routine7<X1, X2, X3, X4, X5, X6, X7> routine,
        X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6, X7 x7) {
        executor.execute(() -> {
            routine.run(x1, x2, x3, x4, x5, x6, x7);
        });
    }

    public static <X1, X2, X3, X4, X5, X6, X7, X8> void go(
        Routines.Routine8<X1, X2, X3, X4, X5, X6, X7, X8> routine,
        X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6, X7 x7, X8 x8) {
        executor.execute(() -> {
            routine.run(x1, x2, x3, x4, x5, x6, x7, x8);
        });
    }

    public static <X1, X2, X3, X4, X5, X6, X7, X8, X9> void go(
        Routines.Routine9<X1, X2, X3, X4, X5, X6, X7, X8, X9> routine,
        X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6, X7 x7, X8 x8, X9 x9) {
        executor.execute(() -> {
            routine.run(x1, x2, x3, x4, x5, x6, x7, x8, x9);
        });
    }
}
