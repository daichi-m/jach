---
title: Advanced Usage
permalink: /advanced/
---

<table style="width: 100%;">
  <tr>
    <td style="text-align: left;"><a href="/jach/selector">&#x25C0; Selector</a></td>
    <td style="text-align: center;"><a href="/jach/index">&#x1F3E0; Home </a></td>
    <td style="text-align: right;"><a href="/jach/examples">Examples &#x25BA;</a></td>
  </tr>
</table>


# Routines

Routines are concurrently executing threads which use channels to communicate and synchronize
between them. Semantically, there is no difference between a Java thread and a Routine in JaCh.
Routines are provided as syntactic sugars to bring JaCh syntax close to Golang.

There are 10 Routine interfaces defined from `Routine` to `Routine9` each of it accepting a
different number of arguments. Each of these is a functional interface, and can be defined by a
lambda expression. JaCh also provides overloaded `go` methods is the `JachChannels` class to mimic
Golang's `go` keyword. The `go` method launches the routine that is passed to it in an
`ExecutorService` which optionally can be provided by the user using `setGlobalExecutor`. It uses
the `ForkJoinPool.default()` as the default executor.

```java
import static io.github.daichim.jach.JachChannels.go;

class Routines {

    void nullaryRountine() {
        // A null-ary routine is exactly similar to a Runnable
        go(() -> {
            // logic of rountine
        });
    }

    void rountineWithArgs() {
        // There are multiple interfaces defined and 
        // overloaded go method that can take from
        // 0 to 9 arguments in its parameter.

        // E.g., a rountine with two String paramerter
        go((String a, String b) -> {
            // do stuff here
        }, "Hello", "world");

        // Another routine with four parameters of different types
        Routine4<String, Integer, Float, List<String>> routine =
            (String a, Integer b, Float c, List<String> d) -> {
                // some more awesome stuff
            };
        go(routine, "Hello", 42, 3.14f, Collections.emptyList());
    }

    void settingExecutor() {
        // The global executor can be set by the user
        ExecutorService ex = Executors.newCachedThreadPool();
        JachChannels.setGlobalExecutor(ex);

        // It is upto the user to shutdown the 
        // executor cleanly when the program exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ex.shutdownNow();
        }));
    }
}

```

# Copier

As discussed in the [concepts](/concepts/), one of the basic tenet of CSPs is:
> Do not communicate by sharing memory; instead, share memory by communicating.

In Java, all objects are actually references. That implies storing the object received in
the `write` call into the channel would actually store the reference to the object. This will mean
the memory is indeed shared between the threads and communication happens by sharing memory. This
will violate the basic principle of CSPs. To avoid this, JaCh relies on an implementation of
`Copier` to create a copy of the object received which can be stored in the channel.

JaCh uses [Kryo](https://github.com/EsotericSoftware/kryo) as a default Copier implementation. Kryo
is a fast and efficient binary object graph serialization framework, which can also be used
efficiently for deep copying Java object graphs. JaCh also exposes a `RefCopier` class, which
actually uses the reference itself (and thus does not really copies the memory), but it is not
recommended for use. It has been provided as a fast alternative for Immutable classes and can be
used in scenarios where speed is paramount.


<table style="width: 100%;">
  <tr>
    <td style="text-align: left;"><a href="/jach/selector">&#x25C0; Selector</a></td>
    <td style="text-align: center;"><a href="/jach/index">&#x1F3E0; Home </a></td>
    <td style="text-align: right;"><a href="/jach/examples">Examples &#x25BA;</a></td>
  </tr>
</table>