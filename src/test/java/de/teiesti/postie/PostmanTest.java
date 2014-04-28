package de.teiesti.postie;

import org.junit.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class PostmanTest {

    private Postman alice;
    private Postman bob;

    @Before
    public void setup() {
        Thread createAlice = new Thread() {
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

    @Test(timeout=5000)
    public void simplexSendTest() {
        bob.send(42);
        assertThat((Integer) alice.receive(), is(42));
    }

    @Test(timeout=5000)
    public void halfDuplexSendTest() {
        bob.send(1234);
        assertThat((Integer) alice.receive(), is(1234));
        alice.send(4321);
        assertThat((Integer) bob.receive(), is(4321));
    }

    @Test(timeout=5000)
    public void fullDuplexSendTest() {
        bob.send(1);
        alice.send(2);
        assertThat((Integer) bob.receive(), is(2));
        assertThat((Integer) alice.receive(), is(2));
    }

    @After
    public void cleanup() throws Exception {
        alice.close();
        bob.close();
    }

}
