package wtf.listenia.citronDB.utils;

public class DebugTime {
    private long start;

    public void start () {
        start = System.nanoTime();
    }

    public void printElapsed () {
        long stop = System.nanoTime();
        System.out.println("Ptn: " + (stop-start));
    }

}
