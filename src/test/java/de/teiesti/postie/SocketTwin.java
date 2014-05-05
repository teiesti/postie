package de.teiesti.postie;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class SocketTwin {

	private static int port = 2103;

	public static Socket[] create() throws IOException, InterruptedException {
		SocketAcceptor aliceCreator = new SocketAcceptor(port);

		Socket bob = new Socket("localhost", port++);
		Socket alice = aliceCreator.getSocket();

		return new Socket[] { alice, bob };
	}

	private static class SocketAcceptor extends Thread {

		private ServerSocket serverSocket;
		private Socket socket;

		private IOException e;

		public SocketAcceptor(int port) throws IOException {
			serverSocket = new ServerSocket(port);
			this.start();
		}

		@Override
		public void run() {
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				this.e = e;
			}
		}

		public Socket getSocket() throws InterruptedException, IOException{
			this.join();
			if (e != null) throw e;
			return socket;
		}
	}

}
