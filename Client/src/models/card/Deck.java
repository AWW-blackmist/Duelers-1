package models.card;


import models.account.Collection;

import java.util.ArrayList;

public class Deck {
    private String deckName;
    private Card hero;
    private Card item;
    private ArrayList<Card> others = new ArrayList<>();

    public Deck(TempDeck tempDeck, Collection collection) {
        this.deckName = tempDeck.getDeckName();
        this.hero = collection.findHero(tempDeck.getHeroId());
        this.item = collection.findItem(tempDeck.getItemId());
        for (String cardId : tempDeck.getOthersIds()) {
            others.add(collection.findOthers(cardId));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Deck) {
            Deck deck = (Deck) obj;
            if (hero == null ^ deck.hero == null) return false;
            if (hero != null && !hero.equals(deck.hero)) return false;

            if (item == null ^ deck.item == null) return false;
            if (item != null && !item.equals(deck.item)) return false;

            if (others.size() != deck.others.size()) return false;
            for (Card other : others) {
                if (!deck.others.contains(other)) return false;
            }
            return true;
        }
        if (obj instanceof TempDeck) {
            TempDeck deck = (TempDeck) obj;
            if (hero == null ^ deck.getHeroId() == null) return false;
            if (hero != null && !hero.getCardId().equalsIgnoreCase(deck.getHeroId())) return false;

            if (item == null ^ deck.getItemId() == null) return false;
            if (item != null && !item.getCardId().equalsIgnoreCase(deck.getItemId())) return false;

            if (others.size() != deck.getOthersIds().size()) return false;
            Outer:
            for (String otherId : deck.getOthersIds()) {
                for (Card other : others) {
                    if (other.getCardId().equalsIgnoreCase(otherId)) continue Outer;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    public String getName() {
        return this.deckName;
    }

    public Card getHero() {
        return this.hero;
    }

    ArrayList<Card> getOthers() {
        return this.others;
    }

    public Card getItem() {
        return this.item;
    }

    public boolean areSame(String deckName) {
        return this.deckName.equalsIgnoreCase(deckName);
    }

    public boolean isValid() {
        if (hero == null || item == null) return false;
        return others.size() == 20;
    }
}