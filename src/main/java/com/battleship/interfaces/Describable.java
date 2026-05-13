package com.battleship.interfaces;

/**
 * Kontrak untuk semua entitas yang bisa mendeskripsikan dirinya sendiri.
 * Diimplementasikan oleh: CannonballType, SpellType, PlayerShip, EnemyShip
 */
public interface Describable {
    String getFullDescription();
}
