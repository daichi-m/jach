package io.github.daichim.jach.channel.copier;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RefCopierTest {

    @Test
    public void refCopyOfTest() throws Exception {
        String str = "Hello, World";
        Integer val = 69873;
        Long lval = 890025L;
        Double pi = 3.14159;
        TestPojo pojo = new TestPojo("Hello", 42);

        String strCopy = new RefCopier<String>().copyOf(str);
        Integer valCopy = new RefCopier<Integer>().copyOf(val);
        Long lvalCopy = new RefCopier<Long>().copyOf(lval);
        Double piCopy = new RefCopier<Double>().copyOf(pi);
        TestPojo pojoCopy = new RefCopier<TestPojo>().copyOf(pojo);

        Assert.assertTrue(str == strCopy);
        Assert.assertTrue(val == valCopy);
        Assert.assertTrue(lval == lvalCopy);
        Assert.assertTrue(pi == piCopy);
        Assert.assertTrue(pojo == pojoCopy);

    }

}