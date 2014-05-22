package de.teiesti.postie.postmen;

import de.teiesti.postie.Postman;
import de.teiesti.postie.PostmanTest;
import de.teiesti.postie.recipients.Mailbox;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ParallelPostmanTest extends PostmanTest {

	@Override
	public <Letter> Postman<Letter> createPostman() {
		return new ParallelPostman<Letter>().observeLetterOrder(false);
	}

	@Test
	@Override
	// The unordered ParallelPostman does not guarantee any order. Therefore we must change this test.
	public void multiLetterSendTest() throws InterruptedException {
		setupStart();

		Mailbox<Integer> aliceMailbox = new Mailbox<>();
		alice.register(aliceMailbox);

		Set<Integer> sent = new HashSet<>(1024);
		for (int i = 0 ; i < 1024; i++) {
			bob.send(i);
			sent.add(i);
		}

		for (int i = 0; i < 1024; i++) {
			assertThat(sent.remove(aliceMailbox.receive()), is(true));
		}

		assertThat(sent.isEmpty(), is(true));
	}

}
