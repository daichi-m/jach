package io.github.daichim.jach.internal;

/**
 * SerDe provides a serialization-deserialization mechanism which is used to store the object in the
 * {@link io.github.daichim.jach.Channel}.
 */
public class SerDe {

    public static <T> T identity(T msg) {
        return msg;
    }


}
