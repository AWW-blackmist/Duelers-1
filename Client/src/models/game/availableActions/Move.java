package models.game.availableActions;

import models.comperessedData.CompressedTroop;
import models.game.map.Position;

import java.util.ArrayList;

public class Move {
    private CompressedTroop troop;
    private ArrayList<Position> targets;

    public Move(CompressedTroop troop, ArrayList<Position> targets) {
        this.troop = troop;
        this.targets = targets;
    }

    public CompressedTroop getTroop() {
        return troop;
    }

    public ArrayList<Position> getTargets() {
        return targets;
    }
}