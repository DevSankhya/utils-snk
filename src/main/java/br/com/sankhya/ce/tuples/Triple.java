package br.com.sankhya.ce.tuples;

public class Triple<F, S, T> {
    private final F first;
    private final S second;
    private final T third;

    public Triple(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;

    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    public T getThird() {
        return third;
    }

    public static <L, M, R> Triple<L, M, R> of(L first, M second, R third) {
        return new Triple<>(first, second, third);
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + "," + third + ")";

    }


    public F component1() {
        return first;
    }

    public S component2() {
        return second;
    }
    public T component3() {
        return third;
    }

}