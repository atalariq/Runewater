package com.battleship.ui;

import java.util.List;
import java.util.Map;
import com.battleship.model.enums.CannonballType;

public record RewardViewModel(
    String playerStatusLine,
    List<MageInfo> roster,
    Map<CannonballType, Integer> ammo,
    int potions,
    List<RewardCardInfo> cards
) {}
