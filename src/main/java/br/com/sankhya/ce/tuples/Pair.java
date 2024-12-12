package br.com.sankhya.ce.tuples;

import java.io.Serializable;

public class Pair<F, S> implements Serializable {
    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }


    public String toString() {
        return "Pair(" + first + ", " + second + ")";
    }

    public static <F, S> Pair<F, S> of(F left, S right) {
        return new Pair<>(left, right);
    }

    public F getLeft() {
        return first;
    }

    public S getRight() {
        return second;
    }

    public F component1() {
        return first;
    }

    public S component2() {
        return second;
    }
}

