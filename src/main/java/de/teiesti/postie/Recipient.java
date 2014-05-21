package de.teiesti.postie;

/**
 * A {@link Recipient} accepts {@link Letter}s from one or more {@link Postman}.
 *
 * @param <Letter> type of the letters
 */
public interface Recipient<Letter> {

	/**
	 * Accepts a given {@link Letter} from a given {@link Postman}. This method does something useful with the given
	 * {@link Letter}.
	 *
	 * @param letter the {@link Letter}
	 * @param from the {@link Postman} that delivered the {@link Letter}
	 */
	public void accept(Letter letter, Postman from);

    // TODO documentation
    public void acceptedLast(Postman from);

}
