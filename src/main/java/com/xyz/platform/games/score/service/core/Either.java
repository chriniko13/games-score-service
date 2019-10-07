package com.xyz.platform.games.score.service.core;

import java.util.function.Function;

// Note: Effect Type (reify side effect to data structure)
public class Either<A, B> {

    private A left;
    private B right;

    private Either(A a, B b) {
        left = a;
        right = b;
    }

    public A left() {
        return left;
    }

    public B right() {
        return right;
    }

    public boolean isLeft() {
        return left != null;
    }

    public boolean isRight() {
        return right != null;
    }

    // Note: map method is right based.
    public <C> Either<A, C> map(Function<B, C> f) {
        if (isRight()) {
            return Either.right(f.apply(right));
        }
        return Either.left(left);

    }

    public <C> Either<C, B> onLeftExecute(Function<A, C> f) {
        if (isLeft()) {
            return Either.left(f.apply(left));
        }
        return Either.right(right);
    }

    public static <A, B> Either<A, B> left(A a) {
        return new Either<>(a, null);
    }

    public static <A, B> Either<A, B> right(B b) {
        return new Either<>(null, b);
    }

}