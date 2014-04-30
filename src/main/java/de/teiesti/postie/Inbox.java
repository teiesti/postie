package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An {@code Inbox} is the part of a {@link Mailbox} which receives messages ("letters"). Any message is stored
 * until a user fetches it with the {@link Mailbox#receive(Class)}-method. This class should only be accessed from
 * the associated {@link Mailbox}.<br />
 * <br />
 * Warning: Do not start a {@link Thread} for a {@code Inbox}. These things are completely handled from inside the
 * class.<br />
 * Note: In contrast to {@link Outbox}, an {@link Inbox} must not be closed. Any instance closes automatically when
 * the input ends.
 */
class Inbox implements Runnable {

	/** the source where the messages come from */
	private BufferedReader in;

	/** a queue in which the messages are store until they are fetched by a user */
	private BlockingQueue<String> inbox = new LinkedBlockingDeque<>();

	/**
	 * Create a new {@code Inbox} that reads messages ("letters") from a given {@link BufferedReader}. Calling this
	 * constructor starts a single thread which handles the incoming messages.
	 *
	 * @param in the source of the messages
	 */
	public Inbox(BufferedReader in) {
		if (in == null)
			throw new IllegalArgumentException("in == null");

		this.in = in;

		new Thread(this).start();
	}

	/**
	 * Returns the next "letter" as a JSON string. If there is no message at the moment, this method blocks until a new
	 * messages arrives. Please remain, that any message can only be received once: This method returns the earliest
	 * incoming message that has not been received yet.
	 *
	 * @return the current message as a JSON string
	 */
	public String receive() {
		try {
			return inbox.take();
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
		return null;	// will never be executed but is required by Java
	}

	/**
	 * Returns weather a message has arrived that can be fetched by a user. This method returns {@code true} if and
	 * only if {@link #receive()}} would block.
	 *
	 * @return if a "letter" has arrived
	 */
	public boolean hasLetter() {
		return !inbox.isEmpty();
	}

	/**
	 * Receives any incoming message in a loop. The method - as well as the {@link Thread#start()} should not be
	 * called from outside this class.
	 */
	@Override
	public void run() {
		try {
			String letter = in.readLine();
			while(letter != null) {
				inbox.put(letter);
				letter = in.readLine();
			}
		} catch (InterruptedException | IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

}
