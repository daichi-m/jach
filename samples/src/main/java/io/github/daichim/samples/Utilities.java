package io.github.daichim.samples;

import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

public class Utilities {

    @SneakyThrows
    public static void sleep(long millis) {
        TimeUnit.MILLISECONDS.sleep(millis);
    }

}
