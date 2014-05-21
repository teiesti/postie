package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

/**
 * A {@link SequentialPostman} is a {@link Postman} that delivers received {@link Letter}s without starting a new
 * {@link Thread}. A {@link SequentialPostman} calls {@link Recipient#accept(Object, Postman)} within that
 * {@link Thread} that receives the {@link Letter}s from the {@link java.net.Socket}. Therefore no
 * {@link Recipient#accept(Object, Postman)} should block for long because it blocks all the other {@link Recipient}s
 * and {@link Letter}s. A {@link SequentialPostman} guarantees that the {@link Letter}s are delivered in the order
 * they have been received.
 *
 * @param <Letter> type of the letters
 */
public class SequentialPostman<Letter> extends Postman<Letter> {

	/**
	 * Delives the given {@link Letter} in a sequential way. This method does not start a new {@link Thread}.
	 *
	 * @param letter the {@link Letter} to deliver
	 *
	 * @return this {@link Postman}
	 */
    @Override
    protected Postman<Letter> deliver(Letter letter) {
        for (Recipient r : recipients)
            r.accept(letter, this);

        return this;
    }

    // TODO documentation
    protected Postman<Letter> reportLast() {
        for (Recipient r : recipients)
            r.acceptedLast(this);

        return this;
    }

}
