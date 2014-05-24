package de.teiesti.postie;

import de.teiesti.postie.postmen.SequentialPostman;
import de.teiesti.postie.recipients.Mailbox;
import de.teiesti.postie.serializers.GsonSerializer;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class OfficeTest {

	public static int port = 2103;

	private Office olaf;

	private Postman aliceBlueprint;
	private Mailbox aliceMailbox = new Mailbox();

	@Rule
	public Timeout timeout = new Timeout(1000);

	private Postman createBob(int port) throws IOException {
		Postman bob = new SequentialPostman();
		bob.use(new GsonSerializer(Integer.class));
		return bob.bind(new Socket("localhost", port));
	}

	@Before
	public void before() throws IOException {
		aliceBlueprint = new SequentialPostman();
		aliceBlueprint.use(new GsonSerializer(Integer.class));
		aliceBlueprint.register(aliceMailbox);

		olaf = new Office();
		olaf.spawn(aliceBlueprint);
		olaf.bind(new ServerSocket(port));
		olaf.start();
	}

	@Test
	public void connectDisconnectTest() throws IOException {
		createBob(port).start().stop();

		Postman[] bobs = new Postman[42];
		for (int i = 0; i < bobs.length; i++)
			bobs[i] = createBob(port).start();

		for (int i = 0; i < bobs.length; i++)
			bobs[i].stop();
	}

	@Test
	public void sendTest() throws IOException, InterruptedException {
		Postman bob1 = createBob(port).start();
		Postman bob2 = createBob(port).start();

		bob1.send(1);
		bob2.send(2);
		bob1.send(3);

		Set<Integer> results = new HashSet<>();
		results.add(1);
		results.add(2);
		results.add(3);

		for (int i = 0; i < 3; i++)
			assertThat(results.remove(aliceMailbox.receive()), is(true));

		assertThat(results.isEmpty(), is(true));
	}

	@After
	public void after() {
		olaf.stop(true);
	}

}
