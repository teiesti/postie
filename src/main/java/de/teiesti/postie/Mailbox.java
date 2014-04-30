package de.teiesti.postie;

import com.google.gson.Gson;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.Socket;

/**
 * A {@code Mailbox} delivers messages ("letters") through a given {@link Socket}. The message will be encodes in the
 * JSON format using the GSON library. Therefore a letter can be any object that GSON can handle. Different messages
 * will be separated by a single new line (either CR, LF or CRLF depending on the system).<br>
 * <br>
 * To submit a message, use the {@link #send(Object)}-method. To receive one, use the {@link #receive(Class)}-method.
 * <br><br>
 * The {@link Socket} given to a {@code Mailbox} should only be accessed by {@code Mailbox} it was given to. No
 * warranty about what happens if someone tries access it from outside.
 * <br><br>
 * A {@code Mailbox} starts two threads: one to deliver the incoming messages and one to receive the outgoing
 * messages. Above, all methods are fully thread-safe.
 */
public class Mailbox implements AutoCloseable {

	/** the structure which handles the incoming messages */
	private Inbox inbox;

	/** the structure which handles the outgoing messages */
	private Outbox outbox;

	/** the socket which is administrated by this {@code Mailbox} */
	private Socket socket;

	/** GSON instance that encodes the messages */
	private Gson gson = new Gson();

	/**
	 * Creates a new {@code Mailbox} which delivers objects ("letter") through a given {@link Socket}. Be careful:
	 * The {@code Socket} given to this {@code Mailbox} should not be accessed from outside.
	 *
	 * @param socket the {@link Socket} this {@code Mailbox} delivers through
	 */
	public Mailbox(Socket socket) {
		if (socket == null)
            throw new IllegalArgumentException("socket == null");
        // TODO check more about the socket state here

        this.socket = socket;

		try {

			int inBuffer = socket.getReceiveBufferSize();
			InputStream inStream = socket.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream), inBuffer);
			inbox = new Inbox(in);

			int outBuffer = socket.getSendBufferSize();
			OutputStream outStream = socket.getOutputStream();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream), outBuffer);
			outbox = new Outbox(out);

		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
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

		String rawLetter = gson.toJson(letter);
		outbox.send(rawLetter);
	}

    /**
     * Returns an object ("letter") which was received from the socket as a JSON string. Before returning,
	 * this method decodes the string using the GSON library. Because the JSON string does not provide the original
	 * type, the user must declare it via a parameter. For additional information about how this method decodes a
	 * single message review {@link Gson#fromJson(String, Class)}. This method returns objects in the order they have
	 * arrived at the socket. If this {@code Mailbox} has not "letter" at the moment, this methods blocks until a letter
	 * arrives. Use {@link #hasLetter()} to check weather this method can provide a method instantly. This method is
	 * thread-safe, which means that it can be accessed from different threads concurrently.
	 *
	 * @param letterClass the type of the received "letter" which must be assignable to the generic method type
	 * @param <Letter> the type of the received "letter"
	 *
     * @return the received message
     */
	public <Letter> Letter receive(Class<? extends Letter> letterClass) {
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

        String rawLetter = inbox.receive();
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
		return inbox.hasLetter();
	}

	/**
	 * Closes this {@code Mailbox}. This method does also close the given {@link Socket}.
	 */
	@Override
	public void close() {
		// inbox.close(); 	// there is now need to close the inbox
		outbox.close();
		try {
			socket.close();
		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}
}