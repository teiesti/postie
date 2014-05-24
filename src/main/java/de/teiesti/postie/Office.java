package de.teiesti.postie;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link Office} accepts network connection on a given {@link ServerSocket} and spawns {@link Postman} to respond
 * to requests. A running {@link Office} awaits incoming connection, then clones the configured {@link Postman},
 * hands over the connection and starts it. Afterwards it repeats this procedure.<br>
 * <br>
 * To setup an {@link Office} call {@link #spawn(Postman)} with a {@link Postman} that was configured with a {@link
 * Serializer} and all necessary {@link Recipient}s. If a {@link Socket} was bound it will be ignored. There is no
 * trouble if the {@link Postman} is running but the system may behaves strange if the {@link Postman}'s state is
 * changing. In addition, you must call {@link #bind(ServerSocket)} with a {@link ServerSocket}. To complete the
 * setup, call {@link #start()} which starts the required thread. If work is done call {@link #stop()}.
 */
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

	/**
	 * Binds this {@link Office} to a given {@link ServerSocket}. A given {@link ServerSocket} will override a
	 * previously given one because an {@link Office} can only use one {@link ServerSocket} at once.
	 *
	 * @param serverSocket the {@link ServerSocket}
	 *
	 * @return this {@link Office}
	 *
	 * @throws IllegalArgumentException if {@code serverSocket} is {@code null}
	 */
	public final synchronized Office bind(ServerSocket serverSocket) {
		if (serverSocket == null)
			throw new IllegalArgumentException("serverSocket == null");

        this.serverSocket = serverSocket;

		return this;
	}

	/**
	 * Configures the blueprint of {@link Postman} that should be spawned by this {@link Office}. The given {@link
	 * Postman} will be cloned every time this {@link Office} accepts a connection. The given {@link Postman} must be
	 * configured with a {@link Serializer} and all necessary {@link Recipient}s. Otherwise the {@link Office} may
	 * behaves undefined. Please be aware that the correct setup of a given {@link Postman} is not validated since
	 * the {@link Office} was started and accepted a connection.<br>
	 * <br>
	 * This method cannot be called with {@code null} as parameter or if this {@link Postman} is running. In these
	 * cases adequate exceptions are thrown.
	 *
	 * @param blueprint the {@link Postman} to use as blueprint
	 *
	 * @return this {@link Office}
	 *
	 * @throws IllegalArgumentException if {@code blueprint} is {@code null}
	 * @throws IllegalStateException if this {@link Office} is running
	 */
	public final synchronized Office spawn(Postman blueprint) {
        if (isRunning())
			throw new IllegalStateException("cannot configure a blueprint because this is running");
		if (blueprint == null)
			throw new IllegalArgumentException("blueprint == null");

        this.blueprint = blueprint.register(postmanHelper);

		return this;
	}

	/**
	 * Starts this {@code Office}. Before, you must configure a {@code Postman} to spawn and a {@link ServerSocket}
	 * to listen on. Use {@link #spawn(Postman)} and {@link #bind(ServerSocket)} for that.
	 *
	 * @return this {@link Postman}
	 *
	 * @throws IllegalStateException if this {@link Office} is already running
	 */
	public final synchronized Office start() {
		if (isRunning())
			throw new IllegalStateException("cannot start because this is already running");

		// TODO check configuration, how?

		acceptor = new Acceptor();
		acceptor.start();

		return this;
	}

	/**
	 * Stops this {@link Office}. This method is an alias for {@link #stop(boolean)} with {@code false} as parameter.
	 *
	 * @throws IllegalStateException if this {@link Office} is not running
	 */
	public final synchronized Office stop() {
		return stop(false);
    }

	/**
	 * Stops this {@link Office}. This method closes the {@link ServerSocket} and stops the {@link Thread} that accepts
	 * connections and spawns {@link Postman}. If the given parameter is {@code true},
	 * this {@link Office} stops any running {@link Postman} that was spawned by this {@link Office}.
	 *
	 * @param stopPostmen weather to stop spawned {@link Postman}
	 *
	 * @return this {@link Office}
	 *
	 * @throws IllegalStateException if this {@link Office} is not running
	 */
    public final synchronized Office stop(boolean stopPostmen) {
		if (!isRunning())
			throw new IllegalStateException("cannot stop because this is not running");

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

	/**
	 * Returns weather this {@link Office} is running.
	 *
	 * @return if this {@link Office} is running.
	 */
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
