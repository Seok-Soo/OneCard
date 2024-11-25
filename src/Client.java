import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name; // 클라이언트 이름
    private List<Card> hand = new ArrayList<>(); // 플레이어의 손패
    private OneCardGameGUI gui; // GUI 업데이트를 위한 참조

    public Client(String name) {
        this.name = name;
    }

    public void setGUI(OneCardGameGUI gui) {
        this.gui = gui;
    }

    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버에 이름 전송
            out.println(name);

            // 서버 응답 처리 스레드 시작
            new Thread(this::receiveMessages).start();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 서버로 메시지 전송
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // 서버 메시지 수신 및 처리
    // Client.java
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("PLAY_SUCCESS:")) {
                    String cardInfo = message.substring(13); // "PLAY_SUCCESS:" 이후 카드 정보
                    Card playedCard = parseCard(cardInfo); // 카드 객체로 변환
                    hand.remove(playedCard); // 손패에서 제거
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("PLAY_SUCCESS 처리 완료: " + hand); // 디버깅 로그
                        gui.updateHand(hand); // UI 갱신
                    });
                } else if (message.startsWith("HAND_UPDATE:")) {
                    List<Card> updatedHand = parseHand(message.substring(12));
                    updateHand(updatedHand); // 서버에서 받은 손패로 갱신
                } else if (message.startsWith("GAME_STATE:")) {
                    String gameState = message.substring(11);
                    updateGameState(gameState); // 게임 상태 갱신
                } else if (message.startsWith("ERROR:")) {
                    System.out.println("제출 실패: " + message.substring(6)); // 에러 메시지 출력
                } else {
                    System.out.println("서버 메시지: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private Card parseCard(String cardInfo) {
        String[] parts = cardInfo.split(" ");
        return new Card(parts[0], parts[1], ""); // 이미지 경로는 필요 없으므로 빈 문자열
    }

    private List<Card> parseHand(String handData) {
        List<Card> hand = new ArrayList<>();
        String[] cards = handData.split(",");
        for (String card : cards) {
            String[] parts = card.split(" ");
            if (parts.length == 2) {
                hand.add(new Card(parts[0], parts[1], "")); // 이미지 경로는 필요 없으므로 빈 문자열
            }
        }
        return hand;
    }

    // Client.java - updateHand
    public void updateHand(List<Card> newHand) {
        this.hand.clear(); // 기존 손패 비우기
        this.hand.addAll(newHand); // 새 손패 추가

        SwingUtilities.invokeLater(() -> {
            System.out.println("UI 갱신: " + hand); // 디버깅 로그 추가
            gui.updateHand(hand); // GUI 갱신
        });
    }

    // 게임 상태 업데이트
    private void updateGameState(String gameState) {
        SwingUtilities.invokeLater(() -> {
            gui.updateGameState(gameState); // GUI에서 게임 상태 업데이트
        });
    }

    // 사용자 목록 업데이트
    private void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            gui.updatePlayerList(users); // GUI에서 사용자 목록 업데이트
        });
    }

    // 카드 제출 요청
    public void playCard(Card card) {
        if (hand.contains(card)) {
            System.out.println("제출 요청: " + card);
            sendMessage("PLAY_CARD " + card.getRank() + " " + card.getSuit());
            hand.remove(card);
            System.out.println("손패에서 제거됨: " + card);
            SwingUtilities.invokeLater(() -> gui.updateHand(hand)); // UI 갱신
        } else {
            System.out.println("손패에 없는 카드입니다! 제출 실패: " + card);
        }
    }

    public void setGamePanel(OneCardGameGUI gamePanel) {
        this.gui = gamePanel;
    }

    public void addCard(Card card) {
        hand.add(card);
        SwingUtilities.invokeLater(() -> {
            gui.updateHand(hand); // GUI 갱신
        });
    }

    public List<Card> getHand() {
        return hand;
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
