package com.battleship.ui;

import java.util.List;

public record TitleViewModel(
    String bannerTitle,
    List<String> body,
    String footer,
    String prompt
) {}
