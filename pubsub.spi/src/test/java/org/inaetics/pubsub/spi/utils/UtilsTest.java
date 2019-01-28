package org.inaetics.pubsub.spi.utils;

import org.inaetics.pubsub.api.ann.TypeId;
import org.inaetics.pubsub.api.ann.TypeName;
import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    public UtilsTest(){}

    @Test
    public void testStringHash() {
        int hash = Utils.stringHash("abc");
        Assert.assertEquals(hash, 193485963);

    }


    @TypeName(name = "abc")
    class Msg1 {

    }

    @TypeId(id = 1234)
    class Msg2 {

    }

    public void testTypeIdForClass() {
        int hash = Utils.typeIdForClass(Msg1.class);
        Assert.assertEquals(hash, 193485963);

        hash = Utils.typeIdForClass(Msg2.class);
        Assert.assertEquals(hash, 1234);
    }



}