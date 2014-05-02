package de.teiesti.postie;

import java.net.Socket;

public class ParallelPostman<Letter> extends Postman<Letter> {

	public ParallelPostman(Socket socket, Class<? extends Letter> letterClass) {
		super(socket, letterClass);
	}

	@Override
	protected void deliver(Letter letter) {

	}

}
