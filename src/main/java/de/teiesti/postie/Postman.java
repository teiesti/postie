package de.teiesti.postie;

import java.net.Socket;
import java.util.Set;

public abstract class Postman<Letter> implements Runnable, AutoCloseable {

	public Postman(Socket socket, Class<? extends Letter> letterClass) {

	}

	public /*abstract*/ void send(Letter letter) {

	}

	protected abstract void deliver(Letter letter);

	protected final Set<Recipient> getRecipients() {
		return null;
	}

	public final void registerRecipient(Recipient<Letter> recipient) {

	}

	public final void unregisterRecipient(Recipient<Letter> recipient) {

	}

	@Override
	public final void run() {

	}

	@Override
	public /*final?*/ void close() {

	}

}
