package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An {@code Inbox} is the part of a {@link Mailbox} which receives messages ("letters"). Any message is stored
 * until a user fetches it with the {@link #receive()}-method. This class should only be accessed from the associated
 * {@link Mailbox}.<br>
 * <br>
 * Warning: Do not start a {@link Thread} for a {@code Inbox}. These things are completely handled from inside the
 * class.<br>
 * Note: In contrast to {@link Outbox}, an {@link Inbox} must not be closed. Any instance closes automatically when
 * the input ends. But sometimes it might be useful to wait for the last "letter" put into an {@code} Inbox. This can
 * be done using the {@link #awaitLastLetter()}-method.
 */
class Inbox implements Runnable {

	/** the source where the messages come from */
	private BufferedReader in;

	/** a queue in which the messages are store until they are fetched by a user */
	private BlockingQueue<String> inbox = new LinkedBlockingDeque<>();

	/** the thread working on this {@code Inbox} to receive messages */
	private Thread worker;

	/**
	 * Create a new {@code Inbox} that reads messages ("letters") from a given {@link BufferedReader}. Calling this
	 * constructor starts a single thread which handles the incoming messages.
	 *
	 * @param in the source of the messages
	 *
	 * @throws IllegalArgumentException if {@code in} is {@code null}
	 */
	public Inbox(BufferedReader in) {
		if (in == null)
			throw new IllegalArgumentException("in == null");

		this.in = in;
		this.worker = new Thread(this);

		worker.start();
	}

	/**
	 * Returns the next "letter" as a JSON string. If there is no message at the moment, this method blocks until a new
	 * messages arrives. Please remain, that any message can only be received once: This method returns the earliest
	 * incoming message that has not been received yet.
	 *
	 * @return the current message as a JSON string
	 *
	 * @throws InterruptedException if this method was blocking and interrupted
	 */
	public String receive() throws InterruptedException {
		return inbox.take();
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
	 * Blocks until the last "letter" was received from the source. This is just the case if the source has closed and
	 * the thread working on this {@code Inbox} has terminated. Please note: This method does not wait until this
	 * {@code Inbox} is empty but until the last letter was put into this {@code Inbox}.
	 */
	public void awaitLastLetter() {
		try {
			worker.join();
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	/**
	 * Receives any incoming message in a loop. This method - as well as the {@link Thread#start()} - should not be
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
