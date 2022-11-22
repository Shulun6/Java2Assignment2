import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Player implements Runnable{
  private Scanner input = new Scanner(System.in);
  private Socket socket;
  private int pid;
  private PrintWriter pw;

  private BufferedReader bufferedReader;
  public Player(int pid) throws IOException {
      this.socket = new Socket("localhost",10086);
      this.pid = pid;
      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      pw = new PrintWriter(os);
      bufferedReader = new BufferedReader(new InputStreamReader(is));
  }

  @Override
  public void run() {
    while (true){
      String msgFromServer = null;
      try {
        msgFromServer = bufferedReader.readLine();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (msgFromServer.equals("Player"+pid)){
        System.out.println(msgFromServer);
        continue;
      }

      else if (msgFromServer.equals("Waiting for another player")){
        System.out.println(msgFromServer);
        continue;
      }

      else if (msgFromServer.equals("请输入坐标：")){
        int x = input.nextInt();
        int y = input.nextInt();
        String sendMsg = x+","+y;
        pw.println(sendMsg);
        pw.flush();
      }

      else {
        System.out.println(msgFromServer);
      }

    }
  }
}
