Feature: Read and write from channel
  Use n readers and m writers all of which are in separate threads to write to channel and read from it.
  Ensure a message written is read by only one reader.

  Background:
    Given A channel of size <cap>
    And   <wrt> writers
    And   <rd> readers

  Scenario Template:
    When <msg> messages are written
    Then <msg> messages are read
    And  Each message is read only once

    Examples:
      |  cap  |  wrt |   rd | msg   |
      |  20   |  1   |  1   | 100   |
      |  20   |  10  |  10  | 100   |
      |  20   |  20  |  20  | 100   |