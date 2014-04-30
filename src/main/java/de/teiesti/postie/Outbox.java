package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An {@code Outbox} is the part of a {@link Mailbox} which sends messages ("letters"). Any message that is given to
 * this {@code Outbox} using the {@link #send(String)}-method will be stored until it can be transferred. This class
 * should only be accessed from the associated {@link Mailbox}.<br />
 * <br />
 * Warning: Do not start a {@link Thread} for a {@code Outbox}. These things are completely handled from inside the
 * class.<br />
 * Note: An {@link Outbox} must be closed after the last message was handed over to it. When closing the instance,
 * no message will be discarded.
 */
class Outbox implements Runnable, AutoCloseable {

	/** the sink where the messages go to */
	private BufferedWriter out;

	/** a queue storing messages that have not been transferred yet */
	private BlockingQueue<String> outbox = new LinkedBlockingDeque<>();

	/** a flag that indicated weather this {@code Inbox} should be closed*/
	private boolean close = false;

	private Thread worker;

	public Outbox(BufferedWriter out) {
		if (out == null)
			throw new IllegalArgumentException("out == null");

		this.out = out;
		this.worker = new Thread(this);

		worker.start();
	}

	public void send(String letter) {
		if (letter == null)
			throw new IllegalArgumentException("letter == null");
		if (close)
			throw new IllegalStateException("cannot send a letter because this is already closed");

		try {
			outbox.put(letter);
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	@Override
	public void run() {
		String letter;
		try {
			while (!close) {
				letter = outbox.take();
				out.write(letter);
				out.newLine();
				if (outbox.isEmpty()) out.flush();
			}
		} catch (InterruptedException | IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	@Override
	public void close() {
		if (!close) {
			try {
				// close the worker thread
				close = true;
				worker.join();

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
