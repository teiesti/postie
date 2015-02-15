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

	// TODO comment
	public void noticeStart(Postman from);

	// TODO comment
	public void noticeStop(Postman from);

	/**
	 * FIXME delete
	 * Indicates that a given {@link Postman} delivered the last {@link Letter} to this {@link Recipient}. This
	 * method is called by a {@link Postman} that is stopping. If the connection was ck
	 *
	 *
	 * In case, the connection is closed by the opposite side,
	 * this method gives the last possibility to send a {@link Letter}. Otherwise this method can be use to perform a cleanup.
	 *
	 * @param from the {@link Postman} that is stopping
	 */
    //public void acceptedLast(Postman from);

}
