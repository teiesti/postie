package de.teiesti.postie;

import org.junit.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class PostmanTest {

    private static Postman alice;
    private static Postman bob;

    @BeforeClass
    public static void setup() {
        Thread createAlice = new Thread() {
           	@Override
            public void run() {
                try {
                    Socket aliceSocket = new ServerSocket(2804).accept();
                    alice = new Postman(aliceSocket, Integer.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("could not create alice");
                }
            }
        };

        createAlice.start();

        try {
            Socket bobSocket = new Socket(InetAddress.getLocalHost(), 2804);
            bob = new Postman(bobSocket, Integer.class);
        } catch (IOException e) {
            e.printStackTrace();
            fail("could not create bob");
        }

        try {
            createAlice.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(timeout = 500)
    public void simplexSendTest() {
        bob.send(42);
        assertThat((Integer) alice.receive(), is(42));
    }

    @Test(timeout=500)
    public void halfDuplexSendTest() {
        bob.send(1234);
        assertThat((Integer) alice.receive(), is(1234));
        alice.send(4321);
        assertThat((Integer) bob.receive(), is(4321));
    }

    @Test(timeout = 500)
    public void fullDuplexSendTest() {
        bob.send(1);
        alice.send(2);
        assertThat((Integer) bob.receive(), is(2));
        assertThat((Integer) alice.receive(), is(1));
    }

	@Test(timeout = 500)
	public void multiSendTest() {
		for (int i = 0 ; i < 1024; i++)
			bob.send(i);

		for (int i = 0; i < 1024; i++)
			assertThat((Integer) alice.receive(), is(i));
	}

	@Test(timeout = 500)
	public void nullSendTest() {
		try {
			bob.send(null);
			fail();
		} catch (IllegalArgumentException e) {}
	}

	@Test(timeout = 500)
	public void wrongTypeSendTest() {
		try {
			bob.send(1.0F);
			fail();
		} catch (ClassCastException e) {}
	}

    @AfterClass
    public static void cleanup() throws Exception {
        alice.close();
        bob.close();
    }

}
