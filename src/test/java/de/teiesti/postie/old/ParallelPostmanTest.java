package de.teiesti.postie.old;

import java.net.Socket;

public class ParallelPostmanTest extends PostmanTest {

	@Override
	public <Letter> Postman createPostman(Socket socket, Class<? extends Letter> letterClass) {
		return new ParallelPostman<>(socket, letterClass);
	}

}