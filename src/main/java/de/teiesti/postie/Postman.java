package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;

public abstract class Postman<Letter> implements Cloneable {

	private Socket socket;
	private Serializer<Letter> serializer;
	protected final Set<Recipient<Letter>> recipients = new CopyOnWriteArraySet<>();

	private final BlockingQueue<Letter> outbox = new LinkedBlockingDeque<>();

	private Thread sender;
	private Thread receiver;

	@Override
	public Postman clone() {
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

	public final Postman bind(Socket socket) {
		if (this.isRunning())
			throw new IllegalStateException("cannot bind a socket because this postman is running");
		if (socket == null)
			throw new IllegalArgumentException("socket == null");
		// TODO check more about the socket state here

		this.socket = socket;

		return this;
	}

	public final Postman use(Serializer<Letter> serializer) {
		if (this.isRunning())
			throw new IllegalStateException("cannot use a serializer because this postman is running");
		if (serializer == null)
			throw new IllegalArgumentException("serializer == null");

		this.serializer = serializer;

		return this;
	}

	public final Postman start() {
		// TODO	check conditions: socket?

		sender = new Sender(this);
		receiver = new Receiver(this);

		sender.start();
		receiver.start();

		return this;
	}

	public final Postman register(Recipient recipient) {
		if (recipient == null)
			throw new IllegalArgumentException("recipient == null");

		recipients.add(recipient);

		return this;
	}

	public final Postman unregister(Recipient recipient) {
		if (recipient == null)
			throw new IllegalArgumentException("recipient == null");

		recipients.remove(recipient);

		return this;
	}

	public final Postman send(Letter letter) {
		// TODO question: Must this Postman be running if sending a message. --> no, but the message will not be sent yet
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

	protected abstract Postman deliver(Letter letter);

	public final Postman stop() {
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

	public final boolean isRunning() {
		return sender != null && !sender.isInterrupted();
	}

	private class Sender extends Thread {

		private final Postman postman;

		public Sender(Postman postman) {
			assert postman != null : "postman == null";
			this.postman = postman;
		}

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
			// TODO

			// close the postman output
			try {
				postman.socket.shutdownOutput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private BufferedWriter openOutput() {
			BufferedWriter result = null;

			try {
				int outBuffer = postman.socket.getSendBufferSize();
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
		private final Postman postman;

		public Receiver (Postman postman) {
			assert postman != null : "postman == null";
			this.postman = postman;
		}

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
				int inBuffer = postman.socket.getReceiveBufferSize();
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
