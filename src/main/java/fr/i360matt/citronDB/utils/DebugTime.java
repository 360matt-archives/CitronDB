package fr.i360matt.citronDB.utils;

public class DebugTime {
    private long start;

    public void start () {
        start = System.nanoTime();
    }

    public void printElapsed () {
        long stop = System.nanoTime();
        // deepcode ignore SystemPrintln: < for test only >
        System.out.println("Ptn: " + (stop-start));
    }

}
