package io.github.daichim.jach.channel.copier;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.serializers.BeanSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class KryoCopierTest {

    @Test
    public void copyOfDefaultSerializerTest() throws Exception {
        String str = "Hello, World";
        Integer val = 69873;
        Long lval = 890025L;
        Double pi = 3.14159;
        TestPojo pojo = new TestPojo("Hello", 42);

        String strCopy = new KryoCopier<String>(String.class).copyOf(str);
        Integer valCopy = new KryoCopier<Integer>(Integer.class).copyOf(val);
        Long lvalCopy = new KryoCopier<Long>(Long.class).copyOf(lval);
        Double piCopy = new KryoCopier<Double>(Double.class).copyOf(pi);
        TestPojo pojoCopy = new KryoCopier<TestPojo>(TestPojo.class).copyOf(pojo);


        Assert.assertEquals(str, strCopy);
        Assert.assertEquals(val, valCopy);
        Assert.assertEquals(lval, lvalCopy);
        Assert.assertEquals(pi, piCopy);
        Assert.assertFalse(pojo == pojoCopy);
        Assert.assertEquals(pojo, pojoCopy);
    }

    @Test
    public void copyOfCustomSerializerTest() throws Exception {

        TestPojo pojo = new TestPojo("Hello", 42);
        Kryo kryo = new Kryo();
        kryo.register(TestPojo.class);
        TestPojo pojoCopy =
            new KryoCopier<TestPojo>(kryo, new BeanSerializer<>(kryo, TestPojo.class)).copyOf(pojo);
        Assert.assertFalse(pojo == pojoCopy);
        Assert.assertEquals(pojo, pojoCopy);
    }

}
