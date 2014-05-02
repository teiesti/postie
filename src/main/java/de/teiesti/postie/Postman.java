package de.teiesti.postie;

import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Postman<Letter> implements Runnable, AutoCloseable {

	private Mailbox mailbox;
	private Class<? extends Letter> letterClass;

	private Set<Recipient> recipients = new CopyOnWriteArraySet<>();

	private Thread worker;

	public Postman(Socket socket, Class<? extends Letter> letterClass) {
		if (letterClass == null)
			throw new IllegalArgumentException("letterClass == null");

		this.mailbox = new Mailbox(socket);
		this.letterClass = letterClass;
		this.worker = new Thread(this);

		worker.start();
	}

	public /*abstract*/ void send(Letter letter) {
		// not needed, because it is checked in mailbox.send(letter)
		/*if (letter == null)
			throw new IllegalArgumentException("letter == null");*/

		mailbox.send(letter);
	}

	protected abstract void deliver(Letter letter);

	protected final Set<Recipient> getRecipients() {
		return recipients;
	}

	public final void registerRecipient(Recipient<Letter> recipient) {
		if (recipient == null)
			throw new IllegalArgumentException("recipient == null");

		recipients.add(recipient);
	}

	public final void unregisterRecipient(Recipient<Letter> recipient) {
		if (recipient == null)
			throw new IllegalArgumentException("recipient == null");

		recipients.add(recipient);
	}

	@Override
	public final void run() {
		try {
			Letter letter;
			while (!worker.isInterrupted()) {
				letter = mailbox.receive(letterClass);
				deliver(letter);
			}
		} catch(InterruptedException e) {
			worker.interrupt();
		}
	}

	@Override
	public /*final?*/ void close() {
		// TODO
		// TODO we need to join the inbox thread before we can receive the last messages --> close inbox!
	}

}
