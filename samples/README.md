# JaCh Samples

This directory contains sample code that can be used as references to start off with JaCh. The
programs are picked from
[Go by Example](https://gobyexample.com/channels) and corresponding code is converted to JaCh from
the go examples there.

## Running the samples:

You can clone the repo and run a `mvn clean install` inside the samples folder. That will generate
an uber jar in samples/target. That jar can be given as a classpath to run the corresponding sample
classes.

```shell
# To run the ChannelMake class

java -cp target/jach-samples-${version}.jar io.github.daichim.samples.ChannelMake
```

## Sample classes to run

The following sample classes are build into this repo:

1. [ChannelMake]() - Create a channel and send and receive data on it on two different routines
2. [ChannelBuffering]() - Create a buffered channel that can send multiple data before it is
   recieved in another routine.
3. [ChannelSync]() - Use channels to synchronize across routines.
4. [Select]() - Use Selector to select a case on the channel where data is available.
5. [Timeouts]() - Building a timeout logic using channels.
5. [Iterating over Channels]() - Iterate over all the messages that appear in the channel.
