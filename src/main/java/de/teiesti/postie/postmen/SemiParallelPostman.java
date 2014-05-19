package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;
import org.pmw.tinylog.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO doc: difference between ParallelPostman and SemiParallelPostman: the last sequentializes the letters!

/**
 * A {@link SemiParallelPostman} is a {@link Postman} that delivers {@link Letter}s in parallel but with care for the
 * {@link Letter} order. This means that a {@link SemiParallelPostman} - in contrast to a {@link ParallelPostman} -
 * waits until any {@link Recipient#accept} has return before the next {@link Letter} is processed. A
 * {@link SemiParallelPostman} creates a {@link Runnable} for each pair of received {@link Letter} and registered
 * {@link Recipient}. The {@link Runnable}s are submitted to an {@link ExecutorService}.
 *
 * @param <Letter> type of the letters
 */
public class SemiParallelPostman<Letter> extends Postman<Letter> {

    private ExecutorService es = Executors.newCachedThreadPool();
    private CountDownLatch cdl = new CountDownLatch(0);

	/**
	 * Delivers a given {@link Letter} in parallel using an {@link ExecutorService}. This method creates a {@link
	 * Runnable} for each {@link Recipient} and submits it to the {@link ExecutorService}. This method blocks until
	 * the {@link Letter} from the the preceding call is completely executed. This guarantes the {@link Letter} order
	 * is not mixed up.
	 *
	 * @param letter the {@link Letter} to deliver
	 *
	 * @return this {@link Postman}
	 */
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
