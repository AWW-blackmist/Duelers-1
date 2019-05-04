package server.models.game;

import server.Server;
import server.models.account.Account;
import server.models.account.MatchHistory;
import server.models.card.AttackType;
import server.models.card.Card;
import server.models.card.CardType;
import server.models.card.Deck;
import server.models.card.spell.Spell;
import server.models.card.spell.SpellAction;
import server.models.comperessedData.CompressedGame;
import server.models.exceptions.ClientException;
import server.models.exceptions.LogicException;
import server.models.exceptions.ServerException;
import server.models.map.Cell;
import server.models.map.GameMap;
import server.models.map.Position;
import server.models.message.CardPosition;

import java.util.ArrayList;

public abstract class Game {
    private Player playerOne;
    private Player playerTwo;
    private GameType gameType;
    private ArrayList<Buff> buffs = new ArrayList<>();
    private GameMap gameMap;
    private int turnNumber;
    private int lastTurnChangingTime;

    protected Game(Account account, Deck secondDeck, String userName, GameMap gameMap, GameType gameType) {
        this.gameType = gameType;
        this.gameMap = gameMap;
        this.playerOne = new Player(account.getMainDeck(), account.getUsername(), 1);
        this.playerTwo = new Player(secondDeck, userName, 2);
    }

    public void startGame() throws ServerException {
        this.turnNumber = 1;
        putMinion(1, playerOne.getHero(), gameMap.getCell(2, 0));
        this.turnNumber = 2;
        putMinion(2, playerTwo.getHero(), gameMap.getCell(2, 8));
        this.turnNumber = 1;

        playerOne.setCurrentMP(2);

        applyOnStartSpells(playerTwo.getDeck());
        applyOnStartSpells(playerTwo.getDeck());
    }

    public CompressedGame toCompressedGame() {
        return new CompressedGame(playerOne, playerTwo, gameMap, turnNumber, gameType);
    }

    private void applyOnStartSpells(Deck deck) throws ServerException {
        for (Card card : deck.getOthers()) {
            iterateOnOnStartSpells(card);
        }
        if (deck.getItem() != null) {
            iterateOnOnStartSpells(deck.getItem());
        }
        if (deck.getHero() != null) {
            iterateOnOnStartSpells(deck.getHero());
        }
    }

    private void iterateOnOnStartSpells(Card card) throws ServerException {
        for (Spell spell : card.getSpells()) {
            if (spell.getAvailabilityType().isOnStart())
                applySpell(spell, detectTarget(
                        spell,
                        gameMap.getCell(0, 0),
                        gameMap.getCell(0, 0),
                        gameMap.getCell(0, 0))
                );
        }
    }

    public Player getPlayerOne() {
        return playerOne;
    }

    public Player getPlayerTwo() {
        return playerTwo;
    }

    private Player getCurrentTurnPlayer() {
        if (turnNumber % 2 == 1) {
            return playerOne;
        } else {
            return playerTwo;
        }
    }

    private Player getOtherTurnPlayer() {
        if (turnNumber % 2 == 0) {
            return playerOne;
        } else {
            return playerTwo;
        }
    }

    private boolean canCommand(String username) {
        return (turnNumber % 2 == 0 && username.equalsIgnoreCase(playerTwo.getUserName()))
                || (turnNumber % 2 == 1 && username.equalsIgnoreCase(playerOne.getUserName()));
    }

    public void changeTurn(String username) throws LogicException {
        if (canCommand(username)) {
            addNextCardToHand();
            revertNotDurableBuffs();
            removeFinishedBuffs();
            turnNumber++;
            setAllTroopsCanAttackAndCanMove();
            applyAllBuffs();
            if (turnNumber < 14)
                getCurrentTurnPlayer().setCurrentMP(turnNumber / 2 + 2);
            else
                getCurrentTurnPlayer().setCurrentMP(9);
            Server.getInstance().sendGameUpdateMessage(this);
            if (getCurrentTurnPlayer().getUserName().equals("AI")) {
                playCurrentTurn();
            }
        } else {
            throw new ClientException("it isn't your turn!");
        }
    }

    private void addNextCardToHand() throws ServerException {
        Card nextCard = getCurrentTurnPlayer().getNextCard();
        if (getCurrentTurnPlayer().addNextCardToHand()) {
            Server.getInstance().sendChangeCardPositionMessage(this, nextCard, CardPosition.HAND);
            Server.getInstance().sendChangeCardPositionMessage(this, getCurrentTurnPlayer().getNextCard(), CardPosition.NEXT);
        }
    }

    private void playCurrentTurn() {
        //TODO:write AI
        //TODO: change turn
    }

    private void removeFinishedBuffs() {
        for (Buff buff : buffs) {
            if (buff.getAction().getDuration() == 0) {
                buffs.remove(buff);
            }
        }
    }

    private void setAllTroopsCanAttackAndCanMove() throws ServerException {
        for (Troop troop : gameMap.getTroops()) {
            troop.setCanAttack(true);
            troop.setCanMove(true);
            Server.getInstance().sendTroopUpdateMessage(this, troop);
        }
    }

    private void applyAllBuffs() throws ServerException {
        for (Buff buff : buffs) {
            applyBuff(buff);
        }
    }

    private void revertNotDurableBuffs() throws ServerException {
        for (Buff buff : buffs) {
            if (!buff.getAction().isDurable()) {
                revertBuff(buff);
            }
        }
    }

    private void revertBuff(Buff buff) throws ServerException {
        SpellAction action = buff.getAction();

        for (Troop troop : buff.getTarget().getTroops()) {
            if (!(buff.isPositive() || troop.canGiveBadEffect())) continue;

            troop.changeEnemyHit(-action.getEnemyHitChanges());
            troop.changeCurrentAp(-action.getApChange());
            if (!action.isPoison() || troop.canGetPoison()) {
                troop.changeCurrentHp(-action.getHpChange());
                if (troop.getCurrentHp() <= 0) {
                    killTroop(troop);
                }
            }
            if (action.isMakeStun() && troop.canGetStun()) {
                troop.setCanMove(true);
            }
            if (action.isMakeDisarm() && troop.canGetDisarm()) {
                troop.setDisarm(false);
            }
            if (action.isNoDisarm()) {
                troop.setCantGetDisarm(false);
            }
            if (action.isNoPoison()) {
                troop.setCantGetPoison(false);
            }
            if (action.isNoStun()) {
                troop.setCantGetStun(false);
            }
            if (action.isNoBadEffect()) {
                troop.setDontGiveBadEffect(false);
            }
            if (action.isNoAttackFromWeakerOnes()) {
                troop.setNoAttackFromWeakerOnes(false);
            }
            if (action.isDisableHolyBuff()) {
                troop.setDisableHolyBuff(false);
            }
            Server.getInstance().sendTroopUpdateMessage(this, troop);
        }
    }

    public void insert(String username, String cardId, Position position) throws LogicException {
        if (!canCommand(username)) {
            throw new ClientException("it's not your turn");
        }
        Player player = getCurrentTurnPlayer();
        Card card = player.insert(cardId);

        if (card.getType() == CardType.MINION) {
            Server.getInstance().sendChangeCardPositionMessage(this, card, CardPosition.MAP);
            putMinion(
                    player.getPlayerNumber(),
                    new Troop(card, getCurrentTurnPlayer().getPlayerNumber()),
                    gameMap.getCell(position)
            );
        } else {
            Server.getInstance().sendChangeCardPositionMessage(this, card, CardPosition.GRAVE_YARD);
        }
        if (card.getType() == CardType.SPELL) {
            player.addToGraveYard(card);
        }
        applyOnPutSpells(card, gameMap.getCell(position));
    }

    private void putMinion(int playerNumber, Troop troop, Cell cell) throws ServerException {
        troop.setCell(cell);
        gameMap.addTroop(playerNumber, troop);
        Server.getInstance().sendTroopUpdateMessage(this, troop);
    }

    private void applyOnPutSpells(Card card, Cell cell) throws ServerException {
        for (Spell spell : card.getSpells()) {
            if (spell.getAvailabilityType().isOnPut()) {
                applySpell(spell, detectTarget(spell, cell, cell, getCurrentTurnPlayer().getHero().getCell()));
            }
        }
    }

    public void moveTroop(String username, String cardId, Position position) throws ClientException, ServerException {
        if (!canCommand(username)) {
            throw new ClientException("its not your turn");
        }

        if (!gameMap.checkCoordination(position)) {
            throw new ClientException("coordination is not valid");
        }

        Troop troop = gameMap.getTroop(cardId);
        if (troop == null) {
            throw new ClientException("select a valid card");
        }
        if (troop.getCell().manhattanDistance(position) > 2) {
            throw new ClientException("too far to go");
        }
        if (!troop.canMove()) {
            throw new ClientException("troop can not move");
        }

        Cell cell = gameMap.getCell(position);
        troop.setCell(cell);
        troop.setCanMove(false);
        Server.getInstance().sendTroopUpdateMessage(this, troop);

        for (Card item : cell.getItems()) {
            if (item.getType() == CardType.FLAG) {
                catchFlag(troop, item);
            } else if (item.getType() == CardType.COLLECTIBLE_ITEM) {
                catchItem(item);
                Server.getInstance().sendChangeCardPositionMessage(this, item, CardPosition.COLLECTED);
            }
        }
        cell.clearItems();
    }

    void catchFlag(Troop troop, Card item) throws ServerException {
        troop.addFlag(item);
        getCurrentTurnPlayer().increaseNumberOfCollectedFlags();
        getCurrentTurnPlayer().addFlagCarrier(troop);
        Server.getInstance().sendGameUpdateMessage(this);
    }

    private void catchItem(Card item) {
        getCurrentTurnPlayer().addCollectibleItems(item);
    }

    public void attack(String username, String attackerCardId, String defenderCardId) throws LogicException {
        if (!canCommand(username)) {
            throw new ClientException("its not your turn");
        }

        Troop attackerTroop = getAndValidateTroop(attackerCardId, getCurrentTurnPlayer());
        Troop defenderTroop = getAndValidateTroop(defenderCardId, getOtherTurnPlayer());

        if (!attackerTroop.canAttack()) {
            throw new ClientException("attacker can not attack");
        }

        checkRangeForAttack(attackerTroop, defenderTroop);

        if (defenderTroop.canGiveBadEffect() &&
                (defenderTroop.canBeAttackedFromWeakerOnes() || attackerTroop.getCurrentAp() > defenderTroop.getCurrentAp())
        ) {
            damage(attackerTroop, defenderTroop);

            attackerTroop.setCanAttack(false);
            attackerTroop.setCanMove(false);
            Server.getInstance().sendTroopUpdateMessage(this, attackerTroop);

            applyOnAttackSpells(attackerTroop, defenderTroop);
            applyOnDefendSpells(defenderTroop, attackerTroop);
            counterAttack(defenderTroop, attackerTroop);
        }
    }

    private void applyOnAttackSpells(Troop attackerTroop, Troop defenderTroop) throws ServerException {
        for (Spell spell : attackerTroop.getCard().getSpells()) {
            if (spell.getAvailabilityType().isOnAttack())
                applySpell(
                        spell,
                        detectTarget(spell, defenderTroop.getCell(), defenderTroop.getCell(), getCurrentTurnPlayer().getHero().getCell())
                );
        }
    }

    private void applyOnDefendSpells(Troop defenderTroop, Troop attackerTroop) throws ServerException {
        for (Spell spell : defenderTroop.getCard().getSpells()) {
            if (spell.getAvailabilityType().isOnDefend())
                applySpell(
                        spell,
                        detectTarget(spell, attackerTroop.getCell(), attackerTroop.getCell(), getOtherTurnPlayer().getHero().getCell())
                );
        }
    }

    private void counterAttack(Troop defenderTroop, Troop attackerTroop) throws LogicException {
        if (defenderTroop.isDisarm()) {
            throw new ClientException("defender is disarm");
        }

        checkRangeForAttack(defenderTroop, attackerTroop);

        if (attackerTroop.canGiveBadEffect() &&
                (attackerTroop.canBeAttackedFromWeakerOnes() || defenderTroop.getCurrentAp() > attackerTroop.getCurrentAp())
        ) {
            damage(defenderTroop, attackerTroop);
        }
    }

    private void damage(Troop attackerTroop, Troop defenderTroop) throws ServerException {
        int attackPower = calculateAp(attackerTroop, defenderTroop);

        defenderTroop.changeCurrentHp(-attackPower);

        if (defenderTroop.getCurrentHp() <= 0) {
            killTroop(defenderTroop);
        } else {
            Server.getInstance().sendTroopUpdateMessage(this, defenderTroop);
        }
    }

    private int calculateAp(Troop attackerTroop, Troop defenderTroop) {
        int attackPower = attackerTroop.getCurrentAp();
        if (!attackerTroop.isHolyBuffDisabling() || defenderTroop.getEnemyHitChanges() > 0) {
            attackPower += defenderTroop.getEnemyHitChanges();
        }
        return attackPower;
    }

    public void useSpecialPower(String username, String cardId, Position target) throws LogicException {
        if (!canCommand(username)) {
            throw new ClientException("its not your turn");
        }

        Troop hero = getAndValidateHero(cardId);
        Spell specialPower = getAndValidateSpecialPower(hero);

        applySpell(
                specialPower,
                detectTarget(specialPower, hero.getCell(), gameMap.getCell(target), hero.getCell())
        );
    }

    private Troop getAndValidateHero(String cardId) throws ClientException {
        Troop hero = getCurrentTurnPlayer().getHero();
        if (hero == null || !hero.getCard().getCardId().equalsIgnoreCase(cardId)) {
            throw new ClientException("hero id is not valid");
        }
        return hero;
    }

    private Spell getAndValidateSpecialPower(Troop hero) throws ClientException {
        Spell specialPower = hero.getCard().getSpells().get(0);
        if (specialPower == null || specialPower.getAvailabilityType().isSpecialPower()) {
            throw new ClientException("special power is not available");
        }

        if (specialPower.isCoolDown(turnNumber)) {
            throw new ClientException("special power is cool down");
        }

        if (getCurrentTurnPlayer().getCurrentMP() < specialPower.getMannaPoint()) {
            throw new ClientException("insufficient manna");
        }
        return specialPower;
    }

    public void comboAttack(String username, String[] attackerCardIds, String defenderCardId) throws LogicException {
        if (!canCommand(username)) {
            throw new ClientException("its not your turn");
        }

        Troop defenderTroop = getAndValidateTroop(defenderCardId, getOtherTurnPlayer());
        Troop[] attackerTroops = getAndValidateAttackerTroops(attackerCardIds, defenderTroop);

        damageFromAllAttackers(defenderTroop, attackerTroops);

        applyOnDefendSpells(defenderTroop, attackerTroops[0]);
        counterAttack(defenderTroop, attackerTroops[0]);
    }

    private Troop getAndValidateTroop(String defenderCardId, Player otherTurnPlayer) throws ClientException {
        Troop troop = otherTurnPlayer.getTroop(defenderCardId);
        if (troop == null) {
            throw new ClientException("card id is not valid");
        }
        return troop;
    }

    private Troop[] getAndValidateAttackerTroops(String[] attackerCardIds, Troop defenderTroop) throws ClientException {
        Troop[] attackerTroops = new Troop[attackerCardIds.length];
        for (int i = 0; i < attackerTroops.length; i++) {
            attackerTroops[i] = getCurrentTurnPlayer().getTroop(attackerCardIds[i]);
            if (attackerTroops[i] == null || !attackerTroops[i].getCard().hasCombo()) {
                throw new ClientException("invalid attacker troop");
            }

            checkRangeForAttack(attackerTroops[i], defenderTroop);
        }
        return attackerTroops;
    }

    private void checkRangeForAttack(Troop attackerTroop, Troop defenderTroop) throws ClientException {
        if (attackerTroop.getCard().getAttackType() == AttackType.MELEE) {
            if (!attackerTroop.getCell().isNextTo(defenderTroop.getCell())) {
                throw new ClientException("can not attack to this target");
            }
        } else if (attackerTroop.getCard().getAttackType() == AttackType.RANGED) {
            if (attackerTroop.getCell().isNextTo(defenderTroop.getCell()) ||
                    attackerTroop.getCell().manhattanDistance(defenderTroop.getCell()) > attackerTroop.getCard().getRange()) {
                throw new ClientException("can not attack to this target");
            }
        } else { // HYBRID
            if (attackerTroop.getCell().manhattanDistance(defenderTroop.getCell()) > attackerTroop.getCard().getRange()) {
                throw new ClientException("can not attack to this target");
            }
        }
    }

    private void damageFromAllAttackers(Troop defenderTroop, Troop[] attackerTroops) throws ServerException {
        for (Troop attackerTroop : attackerTroops) {
            if (defenderTroop.canGiveBadEffect() &&
                    (defenderTroop.canBeAttackedFromWeakerOnes() || attackerTroop.getCurrentAp() > defenderTroop.getCurrentAp())
            ) {
                damage(attackerTroop, defenderTroop);

                attackerTroop.setCanAttack(false);
                attackerTroop.setCanMove(false);
                Server.getInstance().sendTroopUpdateMessage(this, attackerTroop);

                applyOnAttackSpells(attackerTroop, defenderTroop);
            }
        }
    }

    public abstract boolean finishCheck();

    private void applySpell(Spell spell, TargetData target) throws ServerException {
        spell.setLastTurnUsed(turnNumber);
        Buff buff = new Buff(spell.getAction(), target);
        buffs.add(buff);
        applyBuff(buff);
    }

    private void applyBuff(Buff buff) throws ServerException {
        TargetData target = buff.getTarget();
        if (haveDelay(buff)) return;

        applyBuffOnCards(buff, target.getCards());
        applyBuffOnCellTroops(buff, target.getCells());
        applyBuffOnTroops(buff, target.getTroops());
        applyBuffOnPlayers(buff, target.getPlayers());

        decreaseDuration(buff);
    }

    private void applyBuffOnPlayers(Buff buff, ArrayList<Player> players) throws ServerException {
        SpellAction action = buff.getAction();
        for (Player player : players) {
            player.changeCurrentMP(action.getMpChange());
            Server.getInstance().sendGameUpdateMessage(this);
        }
    }

    private void decreaseDuration(Buff buff) {
        SpellAction action = buff.getAction();
        if (action.getDuration() > 0) {
            action.decreaseDuration();
        }
    }

    private boolean haveDelay(Buff buff) {
        SpellAction action = buff.getAction();
        if (action.getDelay() > 0) {
            action.decreaseDelay();
            return true;
        }
        return false;
    }

    private void applyBuffOnCards(Buff buff, ArrayList<Card> cards) {
        SpellAction action = buff.getAction();
        for (Card card : cards) {
            if (action.isAddSpell()) {
                card.addSpell(action.getCarryingSpell());
            }
        }
    }

    private void applyBuffOnCellTroops(Buff buff, ArrayList<Cell> cells) throws ServerException {
        ArrayList<Troop> inCellTroops = getInCellTargetTroops(cells);
        Buff troopBuff = new Buff(
                buff.getAction().makeCopyAction(1, 0), new TargetData(inCellTroops)
        );
        buffs.add(troopBuff);
        applyBuffOnTroops(troopBuff, inCellTroops);
    }

    private void applyBuffOnTroops(Buff buff, ArrayList<Troop> targetTroops) throws ServerException {
        SpellAction action = buff.getAction();
        for (Troop troop : targetTroops) {
            if (!(buff.isPositive() || troop.canGiveBadEffect())) continue;

            troop.changeEnemyHit(action.getEnemyHitChanges());
            troop.changeCurrentAp(action.getApChange());
            if (!action.isPoison() || troop.canGetPoison()) {
                troop.changeCurrentHp(action.getHpChange());
                if (troop.getCurrentHp() <= 0) {
                    killTroop(troop);
                }
            }
            if (action.isMakeStun() && troop.canGetStun()) {
                troop.setCanMove(false);
            }
            if (action.isMakeDisarm() && troop.canGetDisarm()) {
                troop.setDisarm(true);
            }
            if (action.isNoDisarm()) {
                troop.setCantGetDisarm(true);
            }
            if (action.isNoPoison()) {
                troop.setCantGetPoison(true);
            }
            if (action.isNoStun()) {
                troop.setCantGetStun(true);
            }
            if (action.isNoBadEffect()) {
                troop.setDontGiveBadEffect(true);
            }
            if (action.isNoAttackFromWeakerOnes()) {
                troop.setNoAttackFromWeakerOnes(true);
            }
            if (action.isDisableHolyBuff()) {
                troop.setDisableHolyBuff(true);
            }
            if (action.isKillsTarget()) {
                killTroop(troop);
            }
            if (action.getRemoveBuffs() > 0) {
                removePositiveBuffs(troop);
            }
            if (action.getRemoveBuffs() < 0) {
                removeNegativeBuffs(troop);
            }

            Server.getInstance().sendTroopUpdateMessage(this, troop);
        }
    }

    private void removePositiveBuffs(Troop troop) {
        for (Buff buff : buffs) {
            if (buff.isPositive() && buff.getAction().getDuration() >= 0) {
                buff.getTarget().getTroops().remove(troop);
            }
        }
    }

    private void removeNegativeBuffs(Troop troop) {
        for (Buff buff : buffs) {
            if (!buff.isPositive() && buff.getAction().getDuration() >= 0) {
                buff.getTarget().getTroops().remove(troop);
            }
        }
    }

    void killTroop(Troop troop) throws ServerException {
        applyOnDeathSpells(troop);
        if (troop.getPlayerNumber() == 1) {
            playerOne.killTroop(troop);
            gameMap.removeTroop(playerOne, troop);
        } else if (troop.getPlayerNumber() == 2) {
            playerTwo.killTroop(troop);
            gameMap.removeTroop(playerTwo, troop);
        }
        Server.getInstance().sendChangeCardPositionMessage(this, troop.getCard(), CardPosition.GRAVE_YARD);
    }

    private void applyOnDeathSpells(Troop troop) throws ServerException {
        for (Spell spell : troop.getCard().getSpells()) {
            if (spell.getAvailabilityType().isOnDefend())
                applySpell(
                        spell,
                        detectTarget(spell, troop.getCell(), gameMap.getCell(0, 0), getOtherTurnPlayer().getHero().getCell())
                );
        }
    }

    private ArrayList<Troop> getInCellTargetTroops(ArrayList<Cell> cells) {
        ArrayList<Troop> inCellTroops = new ArrayList<>();
        for (Cell cell : cells) {
            Troop troop = playerOne.getTroop(cell);
            if (troop == null) {
                troop = playerTwo.getTroop(cell);
            }
            if (troop != null) {
                inCellTroops.add(troop);
            }
        }
        return inCellTroops;
    }

    private TargetData detectTarget(Spell spell, Cell cardCell, Cell clickCell, Cell heroCell) {
        TargetData targetData = new TargetData();
        Player player = getCurrentTurnPlayer();
        if (spell.getTarget().getCardType().isPlayer()) {
            targetData.getPlayers().add(player);
            return targetData;
        }
        Position centerPosition;
        if (spell.getTarget().isRelatedToCardOwnerPosition()) {
            centerPosition = new Position(cardCell);
        } else if (spell.getTarget().isForAroundOwnHero()) {
            centerPosition = new Position(heroCell);
        } else {
            centerPosition = new Position(clickCell);
        }
        ArrayList<Cell> targetCells = detectCells(centerPosition, spell.getTarget().getDimensions());
        detectTargets(spell, targetData, player, targetCells);
        return targetData;
    }

    private void detectTargets(Spell spell, TargetData targetData, Player player, ArrayList<Cell> targetCells) {
        for (Cell cell : targetCells) {
            if (spell.getTarget().getCardType().isCell()) {
                targetData.getCells().add(cell);
            }
            if (spell.getTarget().getCardType().isHero()) {
                Troop troop = player.getTroop(cell);
                if (troop == null) {
                    troop = getOtherTurnPlayer().getTroop(cell);
                }
                if (troop != null) {
                    if (troop.getCard().getType() == CardType.HERO) {
                        targetData.getTroops().add(troop);
                    }
                }
            }
            if (spell.getTarget().getCardType().isMinion()) {
                Troop troop = player.getTroop(cell);
                if (troop == null) {
                    troop = getOtherTurnPlayer().getTroop(cell);
                }
                if (troop != null) {
                    if (troop.getCard().getType() == CardType.MINION) {
                        targetData.getTroops().add(troop);
                    }
                }
            }
        }
    }

    private ArrayList<Cell> detectCells(Position centerPosition, Position dimensions) {
        int firstRow = centerPosition.getRow() - (dimensions.getRow() - 1) / 2;
        int lastRow = centerPosition.getRow() + dimensions.getRow();
        int firstColumn = centerPosition.getColumn() - (dimensions.getColumn() - 1) / 2;
        int lastColumn = centerPosition.getColumn() + dimensions.getColumn();
        if (firstRow < 0)
            firstRow = 0;
        if (lastRow > GameMap.getRowNumber())
            lastRow = GameMap.getRowNumber();
        if (firstColumn < 0)
            firstColumn = 0;
        if (lastColumn > GameMap.getColumnNumber())
            lastColumn = GameMap.getColumnNumber();
        ArrayList<Cell> targetCells = new ArrayList<>();
        for (int i = firstRow; i <= lastRow; i++) {
            for (int j = firstColumn; j <= lastColumn; j++) {
                targetCells.add(gameMap.getCells()[i][j]);
            }
        }
        return targetCells;
    }

    void setMatchHistories(boolean resultOne, boolean resultTwo) {
        playerOne.setMatchHistory(
                new MatchHistory(playerTwo.getUserName(), resultOne)
        );

        playerTwo.setMatchHistory(
                new MatchHistory(playerOne.getUserName(), resultTwo)
        );
    }
}