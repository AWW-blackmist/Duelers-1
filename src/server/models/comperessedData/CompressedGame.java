package server.models.comperessedData;

import server.models.game.GameType;
import server.models.game.Player;
import server.models.map.GameMap;

public class CompressedGame {
    private CompressedPlayer playerOne;
    private CompressedPlayer playerTwo;
    private CompressedGameMap gameMap;
    private int turnNumber;
    private GameType gameType;

    public CompressedGame(Player playerOne, Player playerTwo, GameMap gameMap, int turnNumber, GameType gameType) {
        this.playerOne = playerOne.toCompressedPlayer();
        this.playerTwo = playerTwo.toCompressedPlayer();
        this.gameMap = gameMap.toCompressedGameMap();
        this.turnNumber = turnNumber;
        this.gameType = gameType;
    }
}
