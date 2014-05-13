package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;
import org.pmw.tinylog.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO doc: difference between ParallelPostman and SemiParallelPostman: the last sequentializes the letters!
public class SemiParallelPostman<Letter> extends Postman<Letter> {

    private ExecutorService es = Executors.newCachedThreadPool();
    private CountDownLatch cdl = new CountDownLatch(0);

    @Override
    protected Postman<Letter> deliver(Letter letter) {
        try {
            cdl.await();
        } catch (InterruptedException e) {
            Logger.error(e);
            System.exit(1);
        }

        cdl = new CountDownLatch(recipients.size());

        for (Recipient<Letter> r : recipients)
            es.submit(new Worker(r, letter, this, cdl));

        return this;
    }

    private class Worker implements Runnable {

        private Recipient<Letter> recipient;
        private Letter letter;
        private Postman postman;

        private CountDownLatch cdl;

        public Worker(Recipient<Letter> recipient, Letter letter, Postman postman, CountDownLatch cdl) {
            this.recipient = recipient;
            this.letter = letter;
            this.postman = postman;
            this.cdl = cdl;
        }

        @Override
        public void run() {
            recipient.accept(letter, postman);
            cdl.countDown();
        }
    }

}
