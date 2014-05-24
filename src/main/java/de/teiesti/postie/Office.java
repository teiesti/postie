package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Office {

	private ServerSocket serverSocket;

	private Postman blueprint;

	private Set<Postman> postmen = Collections.synchronizedSet(new HashSet<Postman>());

	private final Recipient postmanHelper = new Recipient() {
		@Override
		public void accept(Object o, Postman from) {
		}

		@Override
		public void acceptedLast(Postman from) {
			postmen.remove(from);
		}
	};

	private Thread acceptor;

	public final synchronized Office bind(ServerSocket serverSocket) {
		// FIXME check consistency

        this.serverSocket = serverSocket;

		return this;
	}

	public final synchronized Office spawn(Postman blueprint) {
        // FIXME check consistency

        this.blueprint = blueprint.register(postmanHelper);

		return this;
	}

	public final synchronized Office start() {
		acceptor = new Acceptor();
		acceptor.start();

		return this;
	}

	public final synchronized Office stop() {
        return stop(false);
    }

    public final synchronized Office stop(boolean stopPostmen) {
        try {
            serverSocket.close();
            acceptor.join();
        } catch (IOException | InterruptedException e) {
            Logger.error(e);
            System.exit(1);
        }

        acceptor = null;

        if (stopPostmen)
			synchronized (postmen) {
				for (Postman p : postmen) {
					p.unregister(postmanHelper);
					p.stop();
				}
				postmen.clear();
			}

        return this;
    }

    public final boolean isRunning() {
        return acceptor != null && acceptor.isAlive();
    }

	private class Acceptor extends Thread {

	 	@Override
		public void run() {
			Socket socket;
			Postman postman;
			while (true) {
				try {
					socket = serverSocket.accept();
					postman = blueprint.clone().bind(socket);
					postmen.add(postman);
                    postman.start();
				} catch (IOException | CloneNotSupportedException e) {
					if (e instanceof SocketException)
                        break;

                    Logger.error(e);
					System.exit(1);
				}
			}

		}

	}

}
