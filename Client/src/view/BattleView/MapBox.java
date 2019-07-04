package view.BattleView;

import controller.GameController;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import models.card.CardType;
import models.comperessedData.CompressedGameMap;
import models.comperessedData.CompressedTroop;
import models.gui.CardPane;
import models.gui.DefaultLabel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;

public class MapBox implements PropertyChangeListener {
    private final BattleScene battleScene;
    private final CompressedGameMap gameMap;
    private final Group mapGroup;
    private final Polygon[][] cells = new Polygon[5][9];
    private final double[][] cellsX = new double[5][9];
    private final double[][] cellsY = new double[5][9];
    private final HashMap<CompressedTroop, TroopAnimation> troopAnimationHashMap = new HashMap<>();
    private CompressedTroop selectedTroop = null;
    private ArrayList<CompressedTroop> comboTroops = new ArrayList<>();
    private boolean spellSelected = false;
    private boolean comboSelected = false;
    private CardPane cardPane = null;
    private SelectionType selectionType;
    private DefaultLabel[][] flagLabels = new DefaultLabel[5][9];

    MapBox(BattleScene battleScene, CompressedGameMap gameMap, double x, double y) throws Exception {
        this.battleScene = battleScene;
        this.gameMap = gameMap;
        mapGroup = new Group();
        mapGroup.setLayoutY(y);
        mapGroup.setLayoutX(x);
        makePolygons();
        addFlagLabels();
        resetSelection();
        for (CompressedTroop troop : gameMap.getTroops()) {
            updateTroop(null, troop);
        }
        gameMap.addPropertyChangeListener(this);
    }

    private void makePolygons() {
        for (int j = 0; j < 5; j++) {
            double upperWidth = (j * Constants.MAP_DOWNER_WIDTH + (6 - j) * Constants.MAP_UPPER_WIDTH) / 6;
            double downerWidth = ((j + 1) * Constants.MAP_DOWNER_WIDTH + (6 - (j + 1)) * Constants.MAP_UPPER_WIDTH) / 6;
            double upperY = Constants.MAP_HEIGHT * (upperWidth - Constants.MAP_UPPER_WIDTH) /
                    (Constants.MAP_DOWNER_WIDTH - Constants.MAP_UPPER_WIDTH);
            double downerY = Constants.MAP_HEIGHT * (downerWidth - Constants.MAP_UPPER_WIDTH) /
                    (Constants.MAP_DOWNER_WIDTH - Constants.MAP_UPPER_WIDTH);
            for (int i = 0; i < 9; i++) {
                double x1 = (Constants.MAP_DOWNER_WIDTH - upperWidth) / 2 + i * upperWidth / 9;
                double x2 = (Constants.MAP_DOWNER_WIDTH - upperWidth) / 2 + (i + 1) * upperWidth / 9;
                double x3 = (Constants.MAP_DOWNER_WIDTH - downerWidth) / 2 + (i + 1) * downerWidth / 9;
                double x4 = (Constants.MAP_DOWNER_WIDTH - downerWidth) / 2 + i * downerWidth / 9;
                cells[j][i] = new Polygon(x1 + Constants.SPACE_BETWEEN_CELLS / 2,
                        upperY + Constants.SPACE_BETWEEN_CELLS / 2, x2 - Constants.SPACE_BETWEEN_CELLS / 2,
                        upperY + Constants.SPACE_BETWEEN_CELLS / 2, x3 - Constants.SPACE_BETWEEN_CELLS / 2,
                        downerY - Constants.SPACE_BETWEEN_CELLS / 2, x4 + Constants.SPACE_BETWEEN_CELLS / 2,
                        downerY - Constants.SPACE_BETWEEN_CELLS / 2);
                cells[j][i].setFill(Color.DARKBLUE);
                cells[j][i].setOpacity(Constants.CELLS_DEFAULT_OPACITY);
                final int I = i, J = j;
                cells[j][i].setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        hoverCell(J, I);
                    }
                });
                cells[j][i].setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        exitCell(J, I);
                    }
                });
                cells[j][i].setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        clickCell(J, I);
                    }
                });
                mapGroup.getChildren().add(cells[j][i]);
                cellsX[j][i] = (x1 + x2 + x3 + x4) / 4;
                cellsY[j][i] = (upperY + downerY) / 2;
            }
        }
    }

    private void addCircles() {
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 9; i++) {
                mapGroup.getChildren().add(new Circle(cellsX[j][i], cellsY[j][i], 2));
            }
        }
    }

    private void addFlagLabels() {
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 9; i++) {
                flagLabels[j][i] = new DefaultLabel(Integer.toString(gameMap.getCell(j, i).getNumberOfFlags()),
                        Constants.FLAG_FONT, Color.BLACK);
                System.out.println(gameMap.getCell(j, i).getNumberOfFlags());
                flagLabels[j][i].setVisible(true);
                mapGroup.getChildren().add(flagLabels[j][i]);
            }
        }
    }

    private void updateTroop(CompressedTroop oldTroop, CompressedTroop newTroop) {
        final TroopAnimation animation;
        if (newTroop == null) {
            animation = troopAnimationHashMap.get(oldTroop);
            if (animation != null) {
                animation.updateApHp(oldTroop.getCurrentAp(), 0);
                animation.getTroopGroup().setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                    }
                });
                animation.getTroopGroup().setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                    }
                });
                animation.getTroopGroup().setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                    }
                });
                animation.kill();
                troopAnimationHashMap.remove(oldTroop);
            }
        } else if (oldTroop != null && troopAnimationHashMap.containsKey(oldTroop)) {
            animation = troopAnimationHashMap.get(oldTroop);
            troopAnimationHashMap.remove(oldTroop);
            troopAnimationHashMap.put(newTroop, animation);
            animation.updateApHp(newTroop.getCurrentAp(), newTroop.getCurrentHp());
            animation.moveTo(newTroop.getPosition().getRow(), newTroop.getPosition().getColumn());
        } else {
            try {
                animation = new TroopAnimation(mapGroup, cellsX, cellsY, newTroop.getCard().getSpriteName(),
                        newTroop.getPosition().getRow(), newTroop.getPosition().getColumn(),
                        newTroop.getPlayerNumber() == 1,
                        newTroop.getPlayerNumber() == battleScene.getMyPlayerNumber());
                animation.updateApHp(newTroop.getCurrentAp(), newTroop.getCurrentHp());
                animation.getTroopGroup().setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        clickCell(animation.getRow(), animation.getColumn());
                    }
                });
                animation.getTroopGroup().setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        hoverCell(animation.getRow(), animation.getColumn());
                    }
                });
                animation.getTroopGroup().setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent mouseEvent) {
                        exitCell(animation.getRow(), animation.getColumn());
                    }
                });
                troopAnimationHashMap.put(newTroop, animation);
            } catch (Exception e) {
                System.out.println("Error making animation " + newTroop.getCard().getCardId());
            }
        }
        resetSelection();
    }

    Group getMapGroup() {
        return mapGroup;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("troop")) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    updateTroop((CompressedTroop) evt.getOldValue(), (CompressedTroop) evt.getNewValue());
                }
            });
        }
    }

    void resetSelection() {
        selectedTroop = null;
        comboTroops.clear();
        spellSelected = false;
        comboSelected = false;
        for (TroopAnimation animation : troopAnimationHashMap.values()) {
            animation.diSelect();
        }
        battleScene.getPlayerBox().refreshComboAndSpell();
        updateMapColors();
    }

    private void exitCell(int j, int i) {
        cells[j][i].setOpacity(Constants.CELLS_DEFAULT_OPACITY);
        if (cardPane != null) {
            mapGroup.getChildren().remove(cardPane);
            cardPane = null;
        }
        CompressedTroop troop = getTroop(j, i);
        if (troop == null)
            return;
        TroopAnimation animation = troopAnimationHashMap.get(troop);
        if (animation == null)
            return;
        if (!(selectedTroop == troop || comboTroops.contains(troop))) {
            animation.diSelect();
        }
    }

    private void hoverCell(int row, int column) {
        CompressedTroop troop = getTroop(row, column);
        if (troop != null) {
            TroopAnimation animation = troopAnimationHashMap.get(troop);
            animation.select();//TODO:Think about animation starts
            if (cardPane != null) {
                mapGroup.getChildren().remove(cardPane);
                cardPane = null;
            }
            try {
                cardPane = new CardPane(troop.getCard(), false, false, null);
                cardPane.setLayoutY(-150 * Constants.SCALE + cellsY[row][column]);
                cardPane.setLayoutX(80 * Constants.SCALE + cellsX[row][column]);
                mapGroup.getChildren().add(cardPane);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cells[row][column].setOpacity(Constants.CELLS_DEFAULT_OPACITY * 1.5);//TODO
    }

    private void clickCell(int row, int column) {
        if (!battleScene.isMyTurn()) {
            return;
        }
        CompressedTroop currentTroop = getTroop(row, column);
        if (selectionType == SelectionType.INSERTION) {
            if (GameController.getInstance().getAvailableActions().canInsertCard(
                    battleScene.getHandBox().getSelectedCard())) {
                battleScene.getController().insert(battleScene.getHandBox().getSelectedCard(), row, column);
                System.out.println("Insert " + battleScene.getHandBox().getSelectedCard().getCardId());
                battleScene.getHandBox().resetSelection();//TODO:remove
                resetSelection();
            }
            return;
        }
        if (selectionType == SelectionType.SELECTION) {
            if (currentTroop != null && currentTroop.getPlayerNumber() == battleScene.getMyPlayerNumber()) {
                selectedTroop = currentTroop;
                updateMapColors();
                System.out.println("Select " + currentTroop.getCard().getCardId());
            }
            return;
        }
        if (selectedTroop != null && selectedTroop.getPosition().getRow() == row &&
                selectedTroop.getPosition().getColumn() == column) {
            System.out.println("DiSelect");
            battleScene.getHandBox().resetSelection();//TODO:remove
            resetSelection();
            return;
        }
        if (selectionType == SelectionType.SPELL) {
            if (GameController.getInstance().getAvailableActions().canUseSpecialAction(selectedTroop)) {
                battleScene.getController().useSpecialPower(row, column);
                System.out.println(selectedTroop.getCard().getCardId() + " SpecialPower");
                battleScene.getHandBox().resetSelection();//TODO:remove
                resetSelection();
            }
            return;
        }
        if (selectionType == SelectionType.COMBO) {
            if (currentTroop != null && currentTroop.getPlayerNumber() == battleScene.getMyPlayerNumber()
                    && currentTroop.getCard().isHasCombo()) {
                if (comboTroops.contains(currentTroop)) {
                    comboTroops.remove(currentTroop);
                    System.out.println("remove " + currentTroop.getCard().getCardId() + " from combos");
                } else {
                    comboTroops.add(currentTroop);
                    System.out.println("add " + currentTroop.getCard().getCardId() + " to combos");
                }
                updateMapColors();
            } else if (GameController.getInstance().getAvailableActions().canAttack(
                    selectedTroop, row, column)) {
                comboTroops.add(selectedTroop);
                battleScene.getController().comboAttack(comboTroops, currentTroop);
                battleScene.getHandBox().resetSelection();//TODO:remove
                resetSelection();
                System.out.println("combo attack");
            }
            return;
        }
        if (selectionType == SelectionType.NORMAL) {
            if (GameController.getInstance().getAvailableActions().canAttack(
                    selectedTroop, row, column)) {
                battleScene.getController().attack(selectedTroop, currentTroop);
                System.out.println(selectedTroop + " attacked to " + currentTroop);
                battleScene.getHandBox().resetSelection();//TODO:remove
                resetSelection();
            } else if (GameController.getInstance().getAvailableActions().canMove(
                    selectedTroop, row, column)) {
                battleScene.getController().move(selectedTroop, row, column);
                System.out.println(selectedTroop.getCard().getCardId() + "moved");
                battleScene.getHandBox().resetSelection();//TODO:remove
                resetSelection();
            }
            return;
        }
    }

    void updateMapColors() {
        updateSelectionType();
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 9; column++) {
                if (!battleScene.isMyTurn()) {
                    cells[row][column].setFill(Constants.defaultColor);
                }
                CompressedTroop currentTroop = getTroop(row, column);
                if (selectionType == SelectionType.INSERTION) {
                    if (GameController.getInstance().getAvailableActions().canInsertCard(
                            battleScene.getHandBox().getSelectedCard())) {
                        if (battleScene.getHandBox().getSelectedCard().getType() == CardType.HERO ||
                                battleScene.getHandBox().getSelectedCard().getType() == CardType.MINION) {
                            cells[row][column].setFill(Constants.MoveColor);
                        } else {
                            cells[row][column].setFill(Constants.SpellColor);
                        }
                    } else
                        cells[row][column].setFill(Constants.defaultColor);
                    continue;
                }
                if (selectionType == SelectionType.SELECTION) {
                    if (currentTroop != null && currentTroop.getPlayerNumber() == battleScene.getMyPlayerNumber()) {
                        cells[row][column].setFill(Constants.CanSelectColor);
                    } else
                        cells[row][column].setFill(Constants.defaultColor);
                    continue;
                }
                if (selectedTroop != null && selectedTroop.getPosition().getRow() == row &&
                        selectedTroop.getPosition().getColumn() == column) {
                    cells[row][column].setFill(Constants.SelectedColor);//not important
                    continue;
                }
                if (selectionType == SelectionType.SPELL) {
                    if (GameController.getInstance().getAvailableActions().canUseSpecialAction(selectedTroop)) {
                        cells[row][column].setFill(Constants.SpellColor);
                    } else
                        cells[row][column].setFill(Constants.defaultColor);
                    continue;
                }
                if (selectionType == SelectionType.COMBO) {
                    if (currentTroop != null && currentTroop.getPlayerNumber() == battleScene.getMyPlayerNumber()
                            && currentTroop.getCard().isHasCombo()) {
                        if (comboTroops.contains(currentTroop))
                            cells[row][column].setFill(Constants.SelectedColor);
                        else
                            cells[row][column].setFill(Constants.CanSelectColor);
                    } else if (GameController.getInstance().getAvailableActions().canAttack(
                            selectedTroop, row, column))
                        cells[row][column].setFill(Constants.attackColor);
                    else
                        cells[row][column].setFill(Constants.defaultColor);
                    continue;
                }
                if (selectionType == SelectionType.NORMAL) {
                    if (GameController.getInstance().getAvailableActions().canAttack(
                            selectedTroop, row, column))
                        cells[row][column].setFill(Constants.attackColor);
                    else if (GameController.getInstance().getAvailableActions().canMove(
                            selectedTroop, row, column))
                        cells[row][column].setFill(Constants.MoveColor);
                    else
                        cells[row][column].setFill(Constants.defaultColor);
                    continue;
                }
            }
        }
    }

    private void updateSelectionType() {
        if (battleScene.getHandBox().getSelectedCard() != null) {
            selectionType = SelectionType.INSERTION;
            return;
        }
        if (selectedTroop == null) {
            selectionType = SelectionType.SELECTION;
            return;
        }
        if (isComboSelected()) {
            selectionType = SelectionType.COMBO;
            return;
        }
        if (isSpellSelected()) {
            selectionType = SelectionType.SPELL;
            return;
        }
        selectionType = SelectionType.NORMAL;
    }

    private CompressedTroop getTroop(int j, int i) {
        for (CompressedTroop troop : troopAnimationHashMap.keySet()) {
            if (troop.getPosition().getRow() == j && troop.getPosition().getColumn() == i)
                return troop;
        }
        return null;
    }

    CompressedTroop getSelectedTroop() {
        return selectedTroop;
    }

    void setSpellSelected() {
        this.spellSelected = true;
    }

    void setComboSelected() {
        this.comboSelected = true;
    }

    boolean isSpellSelected() {
        return spellSelected;
    }

    boolean isComboSelected() {
        return comboSelected;
    }

    CompressedGameMap getGameMap() {
        return gameMap;
    }

    void showAttack(String cardId, int i) {
        if (cardId == null)
            System.out.println("Error0 MapBox");
        CompressedTroop troop = gameMap.getTroop(cardId);
        if (troop == null)
            System.out.println("Error1 MapBox");
        TroopAnimation animation = troopAnimationHashMap.get(troop);
        if (animation == null)
            System.out.println("Error2 MapBox");
        else
            animation.attack(i);
    }

    void showDefend(String cardId, int i) {
        CompressedTroop troop = gameMap.getTroop(cardId);
        if (troop == null)
            System.out.println("Error3 MapBox");
        else {
            TroopAnimation animation = troopAnimationHashMap.get(troop);
            if (animation == null)
                System.out.println("Error4 MapBox");
            else
                animation.hit(i);
        }
    }

    void showSpell(String spriteName, int j, int i) {
        //TODO
    }

    enum SelectionType {
        INSERTION, SELECTION, COMBO, SPELL, NORMAL
    }
}
