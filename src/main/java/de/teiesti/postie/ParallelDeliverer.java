package de.teiesti.postie;

import java.net.Socket;

public class ParallelDeliverer<Letter> extends Postman<Letter> {

	public ParallelDeliverer(Socket socket, Class<? extends Letter> letterClass) {
		super(socket, letterClass);
	}

	@Override
	protected void deliver(Letter letter) {

	}

}
