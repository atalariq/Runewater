package com.battleship.interfaces;

import com.battleship.model.Mage;
import com.battleship.model.Ship;
import com.battleship.model.enums.Element;

/**
 * Kontrak untuk kapal yang bisa melakukan sihir dan jurus.
 * Diimplementasikan oleh: PlayerShip
 */
public interface MagicCastable {
    int     castMagic(Ship target, Mage mage);
    String  castSpell(Ship target, Mage mage);
    boolean hasCounterMage(Element enemyElement);
    void    displayMageRoster(boolean showSpell);
}
