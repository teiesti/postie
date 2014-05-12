package de.teiesti.postie.old;

import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class MailboxTest {

    private Mailbox alice;
    private Mailbox bob;

	@Rule
	public Timeout timeout = new Timeout(1000);

	@Before
	public void create() throws IOException, InterruptedException {
		Socket[] pair = SocketTwin.create();
		alice = new Mailbox(pair[0]);
		bob = new Mailbox(pair[1]);
	}

    @Test
    public void simplexSendTest() throws InterruptedException {
        bob.send(42);
        assertThat((Integer) alice.receive(Integer.class), is(42));
    }

    @Test
    public void halfDuplexSendTest() throws InterruptedException {
        bob.send(1234);
        assertThat((Integer) alice.receive(Integer.class), is(1234));
        alice.send(4321);
        assertThat((Integer) bob.receive(Integer.class), is(4321));
    }

    @Test
    public void fullDuplexSendTest() throws InterruptedException {
        bob.send(1);
        alice.send(2);
        assertThat((Integer) bob.receive(Integer.class), is(2));
        assertThat((Integer) alice.receive(Integer.class), is(1));
    }

	@Test
	public void multiSendTest() throws InterruptedException {
		for (int i = 0 ; i < 1024; i++)
			bob.send(i);

		for (int i = 0; i < 1024; i++)
			assertThat((Integer) alice.receive(Integer.class), is(i));
	}

	@Test
	public void nullSendTest() {
		try {
			bob.send(null);
			fail();
		} catch (IllegalArgumentException e) {}
	}

	@Test
	public void hasLetterTest() throws InterruptedException {
		assertThat(alice.hasLetter(), is(false));

		bob.send(42);
		while(!alice.hasLetter());	// spinlock that waits until the letter has passed the concurrent threads
		assertThat(alice.hasLetter(), is(true));

		alice.receive(Integer.class);
		assertThat(alice.hasLetter(), is(false));
	}

    @After
    public void cleanup() throws Exception {
		alice.close();
		bob.close();
	}

}