package de.teiesti.postie;

import de.teiesti.postie.recipients.Mailbox;
import de.teiesti.postie.serializers.GsonSerializer;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

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

	public void setupStart() {
		setup();
		alice.start();
		bob.start();
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
		bob.start();	// if we don't do this, alice will not close

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
		bob.start();	// if we don't do this, alice will not close

		try {
			alice.use(new GsonSerializer<>(Integer.class));
			fail();
		} catch (IllegalStateException e) {}

		alice.stop();

		alice.use(new GsonSerializer<>(Integer.class));
	}

	@Test
	public void startStopTest() {
		assertThat(alice.isRunning(), is(false));
		assertThat(bob.isRunning(), is(false));

		try {
			alice.start();
			fail();
		} catch (IllegalStateException e) {}

		try {
			bob.start();
			fail();
		} catch (IllegalStateException e) {}

		setup();
		alice.start();
		bob.start();

		assertThat(alice.isRunning(), is(true));
		assertThat(bob.isRunning(), is(true));

		alice.stop();

		assertThat(alice.isRunning(), is(false));
		while (bob.isRunning());	// spinlock that waits for the other thread
		assertThat(bob.isRunning(), is(false));

		try {
			alice.start();
			fail();
		} catch (IllegalStateException e) {}

		try {
			bob.start();
			fail();
		} catch (IllegalStateException e) {}
	}

	@Test
	public void nullSendTest() {
		setupStart();

		try {
			bob.send(null);
			fail();
		} catch (IllegalArgumentException e) {}
	}

	@Test
	public void simplexSendTest() throws InterruptedException{
		setupStart();

		Mailbox<Integer> aliceMailbox = new Mailbox<>();
		alice.register(aliceMailbox);

		bob.send(42);
		assertThat(aliceMailbox.receive(), is(42));
	}

	@Test
	public void halfDuplexSendTest() throws InterruptedException {
		setupStart();

		Mailbox<Integer> aliceMailbox = new Mailbox<>();
		alice.register(aliceMailbox);

		Mailbox<Integer> bobMailbox = new Mailbox<>();
		bob.register(bobMailbox);

		bob.send(1234);
		assertThat(aliceMailbox.receive(), is(1234));
		alice.send(4321);
		assertThat(bobMailbox.receive(), is(4321));
	}

	@Test
	public void fullDuplexSendTest() throws InterruptedException {
		setupStart();

		Mailbox<Integer> aliceMailbox = new Mailbox<>();
		alice.register(aliceMailbox);

		Mailbox<Integer> bobMailbox = new Mailbox<>();
		bob.register(bobMailbox);

		bob.send(1);
		alice.send(2);
		assertThat(bobMailbox.receive(), is(2));
		assertThat(aliceMailbox.receive(), is(1));
	}

	@Test
	public void multiLetterSendTest() throws InterruptedException {
		setupStart();

		Mailbox<Integer> aliceMailbox = new Mailbox<>();
		alice.register(aliceMailbox);

		for (int i = 0 ; i < 1024; i++)
			bob.send(i);

		for (int i = 0; i < 1024; i++)
			assertThat(aliceMailbox.receive(), is(i));
	}

	@Test
	public void multiRecipientSendTest() throws InterruptedException {
		setupStart();

		Mailbox<Integer>[] mailboxes = new Mailbox[100];
		for (int i = 0; i < mailboxes.length; i++) {
			mailboxes[i] = new Mailbox<>();
			alice.register(mailboxes[i]);
		}

		bob.send(42);

		for (int i = 0; i < mailboxes.length; i++)
			assertThat(mailboxes[i].receive(), is(42));
	}

	@After
	public void after() {
		if (alice.isRunning()) alice.stop();
		if (bob.isRunning()) bob.stop();
	}

}
