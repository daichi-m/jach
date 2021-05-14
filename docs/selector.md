---
title: Selector
permalink: /selector/
---

<table style="width: 100%;">
  <tr>
    <td style="text-align: left;"><a href="/channels">&#x25C0; Channels</a></td>
    <td style="text-align: center;"><a href="/index">&#x1F3E0; Home </a></td>
    <td style="text-align: right;"><a href="/advanced">Advanced &#x25BA;</a></td>
  </tr>
</table>


# Channel selector

## Select

Selectors provide an option for the user to wait on multiple channels and take appropriate action
based on which channel the message appears. The thread executing the select blocks until there is a
message that appears in one of the channels and executes the appropriate action for that message and
returns.

```java
class Selector {
    <T1, T2> void selectorTest(Channel<T1> chan1,
                               Channel<T2> chan2) {
        Selector selector = selector(selectCase(
            chan1, t1 -> {
                // do something
            }),
            selectCase(chan2, t2 -> {
                // do something else
            })
        );
        selector.select();
    }
}
```

## Until Done

Selector also expose a `untilDone` method, calls select in a loop, waiting for a message on any of
the channel and then taking the corresponding action. This loop continues executing until all the
channels have been closed and completely read. Once the call returns from this method the `Selector`
object is closed and cannot be further used for any other call.

There are two special action that are defined in the`Selector` class:
`Selector.BREAK_ACTION` and `Selector.CONTINUE_ACTION`. If a channel is associated with the break
action, and a message appears on that channel, the `untilDone` loop is broken, and the method
returns to the caller. The continue action acts like a pass statement, it basically means take no
action if a message appears on that channel and wait for the next message on some other channel.

```java
class Selector {
    <T1, T2> void untilDoneTest(Channel<T1> chan1, Channel<T2> chan2,
                                Channel<String> exitChan) {
        Selector selector = selector(selectCase(
            chan1, t1 -> {
                // do something
            }),
            selectCase(chan2, t2 -> {
                // do something else
            }),
            selectCase(exitChan, Selector.BREAK_ACTION)
        );
        // This method will keep on executing the corresponding 
        // actions as message keeps on appearing in chan1 and chan2. 
        // On a message in exitChan, it executes the BREAK_ACTION
        // which will break the loop and return to the caller.
        selector.untilDone();
    }
}
```

## Default Action

Sometimes, there is a need to execute a default action when there is no message in any of the
channels. To achieve this, Selector exposes `untilOrDefault` method. It works exactly like the
`untilDone` method, but additionally takes a null-ary action which is executed when there is no
message in any of the channels.

```java
class Selector {
    <T1, T2> void untilDoneTest(Channel<T1> chan1, Channel<T2> chan2,
                                Channel<String> exitChan) {
        Selector selector = selector(selectCase(
            chan1, t1 -> {
                // do something
            }),
            selectCase(chan2, t2 -> {
                // do something else
            }),
            selectCase(exitChan, Selector.BREAK_ACTION)
        );
        // This method will keep on executing the corresponding actions 
        // as message keeps on appearing in chan1 and chan2. 
        // On a message in exitChan, it executes the BREAK_ACTION which will 
        // break the loop and return to the caller.
        selector.untilOrDefault(() -> {
            // do default action handling
        });
    }
}
```


<table style="width: 100%;">
  <tr>
    <td style="text-align: left;"><a href="/channels">&#x25C0; Channels</a></td>
    <td style="text-align: center;"><a href="/index">&#x1F3E0; Home </a></td>
    <td style="text-align: right;"><a href="/advanced">Advanced &#x25BA;</a></td>
  </tr>
</table>