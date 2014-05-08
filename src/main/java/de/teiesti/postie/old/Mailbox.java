package de.teiesti.postie.old;

import com.google.gson.Gson;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * // TODO revise
 * A {@code Mailbox} delivers messages ("letters") through a given {@link Socket}. The message will be encodes in the
 * JSON format using the GSON library. Therefore a letter can be any object that GSON can handle. Different messages
 * will be separated by a single new line (either CR, LF or CRLF depending on the system).<br>
 * <br>
 * To submit a message, use the {@link #send(Object)}-method. To receive one, use the {@link #receive(Class)}-method.
 * <br><br>
 * The {@link Socket} given to a {@code Mailbox} should only be accessed by the {@code Mailbox} it was given to. No
 * warranty about what happens if someone tries to access it from outside.
 * <br><br>
 * A {@code Mailbox} starts two threads: one to deliver the incoming messages and one to receive the outgoing
 * messages. Above, all methods are fully thread-safe.
 */
public class Mailbox implements AutoCloseable {

	/** the {@link Socket} which is administrated by this {@code Mailbox} */
	private Socket socket;

	/** a {@link Gson} instance that encodes the messages */
	private Gson gson = new Gson();

	/** a {@link BlockingQueue} that stores the incoming messages */
	private BlockingQueue<String> inbox = new LinkedBlockingDeque<>();

	/** a {@link BlockingQueue} that stores the outgoing messages */
	private BlockingQueue<String> outbox = new LinkedBlockingDeque<>();

	/** a {@link Thread} which receives messages from the {@link Socket} */
	private Thread inboxWorker;

	/** a {@link Thread} which sends messages to the {@link Socket} */
	private Thread outboxWorker;

	/**
	 * Creates a new {@code Mailbox} which delivers objects ("letter") through a given {@link Socket}. Be careful:
	 * The {@code Socket} given to this {@code Mailbox} should not be accessed from outside.
	 *
	 * @param socket the {@link Socket} this {@code Mailbox} delivers through
	 *
	 * @throws IllegalArgumentException if {@code socket} is {@code null}
	 */
	public Mailbox(Socket socket) {
		if (socket == null)
			throw new IllegalArgumentException("socket == null");
		// TODO check more about the socket state here

		this.socket = socket;

		this.inboxWorker = new InboxWorker(this);
		this.outboxWorker = new OutboxWorker(this);

		inboxWorker.start();
		outboxWorker.start();
	}

	/**
	 * Sends a given object ("letter") through the socket. Therefore the object is serialized to the JSON format
	 * using the GSON library. Afterwards, the serialized copy is stored for sending, which a different thread handles
	 * concurrently. Because only the copy is stored, the object passed to this method can be modified after this
	 * method returns. This method can handle different "letter" types as long as GSON can encode it. But please note
	 * that the receiver must know the type to successfully decode the received JSON string. This method is thread-safe,
	 * which means that it can be accessed from different threads concurrently.
	 *
	 * @param letter the object to send though the socket
	 * @param <Letter> the {@link Class} of the "letter"
	 *
	 * @throws IllegalArgumentException if {@code letter} is {@code null}
	 * @throws IllegalStateException if this {@code Mailbox} is already closed
	 */
	public <Letter> void send(Letter letter) {
		if (letter == null)
			throw new IllegalArgumentException("letter == null");
		if (outboxWorker.isInterrupted())
			throw new IllegalStateException("Cannot send the given letter because the mailbox is already closed.");

		// convert letter
		String rawLetter = gson.toJson(letter);

		// store letter for sending
		try {
			outbox.put(rawLetter);
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	/**
	 * Returns an object ("letter") which was received from the socket as a JSON string. Before returning,
	 * this method decodes the string using the GSON library. Because the JSON string does not provide the original
	 * type, the user must declare it via a parameter. For additional information about how this method decodes a
	 * single message review {@link Gson#fromJson(String, Class)}. This method returns objects in the order they have
	 * arrived at the socket. If this {@code Mailbox} has no "letter" at the moment, this methods blocks until a letter
	 * arrives. Use {@link #hasLetter()} to check weather this method can provide a method instantly. Please remain,
	 * that any message can only be received once: This method returns the earliest incoming message that has not
	 * been received yet. This method is thread-safe, which means that it can be accessed from different threads
	 * concurrently.
	 *
	 * @param letterClass the type of the received "letter" which must be assignable to the generic method type
	 * @param <Letter> the type of the received "letter"
	 *
	 * @return the received message
	 *
	 * @throws InterruptedException if this method was interrupted while waiting
	 */
	public <Letter> Letter receive(Class<? extends Letter> letterClass) throws InterruptedException {
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

		String rawLetter = inbox.take();
		return gson.fromJson(rawLetter, letterClass);
	}

	/**
	 * Returns weather this {@code Mailbox} has an object ("letter") to deliver using the
	 * {@link #receive(Class)}-method. If this method returns {@code true}, the {@link #receive(Class)} does not block
	 * on the next call. Please be very careful when using this method in a multi-threaded environment because it
	 * does only return the current state: Another thread may steal "your" object.
	 *
	 * @return weather this {@link Mailbox} has an object to deliver using {@link #receive(Class)}
	 */
	public boolean hasLetter() {
		return !inbox.isEmpty();
	}

	/**
	 * Closes this {@code Mailbox}. This method does also close the given {@link Socket}. After closing this {@code
	 * Mailbox} it is no longer possible to send "letters" using the {@link #send(Object)}-method. Beyond that,
	 * this {@code Mailbox} does not longer receive any "letters" from the given {@link Socket} but "letters" that
	 * have been received but not yet collected can still be picked using the {@link #receive(Class)}-method.<br>
	 * <br>
	 * In detail, closing a {@code Mailbox} works as follows: First, this method closes the output stream which means
	 * that EOF (end of file) is sent. A correctly implemented opposite mailbox will notice that and close its output
	 * stream, too. This - indeed - will result in a closed input stream on this side. When at least both streams -
	 * input and output - are closed, this method will close the socket. This protocol guarantees that no message that
	 * was sent will be lost. But if the opposite site is evil, this method possibly never returns.
	 */
	@Override
	public void close() {
		try {
			// close out
			outboxWorker.interrupt();
			outboxWorker.join();

			// close in
			if (inboxWorker != Thread.currentThread())
				inboxWorker.join();

			// close socket
			socket.close();
		} catch(InterruptedException | IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	/**
	 * The {@code InboxWorker} is a {@link Thread} that receives "letters" from the socket's input stream. Received
	 * messages are stores until the user fetches then using the {@link Mailbox#receive(Class)}-method.
	 */
	private class InboxWorker extends Thread {

		/** the {@link Mailbox} this {@code InboxWorker works on */
		private Mailbox mailbox;

		/**
		 * Create a new {@code InboxWorker}.
		 *
		 * @param mailbox the associated {@link Mailbox}
		 */
		public InboxWorker(Mailbox mailbox) {
			assert mailbox != null : "mailbox == null";
			this.mailbox = mailbox;
		}

		/**
		 * Receives any incoming message in a loop. When the last messages was received,
		 * this method closes the associated {@link Mailbox}.
		 */
		@Override
		public void run() {
			// open input reader
			BufferedReader in = openInput();

			// receive letters
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

			// close mailbox
			mailbox.close();
		}

		private BufferedReader openInput() {
			BufferedReader result = null;

			try {
				int inBuffer = mailbox.socket.getReceiveBufferSize();
				InputStream inStream = socket.getInputStream();
				result =  new BufferedReader(new InputStreamReader(inStream), inBuffer);
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			return result;
		}

	}

	/**
	 * The {@code OutboxWorker} is a {@link Thread} that sends "letters" to the socket's output stream: Any message
	 * that is given to {@link Mailbox} using the {@link Mailbox#send(Object)}-method will be stored until it can be
	 * transferred by this thread.
	 */
	private class OutboxWorker extends Thread {

		/** the {@link Mailbox} this {@code OutboxWorker works on */
		private Mailbox mailbox;

		/**
		 * Create a new {@code OutboxWorker}.
		 *
		 * @param mailbox the associated {@link Mailbox}
		 */
		public OutboxWorker(Mailbox mailbox) {
			assert mailbox != null : "mailbox == null";
			this.mailbox = mailbox;
		}

		@Override
		public void run() {
			// open output writer
			BufferedWriter out = openOutput();

			// send letters
			String letter;
			try {
				while (!this.isInterrupted()) {
					letter = outbox.take();
					out.write(letter);
					out.newLine();
					if (outbox.isEmpty()) out.flush();
				}
			} catch (InterruptedException e) {
				// reset interrupt status
				this.interrupt();
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			// close the mailbox output
			try {
				mailbox.socket.shutdownOutput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private BufferedWriter openOutput() {
			BufferedWriter result = null;

			try {
				int outBuffer = mailbox.socket.getSendBufferSize();
				OutputStream outStream = socket.getOutputStream();
				result =  new BufferedWriter(new OutputStreamWriter(outStream), outBuffer);
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			return result;
		}

	}

}
