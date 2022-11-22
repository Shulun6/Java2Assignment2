import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class ServerThread implements Runnable {
  private Object obj = new Object();
  private Socket socketPlayer1;
  private Socket socketPlayer2;

  private ServerSocket serverSocket;

  //1代表玩家1 -1代表玩家2
  private int[][] chessBoard;


  public ServerThread(Socket player1, Socket player2, ServerSocket serverSocket){
    this.socketPlayer1 = player1;
    this.socketPlayer2 = player2;
    this.serverSocket = serverSocket;
    this.chessBoard = new int[3][3];
  }

  public int judgeWinner(){
    if (this.chessBoard[0][0] == this.chessBoard[0][1] && this.chessBoard[0][1] == this.chessBoard[0][2] && this.chessBoard[0][0]!=0){
      return this.chessBoard[0][0];
    }
    else if (this.chessBoard[0][0] == this.chessBoard[1][0] && this.chessBoard[1][0] == this.chessBoard[2][0]
        && this.chessBoard[0][0]!=0){
      return this.chessBoard[0][0];
    }
    else if (this.chessBoard[0][0] == this.chessBoard[1][1] && this.chessBoard[1][1] == this.chessBoard[2][2]
        && this.chessBoard[0][0]!=0){
      return this.chessBoard[0][0];
    }
    else if (this.chessBoard[0][2] == this.chessBoard[1][1] && this.chessBoard[1][1] == this.chessBoard[2][0]
        && this.chessBoard[0][2]!=0){
      return this.chessBoard[0][2];
    }
    else if (this.chessBoard[0][2] == this.chessBoard[1][2] && this.chessBoard[1][2] == this.chessBoard[2][2]
        && this.chessBoard[0][2]!=0){
      return this.chessBoard[0][2];
    }
    else if (this.chessBoard[2][0] == this.chessBoard[2][1] && this.chessBoard[2][1] == this.chessBoard[2][2]
        && this.chessBoard[2][0]!=0){
      return this.chessBoard[2][0];
    }
    else if (this.chessBoard[1][0] == this.chessBoard[1][1] && this.chessBoard[1][1] == this.chessBoard[1][2]
        && this.chessBoard[1][0]!=0){
      return this.chessBoard[1][0];
    }
    else if (this.chessBoard[0][1] == this.chessBoard[1][1] && this.chessBoard[1][1] == this.chessBoard[2][1]
        && this.chessBoard[0][1]!=0){
      return this.chessBoard[0][1];
    }
    else return 0;
  }

  public boolean isFull(){
    for (int i = 0;i < 3;i++){
      for (int j = 0;j < 3;j++){
        if (chessBoard[i][j] == 0){
          return false;
        }
      }
    }
    return true;
  }

  public void sendMsg(Socket socket, String msg) throws IOException {
    PrintWriter pw = new PrintWriter(socket.getOutputStream());
    pw.println(msg);
    pw.flush();
  }

  public String receiveMsg(Socket socket) throws IOException {
    InputStream is = null;
    try {
      is = socket.getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
    return bufferedReader.readLine();
  }

  public void changeBoard(int x, int y, int player){
      this.chessBoard[x][y] = player;
  }

  @Override
  public void run() {
    try {
      sendMsg(socketPlayer1,"Player1");
      sendMsg(socketPlayer2,"Player2");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    PlayerThread p1 = new PlayerThread(socketPlayer1, 1, socketPlayer2);
    PlayerThread p2 = new PlayerThread(socketPlayer2, 2, socketPlayer1);

    Thread t1 = new Thread(p1);
    Thread t2 = new Thread(p2);

    t1.start();
    t2.start();
  }

  class PlayerThread implements Runnable{
    private boolean currentPlayer;
    private Socket socket1;
    private Socket opponentSocket;
    private int pid;
    public PlayerThread(Socket socket, int pid, Socket opponentSocket){
      this.socket1 = socket;
      this.pid = pid;
      this.opponentSocket = opponentSocket;
      if (pid == 1){
      currentPlayer = true;
      }
      else currentPlayer = false;
    }
    @Override
    public void run() {
        while (true){
          //player 为行动玩家
          if (this.currentPlayer){
            //服务器向玩家发送提示信息 请玩家输入坐标
            String sendToP = "请输入坐标:";
            try {
              sendMsg(this.socket1, sendToP);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            //服务器接收玩家发来的消息 根据这个消息更改棋盘
            String receiveFromPlayer = "";
            try {
              receiveFromPlayer = receiveMsg(this.socket1);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }

            //把我方下棋的信息告诉对手 让其更新界面
            try {
              sendMsg(opponentSocket, receiveFromPlayer);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }

            //判断信息是否导致终局
            String[] pos = receiveFromPlayer.split(",");
            //处理接收过来的信息
            int y = Integer.parseInt(pos[0]);
            int x = Integer.parseInt(pos[1]);
            //位置合理的情况
            if (chessBoard[x][y] == 0){
              changeBoard(x, y, pid);
              //current下完这一步之后没有赢家
              System.out.println("服务器更改棋盘:");
              System.out.println(Arrays.deepToString(chessBoard));
              if (judgeWinner() == 0){
                if (!isFull()){
                  currentPlayer = !currentPlayer;
                }
                else {
                  String tieMsg = "Tie!";
                  try {
                    sendMsg(socket1, tieMsg);
                    sendMsg(opponentSocket, tieMsg);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                }
              }
              //current下完这一步之后有赢家 赢家只能是current
              else {
                String winMsg = "You win!";
                String loseMsg = "You lose!";
                try {
                  sendMsg(socket1, winMsg);
                  sendMsg(opponentSocket, loseMsg);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
            }
          }

          //player不是current的时候
          else {
            String msg;
            try {
              msg = receiveMsg(socket1);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            if (msg.equals("My turn!")) {
              this.currentPlayer = !this.currentPlayer;
            }
          }
        }
      }
    }
  }
