package de.teiesti.postie;

import java.net.Socket;

public class SequentialDeliverer<Letter> extends Postman<Letter> {

	public SequentialDeliverer(Socket socket, Class<? extends Letter> letterClass) {
		super(socket, letterClass);
	}

	@Override
	protected void deliver(Letter letter) {

	}

}
