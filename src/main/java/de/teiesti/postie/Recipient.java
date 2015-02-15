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

	/**
	 * Indicates that a given {@link Postman} begins to deliver {@link Letter}s to this {@link Recipient}. This method
	 * is called by a {@link Postman} directly after a connection was established. If you register this
	 * {@link Recipient} to a {@link Postman} that already delivers {@link Letter}s to its {@link Recipient}s, this
	 * method will no be called. No {@link Letter} will be delivered before this methods returns.
	 *
	 * @param from the {@link Postman} that is starting
	 */
	public void noticeStart(Postman from);

	/**
	 * Indicates that a given {@link Postman} has delivered the last {@link Letter} to this {@link Recipient}. This
	 * method is called by a {@link Postman} directly before it closes the connections. In case that the opposite site
	 * closes the connection normally, this method gives the last possibility to send a {@link Letter}. If the
	 * connection was terminated or if {@link Postman#stop()} was called, this possibility does not exist. You may use
	 * this method to perform a cleanup.
	 *
	 * @param from the {@link Postman} that is stopping
	 */
	public void noticeStop(Postman from);

}
