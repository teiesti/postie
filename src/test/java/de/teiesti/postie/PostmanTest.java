package de.teiesti.postie;

import de.teiesti.postie.recipients.Mailbox;
import de.teiesti.postie.serializers.GsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.net.Socket;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public abstract class PostmanTest {

	protected Postman<Integer> alice;
	protected Postman<Integer> bob;

	public abstract <Letter> Postman<Letter> createPostman();

    @Rule
    public Timeout timeout = new Timeout(1000);

	@Before
	public void before() {
		alice = createPostman();
		bob = createPostman();
	}

	public void setup() {
		alice.use(new GsonSerializer(Integer.class));
		bob.use(new GsonSerializer(Integer.class));

		Socket[] twin = null;
		try {
			twin = SocketTwin.create();
		} catch (IOException | InterruptedException e) {
			fail("could not create socket twin");
		}

		alice.bind(twin[0]);
		bob.bind(twin[1]);
	}

	@Test
	public void bindTest() {
		alice.bind(new Socket());
		alice.bind(new Socket());

		// TODO test some socket that should not be bound --> closed socket, ...

		try {
			alice.bind(null);
			fail();
		} catch(IllegalArgumentException e) {}

		setup();
		alice.start();
		bob.start();

		try {
			alice.bind(new Socket());
			fail();
		} catch (IllegalStateException e) {}

		alice.stop();

		alice.bind(new Socket());
	}

	@Test
	public void useTest() {
		alice.use(new GsonSerializer<>(Integer.class));
		alice.use(new GsonSerializer<>(Integer.class));

		try {
			alice.bind(null);
			fail();
		} catch(IllegalArgumentException e) {}

		setup();
		alice.start();
		bob.start();

		try {
			alice.use(new GsonSerializer<>(Integer.class));
			fail();
		} catch (IllegalStateException e) {}

		alice.stop();

		alice.use(new GsonSerializer<>(Integer.class));
	}

	// TODO test start, send, receive, stop, ...

}
