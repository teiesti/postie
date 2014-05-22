package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

/**
 * A {@link ParallelPostman} is a {@link Postman} that delivers {@link Letter}s in parallel using an {@link
 * ExecutorService}. For each pair of received {@link Letter} and registered {@link Recipient} a {@link Runnable}
 * is created and submitted to the {@link ExecutorService}. By default, this {@link ParallelPostman} observes the
 * {@link Letter} order: Before the next {@link Letter} is processed a {@link ParallelPostman} waits until any
 * {@link Recipient#accept} has return. This behaviour can be disabled with {@link #observeLetterOrder}. It is
 * possible to specify the {@link ExecutorService} with {@link #setExecutorService(ExecutorService)}.
 *
 * @param <Letter> type of the letters
 */
public class ParallelPostman<Letter> extends Postman<Letter> {

    private ExecutorService es = Executors.newCachedThreadPool();
	private Phaser phaser = new Phaser(1);
	private boolean observeLetterOrder = true;

	/**
	 * Controls weather this {@link ParallelPostman} should observe the {@link Letter} order. If this option is
	 * enabled, this {@link ParallelPostman} will not submit jobs for the next {@link Letter} before the current
	 * {@link Letter} was fully processed. This guarantees that no {@link Letter} overtake another.<br>
	 * <br>
	 * It is possible to change this option at any time but jobs that have already been submitted will not be revoked.
	 * Therefore switching from the unobserved mode to the observed mode may needs some time to take effect. This
	 * method does only guarantee that {@link Letter}s that have not been yet submitted by the opposite side will be
	 * processed in the correct order. Anything else happens by chance. Switching from observed to unobserved mode
	 * will take effect immediately.
	 *
	 * @param observeLetterOrder weather the {@link Letter} order should be observed
	 *
	 * @return this {@link Postman}
	 */
	public Postman<Letter> observeLetterOrder(boolean observeLetterOrder) {
		this.observeLetterOrder = observeLetterOrder;
		return this;
	}

	/**
	 * Sets the {@link ExecutorService} this {@link Postman} uses. If this method was never called, an
	 * {@link Executors#newCachedThreadPool()} is used by default.
	 *
	 * @param executorService the {@link ExecutorService} to use
	 *
	 * @return this {@link Postman}
	 *
	 * @throws java.lang.IllegalArgumentException if the {@link ExecutorService} is {@code null}
	 */
	public Postman<Letter> setExecutorService(ExecutorService executorService) {
		if (executorService == null)
			throw new IllegalArgumentException("executorService == null");

		this.es = executorService;

		return this;
	}

	/**
	 * Delivers a given {@link Letter} in parallel using an {@link ExecutorService}. This method creates a {@link
	 * Runnable} for each {@link Recipient} and submits it to the {@link ExecutorService}. If this {@link
	 * ParallelPostman} should observe the letter order, this method blocks until any {@link Letter} from the the
	 * preceding call is completely processed which guarantees that the order is not mixed up.
	 *
	 * @param letter the {@link Letter} to deliver
	 *
	 * @return this {@link Postman}
	 */
    @Override
    protected Postman<Letter> deliver(Letter letter) {
		if(observeLetterOrder) phaser.arriveAndAwaitAdvance();

        for (Recipient<Letter> r : recipients) {
			phaser.register();
			es.submit(new Deliverer(r, letter, this));
		}

        return this;
    }

	/**
	 * Reports to any {@link Recipient} that the last {@link Letter} was delivered in parallel. This method creates a
	 * {@link Runnable} for each {@link Recipient} and submits it to the {@link ExecutorService}. Before it waits until
	 * any {@link Letter} was processed for any {@link Recipient}. This method does not return before the last
	 * {@link Recipient#acceptedLast(Postman)} has returned.
	 *
	 * @return this {@link Postman}
	 */
	@Override
    protected Postman<Letter> reportLast() {
        phaser.arriveAndAwaitAdvance();

		for (Recipient<Letter> r : recipients) {
			phaser.register();
			es.submit(new LastReporter(r, this));
		}

		phaser.arriveAndAwaitAdvance();

		return this;
    }

    private class Deliverer implements Runnable {

        private Recipient<Letter> recipient;
        private Letter letter;
        private Postman postman;

        public Deliverer(Recipient<Letter> recipient, Letter letter, Postman postman) {
            this.recipient = recipient;
            this.letter = letter;
            this.postman = postman;
        }

        @Override
        public void run() {
			recipient.accept(letter, postman);
            phaser.arriveAndDeregister();
        }

    }

	private class LastReporter implements Runnable {

		private Recipient<Letter> recipient;
		private Postman postman;

		public LastReporter(Recipient<Letter> recipient, Postman postman) {
			this.recipient = recipient;
			this.postman = postman;
		}

		@Override
		public void run() {
			recipient.acceptedLast(postman);
			phaser.arriveAndDeregister();
		}

	}

}
