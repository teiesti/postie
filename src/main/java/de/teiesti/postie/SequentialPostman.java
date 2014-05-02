package de.teiesti.postie;

import java.net.Socket;

public class SequentialPostman<Letter> extends Postman<Letter> {

	public SequentialPostman(Socket socket, Class<? extends Letter> letterClass) {
		super(socket, letterClass);
	}

	@Override
	protected void deliver(Letter letter) {
		for (Recipient r : getRecipients())
			r.accept(letter);
	}

}
