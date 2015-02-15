package de.teiesti.postie.recipients;

import de.teiesti.postie.Postman;
import de.teiesti.postie.Recipient;

/**
 * A {@link SimpleRecipient} is a {@link Recipient} but without the requirement to implement
 * {@link #noticeStart(Postman)} and {@link #noticeStop(Postman)}. If not overwritten, both methods will do nothing.
 *
 * @param <Letter> type of the letters
 */
public abstract class SimpleRecipient<Letter> implements Recipient<Letter> {

	/**
	 * Does nothing.
	 *
	 * @param from the {@link Postman} that is starting - not used
	 */
	@Override
	public void noticeStart(Postman from) {
		/* nothing to do */
	}

	/**
	 * Does nothing.
	 *
	 * @param from the {@link Postman} that is stopping - not used
	 */
	@Override
	public void noticeStop(Postman from) {
		/* nothing to do */
	}

}
