package io.github.daichim.jach.routines;

/**
 * {@link Routines} are interfaces that are equivalent of go-routines.
 */
public class Routines {


    public interface Routine0 {
        void run();
    }

    public interface Routine1<X> {
        void run(X x);
    }

    public interface Routine2<X1, X2> {
        void run(X1 x1, X2 x2);
    }

    public interface Routine3<X1, X2, X3> {
        void run(X1 x1, X2 x2, X3 x3);
    }

    public interface Routine4<X1, X2, X3, X4> {
        void run(X1 x1, X2 x2, X3 x3, X4 x4);
    }

    public interface Routine5<X1, X2, X3, X4, X5> {
        void run(X1 x1, X2 x2, X3 x3, X4 x4, X5 x5);
    }

    public interface Routine6<X1, X2, X3, X4, X5, X6> {
        void run(X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6);
    }

    public interface Routine7<X1, X2, X3, X4, X5, X6, X7> {
        void run(X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6, X7 x7);
    }

    public interface Routine8<X1, X2, X3, X4, X5, X6, X7, X8> {
        void run(X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6, X7 x7, X8 x8);
    }

    public interface Routine9<X1, X2, X3, X4, X5, X6, X7, X8, X9> {
        void run(X1 x1, X2 x2, X3 x3, X4 x4, X5 x5, X6 x6, X7 x7, X8 x8, X9 x9);
    }

}
