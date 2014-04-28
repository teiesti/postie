package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.net.Socket;

/**
 * A {@code Postman} delivers messages ("letters") through a given {@link Socket}. A message - which can be any object
 * that is assignable to the {@link Type} specified in the constructor - will be encodes in the JSON format and
 * separated by a single new line (either CR, LF or CRLF depending on the system).<br />
 * <br />
 * To submit a message, use the {@link #send(Object)}-method. To receive one, use the {@link #receive()}-method.<br />
 * <br />
 * A {@code Postman} starts one thread to handle the incoming messages (see {@link Inbox}) and one thread to handle the
 * outgoing messages (see {@link Outbox}). Above, it is fully thread-safe.
 */
public class Postman implements AutoCloseable {

	/** the structure which handles the incoming messages */
	private Inbox inbox;

	/** the structure which handles the outgoing messages */
	private Outbox outbox;

	/** the socket which is administrated by this {@code Postman} */
	private Socket socket;

	/**
	 * Creates a new {@code Postman} which delivers objects ("letter") of a given {@link Type} through a given {@link
	 * Socket}. The type is used to instantiate incoming objects.
	 *
	 * @param socket the {@link Socket} this {@code Postman} delivers through
	 * @param letterType the {@link Type} of the letters this {@code Postman} delivers
	 */
	public Postman(Socket socket, Type letterType) {
		if (socket == null)
            throw new IllegalArgumentException("socket == null");
        // TODO check more about the socket here
        if (letterType == null)
			throw new IllegalArgumentException("letterClass == null");

        this.socket = socket;

		try {

			int inBuffer = socket.getReceiveBufferSize();
			InputStream inStream = socket.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(inStream), inBuffer);
			inbox = new Inbox(in, letterType);

			int outBuffer = socket.getSendBufferSize();
			OutputStream outStream = socket.getOutputStream();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream), outBuffer);
			outbox = new Outbox(out, letterType);

		} catch (IOException e) {
			Logger.error(e);
			System.exit(1);
		}
	}

	/**
	 * Sends a given object ("letter") through the socket. Therefore the object is serialized to the JSON format. This
	 * method does not block until the sending has completed: It just takes the object; the serialization and delivery
	 * is organized by another concurrent thread. Therefore, it is not allowed to change the object after submitting
	 * it to this method. This method is thread-safe, which means that it can be accessed from different threads
	 * concurrently.
	 *
	 * @param letter the object to send though the socket
	 * @param <Letter> the {@link Type} of the "letter" which must be assignable to the {@link Type} specified in the
	 *                   constructor
	 * @throws IllegalArgumentException if {@code letter} is {@code null}
	 * @throws ClassCastException if {@code letter} is not assignable to the {@link Type} specified in the constructor
	 * @throws IllegalStateException if this {@code Postman} is already closed
	 */
	public <Letter> void send(Letter letter) {
		outbox.send(letter);
	}

    /**
     * Returns an object ("letter") received from the socket. The object was accepted and deserialized from the JSON
	 * format to the {@link Type} that was specified in the constructor by another concurrent thread. This method
	 * returns objects in the order they have arrived at the socket. This method is thread-safe, which means that it can
	 * be accessed from different threads concurrently. This method is generic and casts the received object to the type
	 * that is needed by the caller: This means that it comes to a {@link ClassCastException} if the requested type
	 * is not assignable by the {@link Type} specified in the constructor. If this {@code Postman} has not letter at
	 * the moment, this methods blocks until a letter arrives. Use {@link #hasLetter()} to check wheather this method
	 * can provide a method instantly.
	 *
     * @param <Letter> the type of the received object
     * @return an object
     */
    @SuppressWarnings("unchecked")
	public <Letter> Letter receive() {
        return (Letter) inbox.receive();
	}

	public boolean hasLetter() {
		// TODO
		return false;
	}

	/**
	 * Closes this {@code Postman}. This method does also close the given {@link Socket}.
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