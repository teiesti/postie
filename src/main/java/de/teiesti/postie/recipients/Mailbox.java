package de.teiesti.postie.recipients;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A {@link Mailbox} is a {@link Recipient} that stores accepted {@link Letter}s until they where received. To use a
 * {@link Mailbox} register it to one or more {@link Postman} with {@link Postman#register(Recipient)}. A {@link
 * Postman} will put any received {@link Letter} into this {@link Mailbox} using {@link #accept(Object,
 * Postman)}. You can receive accepted letters with {@link #receive()}.

 * @param <Letter> type of the letters
 */
public class Mailbox<Letter> implements Recipient<Letter> {

    private final BlockingQueue<Letter> inbox = new LinkedBlockingQueue<>();

	/**
	 * Accepts {@link Letter}s and stores it in this {@link Mailbox} until they where received with {@link #receive()}.
	 *
	 * @param letter the {@link Letter}
	 * @param postman the {@link Postman} that delivered the {@link Letter} - not used
	 */
	@Override
	public void accept(Letter letter, Postman postman) {
       inbox.add(letter);
    }

	// TODO doc --> this method does nothing
	@Override
	public void acceptedLast(Postman from) { /*nothing to do*/ }

	/**
	 * Returns a {@link Letter} that was put into this {@link Mailbox} with {@link #accept(Object,
	 * Postman)}. A {@link Mailbox} works according to the FIFO principle: {@link #receive()} will return the {@link
	 * Letter} that was accepted the longest time ago. Receiving a {@link Letter} from a {@link Mailbox} will remove
	 * it. A {@link Letter} can only received once. If this {@link Mailbox} does not store a {@link Letter} when this
	 * method is called, it will block until a {@link Letter} was accepted or the blocking {@link Thread} was
	 * interrupted.
	 *
	 * @return the {@link Letter}
	 *
	 * @throws InterruptedException if a waiting {@link Thread} was interrupted
	 */
    public Letter receive() throws InterruptedException {
        return inbox.take();
    }

	/**
	 * Returns if this {@link Mailbox} has a {@link Letter} at the moment. This method returns {@code true} if and
	 * only if {@link #receive()} does not block. But please do not rely on this guarantee in a multi-threaded
	 * environment. Another thread may steal "your" {@link Letter}.
	 *
	 * @return if this {@link Mailbox} stores a {@link Letter}
	 */
    public boolean hasLetter() {
        return !inbox.isEmpty();
    }

}
