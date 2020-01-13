import java.io.*;
import java.net.*;

public class Client {
	private static final int PORT = 8080;
	private static final String HOST = "localhost";
	private static ReceiverThread receiverThread;

	private class ReceiverThread extends Thread {

		private DataInputStream dataInputStream;
		private DataOutputStream dataOutputStream;

		public void setParams(Socket clientSocket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
			this.dataInputStream = dataInputStream;
			this.dataOutputStream = dataOutputStream;
		}

		@Override
		public void run() {
			while (true) {
				try {
					dataOutputStream.writeUTF("UPDATE");
					System.out.println(dataInputStream.readUTF());
				} catch (java.net.SocketException e) {
					System.out.println("Connection has been lost");
					System.exit(3);
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}

	public static void main(String[] args) {
		new Client().start(args);
	}

	public void start(String[] args) {
		Socket clientSocket = null;
		DataInputStream dataInput = null;
		DataOutputStream dataOutput = null;
		System.out.println("Connecting to: " + HOST + ", port: " + PORT + "...");
		try {
			clientSocket = new Socket(HOST, PORT);
			dataInput = new DataInputStream(clientSocket.getInputStream());
			dataOutput = new DataOutputStream(clientSocket.getOutputStream());
			receiverThread = new ReceiverThread();
			receiverThread.setParams(clientSocket, dataInput, dataOutput);
			receiverThread.start();
			System.out.println("Connected.");
		} catch (ConnectException e) {
			System.out.println("Server is unavailable");
		} catch (SocketException e) {
			System.out.println("Connection has been lost");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(receiverThread != null)
				try {
					receiverThread.interrupt();
				} catch(Exception e) {} 
			}
		}
}