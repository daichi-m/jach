---
title: Channels
permalink: /channels/
---

<table style="width: 100%;">
  <tr>
    <td style="text-align: left;"><a href="/jach/installing">&#x25C0; Installing</a></td>
    <td style="text-align: center;"><a href="/jach/index">&#x1F3E0; Home </a></td>
    <td style="text-align: right;"><a href="/jach/selector">Selector &#x25BA;</a></td>
  </tr>
</table>


# Channels

Channels are the core structure in JaCh. A channel act as a pipe or conduit that can pass messages
from one thread to another. You can send values to the channel from one thread and receive them in
another. Channels guarantee the following:

1. Each message sent in the channel will be read by only one process.
2. Each message is read only once by any of the reading processes.

Channels can be of two types, Buffered and Unbuffered. A buffered channel has a backing storage
which can store messages to a capacity and then subsequently blocks the writes. An unbuffered
channel does not have any backing store, each message must be read before the next write can happen.

## Unbuffered Channels

In case of unbuffered channels, there is no backing store, hence each message has to be read by some
process before the next message can be written. Due to this it has an additional guarantee that the
messages are always delivered in order.

Unbuffered channels can be created using `JachChannels.make` family of methods. It is just a
syntactic sugar on top of the constructor to provide a Golang-like syntax. It can also be created by
directly using the `UnbufferedChannel` constructor.

```java
import io.github.daichim.jach.channel.UnbufferedChannel;
import io.github.daichim.jach.channel.copier.RefCopier;

import static io.github.daichim.jach.JachChannels.make;
import static io.github.daichim.jach.JachChannels.makeInt;
import static io.github.daichim.jach.JachChannels.makeStr;

class ChannelDemo {

    void create() {
        // Create a Channel for a custom POJOClass
        Channel<POJOClass> pojoChannel = make(POJOClass.class);

        // Create a Channel for Strings
        Channel<String> strChannel = make(String.class);
        // Or you can use makeStr method too
        Channel<String> strChannel2 = makeStr();

        // Create a Channel for integers
        Channel<Integer> intChannel = make(Integer.class);
        // Similar to makeStr, there is a makeInt too
        Channel<Integer> intChannel2 = makeInt();

        // This also works fine. We will discuss about Copier later.
        Channel<String> strChan =
            new UnbufferedChannel<>(String.class, new RefCopier<>());
    }
}
```

## Buffered Channels

In a buffered channel there is a fixed capacity buffer that backs the channel. This has certain
important consequences:

* Writes can continue without a corresponding read until the buffer is full.
* Each message is guaranteed to be read by only one thread.
* The order of the message is not guaranteed. Messages can be read out of order in the threads.

Buffered channels can also be created using the `JachChannels.make` family of methods, or users can
directly call the `BufferedChannel` constructor as well.

```java
import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.channel.copier.RefCopier;

import static io.github.daichim.jach.JachChannels.make;
import static io.github.daichim.jach.JachChannels.makeInt;
import static io.github.daichim.jach.JachChannels.makeStr;

class ChannelDemo {

    void create() {
        // Create a Channel for a custom POJOClass that will block after 100 writes
        Channel<POJOClass> pojoChannel = make(POJOClass.class, 100);

        // Create a Channel for Strings the will block after 100 writes.
        Channel<String> strChannel = make(String.class, 100);
        // Or you can use makeStr method too
        Channel<String> strChannel2 = makeStr(100);

        // Create a Channel for integers that will block after 100 writes.
        Channel<Integer> intChannel = make(Integer.class, 100);
        // Similar to makeStr, there is a makeInt too
        Channel<Integer> intChannel2 = makeInt(100);

        // This is also a valid way to create a BufferedChannel
        Channel<String> stringChannel =
            new BufferedChannel<>(100, String.class, new RefCopier<>());
    }
}
```

## Channel operations

The basic operations a channel supports are `read`, `write` and `close`. There are overriden forms
of read and write, that are non-blocking in nature - but their use should be considered only as a
last resort. Using non-blocking channels defeats the very purpose of using channels - synchronize
through communication.

### Read Operations

```java
class ChannelRead {
    <T> void read(Channel<T> channel) {
        // Reads the next message from channel, blocks until available.
        T msg = channel.read();

        // Reads the next message or times out
        T msg = channel.read(100, TimeUnit.MILLISECONDS);

        // Reads the next message or returns null. 
        // This method returns immediately.
        T msg = channel.tryRead();
    }
}
```

### Write Operations

```java
class ChannelWrite {
    <T> void write(Channel<T> channel, T msg) {
        // Writes the message to channel, blocks if the channel is full.
        channel.write(msg);

        // Writes the message to channel or times out
        channel.write(msg, 100, TimeUnit.MILLISECONDS);

        // Try writing the message. This method returns immediately. 
        // It returns false is write fails
        boolean success = channel.tryWrite(msg);
    }
}
```

### Channel close

The `Channel` interface implements `AutoCloseable` so it can either be closed explicitly, or you can
use try-with-resources to automatically close it once the try-block exits.

```java
class ChannelClose {
    void awesomeWork() {
        Channel<String> channel = makeStr();
        try {
            // do awesome stuff
        } finally {
            channel.close();
        }
    }

    // Or this is also a valid way to use
    void someMoreAwesome() {
        try (Channel<String> channel = makeStr()) {
            // do some more awesome stuff
        }
        // Channel will be closed once try block completes.
    }
}

```

Once a channel is closed, it cannot be written to. Writes to a closed channel fails with
a `ClosedChannelException`. Reads from a closed channel will succeed as long as there are messages
in the channel buffer. Once the buffer is empty, subsequent reads will throw
a `NoSuchChannelElementException`.

## Iterating over a Channel

`Channel` implements the Iterable interface. The Channel.getIterator() method will return the
Iterator instance for this channel which can then be used to iterate over the elements of the
channel. Since channel is a blocking structure, the channel iterator has following properties:

1. The channel iterator is a blocking iterator - which means if there is no message in the channel,
   it will block on the `next()` call until an element in available.
2. Since the channel can be closed at any given point of time, and there is no way for the iterator
   to know about that, the iterator will throw `NoSuchChannelElementException`
   in `next()` if the channel is closed in another thread.
3. Channel iterators cannot be split. Using a channel in parallel streams will throw
   `UnsupporteOpertaionException`

Channel iterators can be used like any other iterator - either in a while loop or through a for-each
loop. One caveat in using the iterator in a for-each loop is that the loop will never exit cleanly
and can only exit through a `NoSuchChannelElementException`
(see point 2 above). Hence using the channel in a for-each loop needs to be guarded by a try-catch

```java
class Iteration {
    void iterate(Channel<T> chan) {
        try {
            for (T msg : chan) {
                doWork(msg);
            }
        } catch (NoSuchChannelElementException ignored) {
            // The loop has ended here
        }
    }
}
```


<table style="width: 100%;">
  <tr>
    <td style="text-align: left;"><a href="/jach/installing">&#x25C0; Installing</a></td>
    <td style="text-align: center;"><a href="/jach/index">&#x1F3E0; Home </a></td>
    <td style="text-align: right;"><a href="/jach/selector">Selector &#x25BA;</a></td>
  </tr>
</table>