import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

  private ServerSocket serverSocket;
  private ArrayList<Socket> waitingSocket;

  private int s;

  public Server() throws IOException {
    this.waitingSocket = new ArrayList<>();
    serverSocket = new ServerSocket(10086);
  }

  public void run() {
    while (true) {
      Socket socket = null;
      try {
        socket = serverSocket.accept();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      waitingSocket.add(socket);
      if (waitingSocket.size() == 2) {
        ServerThread serverThread = new ServerThread(waitingSocket.get(0), waitingSocket.get(1),
            serverSocket);
        Thread t = new Thread(serverThread);
        waitingSocket.clear();
        t.start();
      } else if (waitingSocket.size() == 1) {
        try {
          PrintWriter writer = new PrintWriter(waitingSocket.get(0).getOutputStream());
          writer.println("Waiting for another player");
          writer.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    Server server = new Server();
    server.run();
  }
}