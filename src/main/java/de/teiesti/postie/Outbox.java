package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An {@code Outbox} is the part of a {@link Mailbox} which sends messages ("letters"). Any message that is given to
 * this {@code Outbox} using the {@link #send(String)}-method will be stored until it can be transferred. This class
 * should only be accessed from the associated {@link Mailbox}.<br>
 * <br>
 * Warning: Do not start a {@link Thread} for a {@code Outbox}. These things are completely handled from inside the
 * class.<br>
 * Note: An {@link Outbox} must be closed after the last message was handed over to it. When closing the instance,
 * no message will be discarded.
 */
class Outbox implements Runnable, AutoCloseable {

	/** the sink where the messages go to */
	private BufferedWriter out;

	/** a queue storing messages that have not been transferred yet */
	private BlockingQueue<String> outbox = new LinkedBlockingDeque<>();

	/** the thread working this {@code Outbox} to transfer messages*/
	private Thread worker;

	/**
	 * Create a new {@code Outbox} that writes messages ("letters") to a given {@link BufferedWriter}. Calling this
	 * constructor starts a single thread which handles the outgoing messages.
	 *
	 * @param out the sink of the messages
	 *
	 * @throws IllegalArgumentException if {@code out} is {@code null}
	 */
	public Outbox(BufferedWriter out) {
		if (out == null)
			throw new IllegalArgumentException("out == null");

		this.out = out;
		this.worker = new Thread(this);

		worker.start();
	}

	/**
	 * Accepts "letters" encoded as strings for delivery. The transfer will be managed by a concurrent thread.
	 *
	 * @param letter the message to send
	 *
	 * @throws IllegalArgumentException if {@code letter} is {@code null}
	 * @throws IllegalStateException if this {@code Outbox} was already closed
	 *
	 */
	public void send(String letter) {
		if (letter == null)
			throw new IllegalArgumentException("letter == null");
		if (worker.isInterrupted())
			throw new IllegalStateException("cannot send a letter because this outbox is already closed");

		try {
			outbox.put(letter);
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	/**
	 * Sends any pending message in a loop. This method - as well as the {@link Thread#start()} - should not be
	 * called from outside this class.
	 */
	@Override
	public void run() {
		String letter;
		try {
			while (!worker.isInterrupted()) {
				letter = outbox.take();
				out.write(letter);
				out.newLine();
				if (outbox.isEmpty()) out.flush();
			}
		} catch (InterruptedException e) {
			// reset interrupt status and terminate
			worker.interrupt();
		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	/**
	 * Closes this {@code Outbox}. In detail, this method stops the worker thread and transfers any "letter" that was
	 * that was not send yet.
	 */
	@Override
	public void close() {
		if (!worker.isInterrupted()) {
			try {
				worker.interrupt();	// stops the worker thread
				worker.join();		// avoids a concurrent modification of out

				// clean things up
				String letter = outbox.poll();
				while (letter != null) {
					out.write(letter);
					out.newLine();
				}

				// don't close out here, because it belongs to a socket which is handled from elsewhere
				out.flush();
			} catch (IOException | InterruptedException e) {
				Logger.error(e);
				System.exit(1);
			}
		}
	}

}
