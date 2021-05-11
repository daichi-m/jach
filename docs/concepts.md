---
title: Concepts of CSP and Channels 
permalink: /concepts/
---

## Brief glossary of terms

#### Communicating Sequential Processes (CSPs)

Communicating sequential processes are a set of concurrent processes or threads that are allowed to synchronize with
each other by synchronizing their I/O. CSP's form the basis of Golang's go-routines and channels. You can read more
about CSPs in [Wikipedia](https://en.wikipedia.org/wiki/Communicating_sequential_processes).

A fundamental tenet of CSP's which is often repeated is:
> Do not communicate by sharing memory; instead, share memory by communicating.

This has interesting impact on the design of JaCh, as we'll see in later phases.

#### Channels

Channels are a communication structure which can be written to or read from. A channel behaves like a conduit in which
messages can flow from one end to another. A channel can be

- Buffered, which can hold a fixed number of messages before further writes are blocked until something is read in.
- Unbuffered, in which only each message needs to be read in before any further write can happen.

#### Rountines

Routines are the concurrent processes that synchronize with each other using channels as a communication mechanism.

