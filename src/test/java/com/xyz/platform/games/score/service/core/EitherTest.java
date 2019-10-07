package com.xyz.platform.games.score.service.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EitherTest {

    @Test
    public void map() {

        String result = Either.<String, Integer>right(1).map(x -> x.toString()).right();
        assertEquals("1", result);

    }

    @Test
    public void onLeftExecute() {

        Either<Integer, String> e = Either.<Integer, String>left(1).onLeftExecute(x -> x + 10);
        assertEquals(11, e.left().intValue());
    }
}