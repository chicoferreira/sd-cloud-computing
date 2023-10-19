package sd.cloudcomputing.worker;

import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        Worker worker = new Worker(Executors.newFixedThreadPool(10));
        worker.start(9900);
    }
}
