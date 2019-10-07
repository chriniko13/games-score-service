package com.xyz.platform.games.score.service.infra;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class Modifier {

    private static final Unsafe unsafe;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void changeLongField(Field fieldToUpdate, Object obj, long value) {
        try {
            final long offset = unsafe.objectFieldOffset(fieldToUpdate);
            unsafe.putLong(obj, offset, value);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
