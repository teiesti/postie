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

	private Postman alice;
	private Mailbox aliceMailbox;

	private Postman bob;
	private Mailbox bobMailbox;

	public abstract <Letter> Postman<Letter> createPostman();

    @Rule
    public Timeout timeout = new Timeout(1000);

	@Before
	public void setup() throws IOException, InterruptedException {
		Socket[] pair = SocketTwin.create();

        alice.use(new GsonSerializer(Integer.class));
        bob.use(new GsonSerializer(Integer.class));

        alice.bind(pair[0]);
        bob.bind(pair[1]);

        alice.register(aliceMailbox);
        alice.register(bobMailbox);

        alice.start();
        bob.start();
	}

    @Test
    public void simplexSendTest() throws InterruptedException {
        bob.send(42);
        assertThat((Integer) aliceMailbox.receive(), is(42));
    }

    @Test
    public void halfDuplexSendTest() throws InterruptedException {
        bob.send(1234);
        assertThat((Integer) aliceMailbox.receive(), is(1234));
        alice.send(4321);
        assertThat((Integer) bobMailbox.receive(), is(4321));
    }

    @Test
    public void fullDuplexSendTest() throws InterruptedException {
        bob.send(1);
        alice.send(2);
        assertThat((Integer) bobMailbox.receive(), is(2));
        assertThat((Integer) aliceMailbox.receive(), is(1));
    }

    @Test
    public void multiSendTest() throws InterruptedException {
        for (int i = 0 ; i < 1024; i++)
            bob.send(i);

        for (int i = 0; i < 1024; i++)
            assertThat((Integer) aliceMailbox.receive(), is(i));
    }

    @Test
    public void nullSendTest() {
        try {
            bob.send(null);
            fail();
        } catch (IllegalArgumentException e) {}
    }

    // TODO add something like configuration tests

	@After
	public void cleanup() throws Exception {
		alice.stop();
        bob.stop();
	}

}
