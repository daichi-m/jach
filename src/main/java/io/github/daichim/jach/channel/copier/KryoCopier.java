package io.github.daichim.jach.channel.copier;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.KryoException;
import com.esotericsoftware.kryo.kryo5.Serializer;
import io.github.daichim.jach.exception.CopyException;

/**
 * {@link KryoCopier} uses Kryo serializer to do a deep copy of the object. For more information
 * about Kryo, please check https://github.com/EsotericSoftware/kryo/blob/master/README.md. Kryo is
 * used as the default {@link Copier} method for JaCh.
 */
public class KryoCopier<T> implements Copier<T> {

    private final Kryo kryo;
    private final Serializer<T> serializer;

    /**
     * Initializes a {@link KryoCopier} with default instance of Kryo and Serializer.
     */
    public KryoCopier() {
        this.kryo = new Kryo();
        this.serializer = null;
        this.kryo.setRegistrationRequired(false);
    }

    /**
     * Initializes a {@link KryoCopier} using a client's provided instance of {@link Kryo} and
     * default instance of Serializer.
     */
    public KryoCopier(Kryo kryo) {
        this(kryo, null);
    }

    /**
     * Initializes a {@link KryoCopier} with the client's provided {@link Kryo} and {@link
     * Serializer} instances.
     */
    public KryoCopier(Kryo kryo, Serializer<T> serializer) {
        this.kryo = kryo;
        this.serializer = serializer;
    }

    /**
     * @see Copier#copyOf(Object)
     */
    @Override
    public T copyOf(T message) throws CopyException {
        T copy;
        try {
            if (serializer != null) {
                copy = kryo.copy(message, serializer);
            } else {
                copy = kryo.copy(message);
            }
            return copy;
        } catch (KryoException ex) {
            throw new CopyException("Error in deep copying object using Kryo", ex);
        }
    }
}
