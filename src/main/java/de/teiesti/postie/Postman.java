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
	protected final Set<Recipient<Letter>> recipients = new CopyOnWriteArraySet<>();

	private final BlockingQueue<Letter> outbox = new LinkedBlockingDeque<>();

	private Thread sender;
	private Thread receiver;

	/**
	 * Clones this {@link Postman}. Cloning is not possible if this {@link Postman} is running. In this case this
	 * method throws a {@link IllegalStateException}.
	 *
	 * @return a clone of this {@link Postman}
	 *
	 * @throws IllegalArgumentException if this {@link Postman} is running
	 */
	@Override
	public synchronized Postman clone() {
		if (this.isRunning())
			throw new IllegalStateException("cannot clone because this postman is running");

		Postman result = null;
		try {
			result = (Postman) super.clone();
		} catch (CloneNotSupportedException e) {
			Logger.error(e);
			System.exit(1);
		}

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
	 * previously given one. A {@link Postman} can use one {@link Serializer} at once. It is not possible to change
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
	 *
	 * @return this {@link Postman}
	 */
	public synchronized final Postman start() {
		// TODO add javadoc for the IllegalStateExceptions
		if (isRunning())
			throw new IllegalStateException("cannot start because this postman is already running");
		if (socket == null)
			throw new IllegalStateException("cannot start because this postman was bound to no socket");
		if (socket.isClosed())
			throw new IllegalStateException("cannot start because bound socket is already closed");

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
