package application.controller;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.text.Text;

public class Controller implements Initializable {

  private static final int PLAY_1 = 1;
  private static final int PLAY_2 = 2;
  private static final int EMPTY = 0;
  private static final int BOUND = 90;
  private static final int OFFSET = 15;

  private int pid;

  private PrintWriter pw;

  private BufferedReader bufferedReader;

  @FXML
  private Pane base_square;

  @FXML
  private Rectangle game_panel;

  @FXML
  private Text information;

  private String text = "";
  private static boolean TURN = false;

  private static final int[][] chessBoard = new int[3][3];
  private static final boolean[][] flag = new boolean[3][3];

  public Controller() {
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.information = new Text();
    base_square.getChildren().add(information);
    Socket socket;
    try {
      socket = new Socket("localhost", 10086);
    } catch (IOException e) {
      System.err.println("连接失败");
      throw new RuntimeException(e);
    }
    InputStream is = null;
    try {
      is = socket.getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    OutputStream os = null;
    try {
      os = socket.getOutputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    pw = new PrintWriter(os);
    bufferedReader = new BufferedReader(new InputStreamReader(is));

    Thread t = new Thread(() -> {
      while (true) {
        String msgFromServer = null;
        try {
          msgFromServer = bufferedReader.readLine();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (msgFromServer.contains("Player")) {
          System.out.println(msgFromServer);
          this.pid = msgFromServer.charAt(msgFromServer.length() - 1) - '0';
          information.setText("Player" + pid);
          continue;
        } else if (msgFromServer.equals("Waiting for another player")) {
          System.out.println(msgFromServer);
          continue;
        } else if (msgFromServer.contains(":")) {
//          System.out.println(msgFromServer);
          System.out.println("please input your position: ");
          text = "please input your position";
          information.setText("Your turn");
        } else if (msgFromServer.contains(",")) {
          String finalMsgFromServer = msgFromServer;
          Platform.runLater(() -> {
            refreshBoard(Integer.parseInt(finalMsgFromServer.split(",")[0]), Integer.parseInt(
                finalMsgFromServer.split(",")[1]));
            TURN = !TURN;
          });
          pw.println("My turn!");
          pw.flush();
        } else {
          System.out.println(msgFromServer);
          String finalMsgFromServer1 = "end";
          Platform.runLater(() -> {
            information.setText(finalMsgFromServer1);
          });
        }
      }
    });
    t.start();

    game_panel.setOnMouseClicked(event -> {
      if (text.equals("please input your position")) {
        int x = (int) (event.getX() / BOUND);
        int y = (int) (event.getY() / BOUND);
        if (refreshBoard(x, y)) {
          TURN = !TURN;
          information.setText("Your opponent's turn");
        }
        String sendMsg = x + "," + y;
        pw.println(sendMsg);
        pw.flush();
//        if (pid == 1) {
//          drawLine(x, y);
//        } else if (pid == 2)
//          drawCircle(x, y);
      }
    });
  }

  private boolean refreshBoard(int x, int y) {
    if (chessBoard[x][y] == EMPTY) {
      chessBoard[x][y] = TURN ? PLAY_1 : PLAY_2;
      drawChess();
      return true;
    }
    return false;
  }

  private void drawChess() {
    for (int i = 0; i < chessBoard.length; i++) {
      for (int j = 0; j < chessBoard[0].length; j++) {
        if (flag[i][j]) {
          // This square has been drawing, ignore.
          continue;
        }
        switch (chessBoard[i][j]) {
          case PLAY_1:
            drawCircle(i, j);
            break;
          case PLAY_2:
            drawLine(i, j);
            break;
          case EMPTY:
            // do nothing
            break;
          default:
            System.err.println("Invalid value!");
        }
      }
    }
  }

  private void drawCircle(int i, int j) {
    Circle circle = new Circle();
    base_square.getChildren().add(circle);
    circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
    circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
    circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
    circle.setStroke(Color.RED);
    circle.setFill(Color.TRANSPARENT);
    flag[i][j] = true;
  }

  private void drawLine(int i, int j) {
    Line line_a = new Line();
    Line line_b = new Line();
    base_square.getChildren().add(line_a);
    base_square.getChildren().add(line_b);
    line_a.setStartX(i * BOUND + OFFSET * 1.5);
    line_a.setStartY(j * BOUND + OFFSET * 1.5);
    line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
    line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
    line_a.setStroke(Color.BLUE);

    line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
    line_b.setStartY(j * BOUND + OFFSET * 1.5);
    line_b.setEndX(i * BOUND + OFFSET * 1.5);
    line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
    line_b.setStroke(Color.BLUE);
    flag[i][j] = true;
  }

  class Player {

    private Socket socket;
    private int pid;
    private PrintWriter pw;

    private BufferedReader bufferedReader;

    public Player(Socket socket) throws IOException {
      this.socket = socket;
      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      pw = new PrintWriter(os);
      bufferedReader = new BufferedReader(new InputStreamReader(is));
    }

    public void run() {
      while (true) {
        String msgFromServer = null;
        try {
          msgFromServer = bufferedReader.readLine();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (msgFromServer.contains("Player")) {
          System.out.println(msgFromServer);
          this.pid = msgFromServer.charAt(msgFromServer.length() - 1);
          System.out.println(pid - '0');
          continue;
        } else if (msgFromServer.equals("Waiting for another player")) {
          System.out.println(msgFromServer);
          continue;
        }
//          } else if (msgFromServer.contains("请")) {
//            System.out.println(msgFromServer);
//            int x = input.nextInt();
//            int y = input.nextInt();
//            String sendMsg = x + "," + y;
//            refreshBoard(x, y);
//            pw.println(sendMsg);
//            pw.flush();
//            if (pid == 1) {
//              drawLine(x, y);
//            } else
//              drawCircle(x, y);
//          }
        else if (msgFromServer.contains(",")) {
          int x = Integer.parseInt(msgFromServer.split(",")[0]);
          int y = Integer.parseInt(msgFromServer.split(",")[1]);
          if (pid == 1) {
            drawCircle(x, y);
          } else {
            drawLine(x, y);
          }
        }
      }
    }
  }
}
