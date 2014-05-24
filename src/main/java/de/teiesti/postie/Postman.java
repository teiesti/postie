package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A {@link Postman} delivers {@link Letter}s through a given {@link Socket}. A {@link Letter} can be any class that is
 * serializable by a {@link Serializer}. To send a message, pass it to {@link #send(Object)}. To receive messages
 * register a {@link Recipient} with {@link #register(Recipient)}<br>
 * <br>
 * To setup a {@link Postman} you must bind a {@link Socket} with {@link #bind(Socket)}. Beyond, you must register a
 * {@link Serializer} with {@link #use(Serializer)}. Afterwards you can start the {@link Postman} with
 * {@link #start()}. This will start two threads to handle the incoming and outgoing letters. You can stop a
 * {@link Postman} with {@link #stop()}.<br>
 * <br>
 * All provided methods are thread-safe.
 *
 * @param <Letter> type of the letters
 */
public abstract class Postman<Letter> implements Cloneable {

	private Socket socket;
	private Serializer<Letter> serializer;
	protected Set<Recipient<Letter>> recipients = new CopyOnWriteArraySet<>();

	private BlockingQueue<Letter> outbox = new LinkedBlockingDeque<>();

	private Thread sender;
	private Thread receiver;

	/**
	 * Clones this {@link Postman}. Cloning a {@link Postman} works as follows:
	 * <ul>
	 *     <li>The {@link Socket} this {@link Postman} was bound to can not be reused, because it is not
	 *     thread-safe, or copied, because it does not provide the necessary information. Therefore it is set to
	 *     {@code null}.</li>
	 *     <li>The {@link Serializer} can be reused because it is thread-safe and does not save any state. So the
	 *     reference is copied.</li>
	 *     <li>The registered {@link Recipient}s should not be shared across different {@link Postman} automatically.
	 *     Therefore their {@link Set} is copied but the {@link Recipient}s stay the same.
	 *     <li>Because a {@link Socket} is missing, no {@link Thread} can be started.</li>
	 * </ul>
	 * Summary: To obtain a running {@link Postman} from a clone, you must at least - depending on the original -
	 * call {@link #bind(Socket)} and {@link #start()}.
	 *
	 * @throws CloneNotSupportedException not thrown
	 */
	@Override
	public Postman clone() throws CloneNotSupportedException {
		Postman result = (Postman) super.clone();

		// fields that won't be copied and must be initialized for new
		socket = null;
		sender = null;
		receiver = null;

		// fields that will be copied in deep
		recipients = new CopyOnWriteArraySet<>(recipients);
		outbox = new LinkedBlockingDeque<>(outbox);

		// don't wonder: the reference to serializer was copied during super.clone()

		return result;
	}

	/**
	 * Binds this {@link Postman} to a given {@link Socket}. A given {@link Socket} will override a previously given
	 * one. A {@link Postman} can only use one {@link Socket} at once. It is not possible to change the {@link
	 * Socket} as long as this {@link Postman} is running. In this case this method throws a
	 * {@link IllegalStateException}.
	 *
	 * @param socket the {@link Socket} this {@link Postman} should bind to
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalStateException if this {@link Postman} is running
	 * @throws IllegalArgumentException if {@code socket} is {@code null}
	 */
	public synchronized final Postman bind(Socket socket) {
		if (this.isRunning())
			throw new IllegalStateException("cannot bind a socket because this postman is running");
		if (socket == null)
			throw new IllegalArgumentException("socket == null");
		// TODO check more about the socket state here

		this.socket = socket;

		return this;
	}

	/**
	 * Makes this {@link Postman} to use the given {@link Serializer}. A given {@link Serializer} will override a
	 * previously given one. A {@link Postman} can only use one {@link Serializer} at once. It is not possible to change
	 * the {@link Serializer} as long as this {@link Postman} is running. In this case this method throws a
	 * {@link IllegalStateException}.
	 *
	 * @param serializer the {@link Serializer} this {@link Postman} should use
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalStateException if this {@link Postman} is running
	 * @throws IllegalArgumentException if {@code serializer} is {@code null}
	 */
	public synchronized final Postman use(Serializer<Letter> serializer) {
		if (this.isRunning())
			throw new IllegalStateException("cannot use a serializer because this postman is running");
		if (serializer == null)
			throw new IllegalArgumentException("serializer == null");

		this.serializer = serializer;

		return this;
	}

	/**
	 * Starts this {@link Postman}. This will start two {@link Thread}s: one that delivers the incoming {@link
	 * Letter}s to any registered {@link Recipient} and one that sends the outgoing messages through the {@link Socket}.
	 * If this {@link Postman} cannot start for some reason, this method throws an {@link IllegalStateException}.
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalArgumentException if this {@link Postman} cannot start because it is already running,
	 * not bound to a {@link Socket} or bound to {@link Socket} was was already closed or does not use a {@link
	 * Serializer}
	 */
	public synchronized final Postman start() {
		if (isRunning())
			throw new IllegalStateException("cannot start because this postman is already running");
		if (socket == null)
			throw new IllegalStateException("cannot start because this postman not bound to a socket");
		if (socket.isClosed())
			throw new IllegalStateException("cannot start because bound socket is already closed");
		if (serializer == null)
			throw new IllegalArgumentException("cannot start because no serializer was configured (used)");

		sender = new Sender();
		receiver = new Receiver();

		sender.start();
		receiver.start();

		return this;
	}

	/**
	 * Registers a {@link Recipient}. Any {@link Letter} that was received from the {@link Socket} by this
	 * {@link Postman} will be delivered to any registered {@link Recipient} using {@link #deliver(Object)}.
	 *
	 * @param recipient the {@link Recipient} to register
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalArgumentException if {@code recipient} is {@code null}
	 */
	public final Postman register(Recipient recipient) {
		if (recipient == null)
			throw new IllegalArgumentException("recipient == null");

		recipients.add(recipient);

		return this;
	}

	/**
	 * Unregisters the given {@link Recipient}. After calling this method, the given {@link Recipient} will not
	 * receive any letter from this {@link Postman} anymore. If the given {@link Recipient} was not registered,
	 * this method does nothing.
	 *
	 * @param recipient the {@link Recipient} to unregister
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalArgumentException if {@code recipient} is {@code null}
	 */
	public final Postman unregister(Recipient recipient) {
		if (recipient == null)
			throw new IllegalArgumentException("recipient == null");

		recipients.remove(recipient);

		return this;
	}

	/**
	 * Sends a {@link Letter} through the {@link Socket} that was bind to this {@link Postman}. In detail,
	 * this method stores the given {@link Letter} for sending and returns. Therefore the {@link Letter} should
	 * not be modified after it was passed to this method. If this {@link Postman} is running,
	 * an other {@link Thread} will pick the {@link Letter} at some time in the future. It will serialize the letter
	 * with the {@link Serializer} that was given to this {@link Postman} during setup. Afterwards the {@link Thread}
	 * sends the serialized {@link Letter} through the {@link Socket} this {@link Postman} was bind to. If this
	 * {@link Postman} is not running, it will store the {@link Letter} until it was started with {@link #start()}. A
	 * {@link Postman} sends {@link Letter}s in the order they where passed to this method.
	 *
	 * @param letter the {@link Letter} to send
	 *
	 * @return this {@link Postman}
	 */
	public final Postman send(Letter letter) {
		if (letter == null)
			throw new IllegalArgumentException("letter == null");

		try {
			outbox.put(letter);
		} catch(InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}

		return this;
	}

	/**
	 * This method should delivers the given {@link Letter} to any {@link Recipient} that was registered with
	 * {@link #register(Recipient)}. This method is called from the {@link Thread} that receives {@link Letter}s from
	 * the {@link Socket}. Therefore this method should not block for long.
	 *
	 * @param letter the {@link Letter} to deliver
	 *
	 * @return this {@link Postman}
	 */
	protected abstract Postman deliver(Letter letter);

	/**
	 * This method should report to any {@link Recipient} that the last {@link Letter} was delivered. In order to do
	 * that it should call {@link Recipient#acceptedLast} for any registered recipient with this {@link Postman} as
	 * argument. This method is called from the {@link Thread} that receives {@link Letter}s from the {@link Socket}
	 * after the last {@link Letter} was received but before it closes the sending {@link Thread}. This method should
	 * not return before any {@link Recipient#acceptedLast} has returned to give these actions the possibility to send
	 * a {@link Letter}. (Note: If you stop this {@link Postman} with {@link Postman#stop} the sender has closed
	 * before this method is called.)
	 *
	 * @return this {@link Postman}
	 */
    protected abstract Postman reportLast();

	/**
	 * Stops this {@link Postman}. If this {@link Postman} is not running yet, this method throws a
	 * {@link IllegalStateException}. Stopping a {@link Postman} stops the two threads that send and receive
	 * {@link Letter}s and closes the {@link Socket} this {@link Postman} was bind to. Therefore this {@link Postman}
	 * must bind to a new {@link Socket} before it can be started with {@link #start()}. Any message that was stored
	 * for sending with {@link #send(Object)} will be sent before this {@link Postman} is closed.<br>
	 * <br>
	 * The exactly closing procedure works as follows:
	 * <ol>
	 *     <li>Send any message that was given to {@link #send(Object)}.</li>
	 *     <li>Shutdown the {@link Socket} output. That will send an {@code EOF}. Sending {@code EOF} is the
	 *     indication for the opposite side to stop sending.</li>
	 *     <li>Stop the sender thread.</li>
	 *     <li>Wait until the opposite side sends {@code EOF}.</li>
	 *     <li>Close the {@link Socket}.</li>
	 *     <li>Stop the receiving thread.</li>
	 * </ol>
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalStateException if this {@link Postman} is running
	 */
	public synchronized final Postman stop() {
		if (!isRunning())
			throw new IllegalStateException("cannot stop because this postman is not running");

		sender.interrupt();
		try {
			sender.join();
			receiver.join();
		} catch (InterruptedException e) {
			Logger.error(e);
			System.exit(1);
		}

		sender = null;
		receiver = null;

		return this;
	}

	/**
	 * Returns weather this {@link Postman} is running.
	 *
	 * @return if this {@link Postman} is running.
	 */
	public final boolean isRunning() {
		return receiver != null && receiver.isAlive();
	}

	private class Sender extends Thread {

		@Override
		public void run() {
			// open output writer
			BufferedWriter out = openOutput();

			// send letters
			Letter letter;
			try {
				while (!this.isInterrupted()) {
					letter = outbox.take();
					serializer.encodeNext(out, letter);
					if (outbox.isEmpty()) out.flush();
				}
			} catch (InterruptedException e) {
				// reset interrupt status
				this.interrupt();
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			// clean up
			try {
				while (!outbox.isEmpty()) {
					letter = outbox.poll();
					serializer.encodeNext(out, letter);
				}
				out.flush();
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			// close the postman output
			try {
				socket.shutdownOutput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private BufferedWriter openOutput() {
			BufferedWriter result = null;

			try {
				int outBuffer = socket.getSendBufferSize();
				OutputStream outStream = socket.getOutputStream();
				result =  new BufferedWriter(new OutputStreamWriter(outStream), outBuffer);
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			return result;
		}

	}

	private class Receiver extends Thread {

		@Override
		public void run() {
			// open input reader
			BufferedReader in = openInput();

			// receive letters
			try {
				Letter letter = serializer.decodeNext(in);
				while (letter != null) {
					deliver(letter);
					letter = serializer.decodeNext(in);
				}
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

            // report recipients that the last letter was delivered
            reportLast();

			// close sender: receiving EOF shows that the opposite site wants to close the connection
			sender.interrupt();
			try {
				sender.join();
			} catch (InterruptedException e) {
				Logger.error(e);
				System.exit(1);
			}

			// close socket
			try {
				socket.close();
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

		}

		private BufferedReader openInput() {
			BufferedReader result = null;

			try {
				int inBuffer = socket.getReceiveBufferSize();
				InputStream inStream = socket.getInputStream();
				result =  new BufferedReader(new InputStreamReader(inStream), inBuffer);
			} catch (IOException e) {
				Logger.error(e);
				System.exit(1);
			}

			return result;
		}

	}

}
