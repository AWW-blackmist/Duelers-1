package client.models.comperessedData;

import java.util.ArrayList;

public class CompressedGameMap {
    private static final int ROW_NUMBER = 5, COLUMN_NUMBER = 9;

    private CompressedCell[][] cells;
    private ArrayList<CompressedTroop> troops = new ArrayList<>();

    public static int getRowNumber() {
        return ROW_NUMBER;
    }

    public static int getColumnNumber() {
        return COLUMN_NUMBER;
    }

    public CompressedCell[][] getCells() {
        return cells;
    }

    public ArrayList<CompressedTroop> getTroops() {
        return troops;
    }

    public CompressedTroop searchTroop(String cardID) {
        for (CompressedTroop troop :
                troops) {
            if (troop.getCard().getCardId().equals(cardID)){
                return troop;
            }
        }
        return null;
    }

    public CompressedTroop getTroop(int row, int column) {
        for (CompressedTroop troop : troops) {
            if (troop.getCell().getColumn() == column && troop.getCell().getRow() == row) {
                return troop;
            }
        }
        return null;
    }

    public ArrayList<CompressedTroop> getPlayerTroop(int playerNumber) {
        ArrayList<CompressedTroop> compressedTroops = new ArrayList<>();
        for (CompressedTroop troop : troops) {
            if (troop.getPlayerNumber() == playerNumber)
                compressedTroops.add(troop);
        }
        return compressedTroops;
    }

    public boolean checkCoordination(int row, int column) {
        return row >= 0 && row < ROW_NUMBER && column >= 0 && column < COLUMN_NUMBER;
    }
}
