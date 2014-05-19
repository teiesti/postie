package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A {@link ParallelPostman} is a {@link Postman} that delivers {@link Letter}s in parallel using an
 * {@link ExecutorService}. For each pair of received {@link Letter} and registered {@link Recipient} a {@link Runnable}
 * is created and submitted to the {@link ExecutorService}. Therefore, no guarantees concerning the {@link Letter}
 * order where made.
 *
 * @param <Letter> type of the letters
 */
public class ParallelPostman<Letter> extends Postman<Letter> {

    private ExecutorService es = Executors.newCachedThreadPool();

	/**
	 * Delivers a given {@link Letter} in parallel using an {@link ExecutorService}. This method creates a {@link
	 * Runnable} for each {@link Recipient} and submits it to the {@link ExecutorService}.
	 *
	 * @param letter the {@link Letter} to deliver
	 *
	 * @return this {@link Postman}
	 */
    @Override
    protected Postman<Letter> deliver(Letter letter) {
        for (Recipient<Letter> r : recipients)
            es.submit(new Worker(r, letter, this));

        return this;
    }

    private class Worker implements Runnable {

        private Recipient<Letter> recipient;
        private Letter letter;
        private Postman<Letter> postman;

        public Worker(Recipient<Letter> recipient, Letter letter, Postman<Letter> postman) {
            this.recipient = recipient;
            this.letter = letter;
            this.postman = postman;
        }

        @Override
        public void run() {
            recipient.accept(letter, postman);
        }
    }

}
