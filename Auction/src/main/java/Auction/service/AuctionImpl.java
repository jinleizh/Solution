package Auction.service;

import Auction.Auction;
import Auction.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 16-8-20.
 */
public class    AuctionImpl implements Auction {
    private int counter = 0;
    private List<Player> playerList = new ArrayList<>();

    /**
     * 注册参与竞拍的人
     * @param player 竞拍人
     */
    public void registePlayer(Player player) {
        if(!playerList.contains(player)) {
            playerList.add(player);
        }
    }

    /**
     * 竞拍
     */
    @Override
    public void doAuction() {
        System.out.println("doAction begin " + counter);
        for (int i = 0; i < playerList.size(); i++) {
            playerList.get(i).offerPrice();
        }
        System.out.println("doAction end " + counter);

        int maxPrice = 0;
        int playerIndex = 0;
        for(int i = 0; i < playerList.size(); i++) {
            Player player = playerList.get(i);
            try {
                player.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(player.getPrice() > maxPrice) {
                playerIndex = i;
                maxPrice = player.getPrice();
            }
        }

        Player player = playerList.get(playerIndex);
        System.out.println(player.getPlayerName() + "以" + player.getPrice() + "价格成交.");
    }

    public static void test() {
        AuctionImpl auction = new AuctionImpl();
        for(int i = 0; i < 4; i++) {
            Player player = new Player("Thread" + i);
            auction.registePlayer(player);
        }
        auction.doAuction();
    }
}
