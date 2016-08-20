package Auction.player;

import java.util.Random;

/**
 * 竞拍者
 * Created by Administrator on 16-8-20.
 */
public class Player extends Thread {
    private final static int MAX_COUNTER = 8;
    private String playerName;
    private int price;
    private boolean isEnd;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Player(String playerName) {
        this.playerName = playerName;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player)) return false;

        Player player = (Player) o;

        return getPlayerName().equals(player.getPlayerName());

    }

    @Override
    public int hashCode() {
        return getPlayerName().hashCode();
    }

    public void offerPrice() {
        System.out.println(playerName + "offer price begin");
        start();
    }

    @Override
    public void run() {
        try {
            int counter = 0;
            Random random = new Random();
            while(++counter <= MAX_COUNTER) {
                price = random.nextInt(100);
                System.out.println(counter + "轮:" + playerName + " offer price " + price + ".............");
                if(counter != MAX_COUNTER) {
                    sleep(3000);
                }
            }
            System.out.println(playerName + "offer price end");
        } catch (InterruptedException e) {
            System.out.println("thread " + playerName + " interrupted");
        }

        isEnd = true;
    }
}
