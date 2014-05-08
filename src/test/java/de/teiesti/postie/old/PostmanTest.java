package de.teiesti.postie.old;

import org.junit.*;

import java.io.IOException;
import java.net.Socket;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class PostmanTest {

	private Postman alice;
	//private TestRecipient aliceRecipient;

	private Postman bob;
	//private TestRecipient bobRecipient;

	public abstract <Letter> Postman createPostman(Socket socket, Class<? extends Letter> letterClass);

	@Before
	public void setup() throws IOException, InterruptedException {
		Socket[] pair = SocketTwin.create();
		alice = createPostman(pair[0], Integer.class);
		bob = createPostman(pair[1], Integer.class);
	}

	@Test(timeout = 500)
	public void fooTest() throws InterruptedException {
		// TODO add some useful tests instead of this
		// TODO maybe you'll need some kind of TestRecipient to get results
	}

	@After
	public void cleanup() throws Exception {
		this.alice.close();
		this.bob.close();
	}

}
