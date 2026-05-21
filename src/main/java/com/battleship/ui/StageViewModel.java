package com.battleship.ui;

import java.util.List;

public record StageViewModel(
    String playerStatusLine,
    String stageTitle,
    List<EnemyCardInfo> enemies,
    List<String> counterHints
) {}
