package view.BattleView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.card.CardType;
import models.comperessedData.*;
import models.game.GameActions;
import models.game.map.Position;

import java.util.ArrayList;

public class BattleSceneTester extends Application implements GameActions {
    private final int playerNumber = 1;
    private final int oppPlayerNumber = 2;
    private BattleScene battleScene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setResizable(false);
        primaryStage.setFullScreen(true);
        battleScene = new BattleScene(this, generateAGame(), playerNumber);
        Scene scene = new Scene(battleScene.root, Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private CompressedGame generateAGame() {
        final CompressedCell[][] cells = new CompressedCell[5][9];
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 9; i++) {
                cells[j][i] = new CompressedCell(j, i, null, j + i);
            }
        }
        final CompressedPlayer player1 = new CompressedPlayer("Ali", 2, new ArrayList<>(), null,
                null, null, 1, 0, null, null);
        final CompressedPlayer player2 = new CompressedPlayer("Ali1", 1, new ArrayList<>(), null,
                null, null, 2, 0, null, null);
        final CompressedPlayer myPlayer;
        if (playerNumber == 1)
            myPlayer = player1;
        else
            myPlayer = player2;
        final ArrayList<CompressedTroop> troops = new ArrayList<>();
        final CompressedGameMap map = new CompressedGameMap(cells, troops);
        final CompressedGame game = new CompressedGame(player1, player2, map, 7, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CompressedCard card = new CompressedCard("generalspell_f5_overload", null, "a1", CardType.SPELL,
                        null, 0, 0, 0, null, 2, true);
                myPlayer.addCardToNext(card);
                game.gameUpdate(10, 1, 0, 2, 0);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myPlayer.addNextCardToHand();
                myPlayer.removeCardFromNext();
                game.gameUpdate(11, 4, 0, 4, 0);
                card = new CompressedCard("boss_andromeda", null, "a1", CardType.MINION,
                        null, 0, 0, 0, null, 2, true);
                CompressedTroop troop = new CompressedTroop(card, 5, 6, 5, new Position(2, 2),
                        true, true, false, false, 1, 1);
                myPlayer.addCardToNext(card);
                map.updateTroop(troop);
                card = new CompressedCard("boss_andromeda", null, "a3", CardType.HERO,
                        new CompressedSpell("a", null, null, 0, 2, 3),
                        0, 0, 0, null, 2, true);
                troop = new CompressedTroop(card, 5, 6, 5, new Position(1, 3),
                        true, true, false, false, 1, 1);
                map.updateTroop(troop);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myPlayer.addNextCardToHand();
                card = new CompressedCard("boss_chaosknight", null, "a2", null,
                        null, 0, 0, 0, null, 2, true);
                troop = new CompressedTroop(card, 5, 6, 5, new Position(4, 4),
                        true, true, false, false, 1, 2);
                map.updateTroop(troop);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                troop = new CompressedTroop(card, 5, 6, 5, new Position(4, 8),
                        true, true, false, false, 1, 2);
                map.updateTroop(troop);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                troop = new CompressedTroop(card, 5, 6, 5, new Position(1, 4),
                        true, true, false, false, 1, 2);
                map.updateTroop(troop);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return game;
    }

    public void sendMessage(String string) {
        System.out.println(string);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void attack(CompressedTroop selectedTroop, CompressedTroop troop) {
        battleScene.getMapBox().getTroopAnimationHashMap().get(selectedTroop).attack(troop.getPosition().getColumn());
        System.out.println("Attack");
    }

    @Override
    public void comboAttack(ArrayList<CompressedTroop> comboTroops, CompressedTroop troop) {
        for (CompressedTroop comboAttacker : comboTroops)
            battleScene.getMapBox().getTroopAnimationHashMap().get(comboAttacker).attack(troop.getPosition().getColumn());
        System.out.println("Attack Combo");
    }

    @Override
    public void move(CompressedTroop selectedTroop, int j, int i) {
        battleScene.getMapBox().getGameMap().updateTroop(new CompressedTroop(selectedTroop, j, i));
    }

    @Override
    public void endTurn() {
        battleScene.getGame().gameUpdate(battleScene.getGame().getTurnNumber() + 1, 3,
                0, 3, 0);
        System.out.println("end turn");
        System.out.println("new turn:" + battleScene.getGame().getTurnNumber());
    }

    @Override
    public void insert(CompressedCard compressedCard, int row, int column) {
        System.out.println("insert");
    }

    @Override
    public void useSpecialPower(int row, int column) {
        System.out.println("use spell");
    }
}