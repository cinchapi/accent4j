package com.cinchapi.common.base;


import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ArrayBuilderTest
{
    private ArrayBuilder<Object> arrayBuilder;


    @Before
    public void TestArrayBuilder()
    {
        arrayBuilder = ArrayBuilder.builder();

        Assert.assertNotNull( arrayBuilder );
    }


    @Test
    public void TestAddToArray()
    {
        int size = 100;

        for ( int i = 0; i < size; i++ )
        {
            arrayBuilder.add( UUID.randomUUID().toString() );
        }

        int length = arrayBuilder.build().length;

        Assert.assertEquals( length, size );
    }

    @Test(expected = NegativeArraySizeException.class)
    public void TestNegativeArraySizeException(){

        int size = 1000;

        for ( int i = 0; i < size; i++ )
        {
            arrayBuilder.add( UUID.randomUUID().toString() );
        }

        arrayBuilder.build();
    }
}
