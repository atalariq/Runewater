package com.battleship.util;

import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.battleship.model.enums.CannonballType;
import com.battleship.ui.BattleViewModel;
import com.battleship.ui.MageInfo;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;

public class TerminalUi {

    private static final Pattern OPTION_PATTERN = Pattern.compile("^\\[(\\d+)]\\s*(.*)$");

    private static final TextColor BG = TextColor.Factory.fromString("#050816");
    private static final TextColor PANEL = TextColor.Factory.fromString("#0f172a");
    private static final TextColor PANEL_ALT = TextColor.Factory.fromString("#111c34");
    private static final TextColor PANEL_SOFT = TextColor.Factory.fromString("#16213f");
    private static final TextColor BORDER = TextColor.Factory.fromString("#2dd4bf");
    private static final TextColor BORDER_ALT = TextColor.Factory.fromString("#60a5fa");
    private static final TextColor ACCENT = TextColor.Factory.fromString("#f59e0b");
    private static final TextColor ALERT = TextColor.Factory.fromString("#fb7185");
    private static final TextColor TEXT = TextColor.Factory.fromString("#e2e8f0");
    private static final TextColor SUBTLE = TextColor.Factory.fromString("#94a3b8");
    private static final TextColor SUCCESS = TextColor.Factory.fromString("#86efac");

    private final InputHelper input;
    private Screen screen;
    private boolean started;
    private BattleViewModel currentBattleVm;
    private String lastCaptainName = "";

    public TerminalUi(InputHelper input) {
        this.input = input;
    }

    public void showScreen(String title, List<String> body) {
        showScreen(title, body, null);
    }

    public void showScreen(String title, List<String> body, String footer) {
        try {
            ensureScreen();
            renderScreen(title, normalize(body), footer, null, -1, "");
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menampilkan terminal UI", e);
        }
    }

    public void pause(String title, List<String> body) {
        try {
            ensureScreen();
            drainInput();
            while (true) {
                renderScreen(title, normalize(body), "Enter / Space untuk lanjut", null, -1, "");
                KeyStroke key = screen.readInput();
                if (key == null) {
                    continue;
                }
                if (key.getKeyType() == KeyType.Enter || key.getKeyType() == KeyType.EOF) {
                    return;
                }
                if (key.getKeyType() == KeyType.Character && key.getCharacter() == ' ') {
                    return;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input terminal", e);
        }
    }

    public String promptText(String title, List<String> body, String prompt) {
        StringBuilder value = new StringBuilder();
        try {
            ensureScreen();
            drainInput();
            while (true) {
                renderScreen(title, normalize(body), prompt, null, -1, value.toString());
                KeyStroke key = screen.readInput();
                if (key == null) {
                    continue;
                }

                if (key.getKeyType() == KeyType.Enter) {
                    return value.toString().trim();
                }
                if (key.getKeyType() == KeyType.Backspace || key.getKeyType() == KeyType.Delete) {
                    if (!value.isEmpty()) {
                        value.deleteCharAt(value.length() - 1);
                    }
                    continue;
                }
                if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    char ch = key.getCharacter();
                    if (!Character.isISOControl(ch)) {
                        value.append(ch);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input terminal", e);
        }
    }

    public int promptChoice(String title, List<String> body, int min, int max, int defaultValue, String prompt) {
        List<String> lines = normalize(body);
        List<ChoiceGroup> groups = extractChoiceGroups(lines, min, max);
        int selected = Math.max(0, Math.min(groups.size() - 1, defaultValue - min));

        try {
            ensureScreen();
            drainInput();
            while (true) {
                int selectedValue = groups.isEmpty() ? defaultValue : groups.get(selected).option().value();
                renderScreen(title, lines, prompt, groups, selectedValue, "");
                KeyStroke key = screen.readInput();
                if (key == null) {
                    continue;
                }

                if (key.getKeyType() == KeyType.ArrowUp || key.getKeyType() == KeyType.ArrowLeft) {
                    selected = (selected - 1 + groups.size()) % groups.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.ArrowDown || key.getKeyType() == KeyType.ArrowRight) {
                    selected = (selected + 1) % groups.size();
                    continue;
                }
                if (key.getKeyType() == KeyType.Enter) {
                    return groups.isEmpty() ? defaultValue : groups.get(selected).option().value();
                }
                if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    int digit = Character.digit(key.getCharacter(), 10);
                    if (digit >= min && digit <= max) {
                        return digit;
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input terminal", e);
        }
    }

    public List<String> wrapParagraph(String text) {
        return wrap(text, 48);
    }

    // -------------------------------------------------------------------------
    // Screen-typed interface (Foundation)
    // -------------------------------------------------------------------------

    public int showTitleScreen(com.battleship.ui.TitleViewModel vm) {
        try {
            ensureScreen();
            drainInput();
            String namePrompt = (vm.prompt() != null && !vm.prompt().isBlank())
                ? vm.prompt() : "Masukkan nama kapten:";
            lastCaptainName = promptText(vm.bannerTitle(), List.of(namePrompt), "Nama:");
            if (lastCaptainName.isBlank()) {
                lastCaptainName = "Nusantara";
            }
            String choiceFooter = (vm.footer() != null && !vm.footer().isBlank())
                ? vm.footer() : "Panah / 1-6 pilih, Enter konfirmasi";
            return promptChoice(vm.bannerTitle(), vm.body(), 1, 6, 1, choiceFooter);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menampilkan title screen", e);
        }
    }

    public String getLastCaptainName() {
        return lastCaptainName;
    }

    public int showStageSelect(com.battleship.ui.StageViewModel vm) {
        try {
            ensureScreen();
            drainInput();
            int selected = 0;
            int count = vm.enemies().size();
            while (true) {
                screen.clear();
                screen.setCursorPosition(null);
                screen.doResizeIfNecessary();
                TerminalSize sz = screen.getTerminalSize();
                TextGraphics g = screen.newTextGraphics();
                g.setBackgroundColor(BG);
                g.setForegroundColor(TEXT);
                g.fill(' ');

                drawBanner(g, sz, vm.stageTitle());

                int mx = 2;
                int my = 7;
                int mw = sz.getColumns() - 4;

                drawPanel(g, mx, my, mw, 3, BORDER_ALT, PANEL);
                if (vm.playerStatusLine() != null && !vm.playerStatusLine().isBlank()) {
                    drawText(g, mx + 2, my + 1, trim(vm.playerStatusLine(), mw - 4), TEXT, PANEL, false);
                }
                int cardY = my + 5;
                int gap = 2;
                int cardW = (mw - 4 - gap * (count - 1)) / count;
                int cardH = 13;

                for (int i = 0; i < count; i++) {
                    com.battleship.ui.EnemyCardInfo e = vm.enemies().get(i);
                    boolean isSel = i == selected;
                    int cx = mx + 2 + i * (cardW + gap);
                    TextColor fill = isSel ? PANEL_SOFT : PANEL;
                    TextColor border = isSel ? ACCENT : BORDER_ALT;
                    drawPanel(g, cx, cardY, cardW, cardH, border, fill);

                    int row = cardY + 1;
                    drawText(g, cx + 1, row, "[" + (i + 1) + "]", isSel ? ACCENT : SUBTLE, fill, true);
                    int nameX = cx + 5;
                    String nameStr = trim(e.name(), cardW - 14);
                    drawText(g, nameX, row, nameStr, TEXT, fill, true);
                    if (e.isElite()) {
                        drawText(g, nameX + nameStr.length() + 1, row, "[ELITE]", ACCENT, fill, true);
                    }
                    row++;

                    String elemStr = e.elementSym();
                    drawText(g, cx + 2, row, elemStr, elementColor(elemStr), fill, true);
                    row++;

                    int barW = cardW - 6;
                    int filled = barW > 0 ? (int) Math.round((double) e.hp() / Math.max(1, e.maxHp()) * barW) : 0;
                    drawBar(g, cx + 2, row, barW, filled, ALERT);
                    row++;

                    String hpStr = e.hp() + "/" + e.maxHp();
                    drawText(g, cx + 2, row, hpStr, TEXT, fill, true);
                    row++;

                    drawText(g, cx + 2, row, "Dmg: ~" + e.damage(), TEXT, fill, false);
                    row++;

                    String tn = e.traitName();
                    if (tn != null && !tn.isBlank() && !"Biasa".equals(tn)) {
                        drawText(g, cx + 2, row, tn, ACCENT, fill, true);
                    }
                    row++;

                    drawText(g, cx + 2, row, "Bounty: $" + e.bounty(), ACCENT, fill, false);
                    row++;
                    row++;

                    if (i < vm.counterHints().size()) {
                        String hint = vm.counterHints().get(i);
                        String[] parts = hint.split(" \\| ");
                        for (int pi = 0; pi < parts.length && pi < 2; pi++) {
                            String part = trim(parts[pi], cardW - 4);
                            TextColor hc = part.contains("Tersedia") ? SUCCESS : SUBTLE;
                            drawText(g, cx + 2, row++, part, hc, fill, false);
                        }
                    }
                }

                int footerY = cardY + cardH + 1;
                drawPanel(g, mx + 2, footerY, mw - 4, 3, BORDER_ALT, PANEL_SOFT);
                drawText(g, mx + 4, footerY + 1, "Select enemy (1-3, Esc to refresh):", TEXT, PANEL_SOFT, false);

                screen.refresh();
                KeyStroke key = screen.readInput();
                if (key == null) continue;

                if (key.getKeyType() == KeyType.ArrowLeft) {
                    selected = (selected - 1 + count) % count;
                } else if (key.getKeyType() == KeyType.ArrowRight) {
                    selected = (selected + 1) % count;
                } else if (key.getKeyType() == KeyType.Enter) {
                    return selected + 1;
                } else if (key.getKeyType() == KeyType.Escape) {
                    return 0;
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    int digit = Character.digit(key.getCharacter(), 10);
                    if (digit >= 1 && digit <= count) return digit;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input stage select", e);
        }
    }

    public void showBattle(BattleViewModel vm) {
        currentBattleVm = vm;
        try {
            ensureScreen();
            renderBattleLayout(vm, -1, null, null);
            screen.refresh();
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menampilkan battle UI", e);
        }
    }

    public int promptMainCommand() {
        try {
            ensureScreen();
            int highlight = 1;
            while (true) {
                renderBattleLayout(currentBattleVm, highlight, null, null);
                screen.refresh();

                KeyStroke key = screen.readInput();
                if (key == null) continue;

                if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    char ch = key.getCharacter();
                    if (ch >= '1' && ch <= '5') return ch - '0';
                    if (ch == '0') return 0;
                }
                if (key.getKeyType() == KeyType.Escape) return 0;

                if (key.getKeyType() == KeyType.ArrowUp) {
                    highlight = Math.max(1, highlight - 1);
                }
                if (key.getKeyType() == KeyType.ArrowDown) {
                    highlight = Math.min(5, highlight + 1);
                }
                if (key.getKeyType() == KeyType.Enter) {
                    return highlight;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input battle", e);
        }
    }

    public int showOverlay(String title, List<String> items) {
        try {
            ensureScreen();
            while (true) {
                renderBattleLayout(currentBattleVm, -1, items, title);
                screen.refresh();

                KeyStroke key = screen.readInput();
                if (key == null) continue;

                if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    char ch = key.getCharacter();
                    int digit = Character.digit(ch, 10);
                    if (digit >= 1 && digit <= items.size()) return digit;
                    if (ch == '0') return 0;
                }
                if (key.getKeyType() == KeyType.Escape) return 0;

                if (key.getKeyType() == KeyType.ArrowUp) {
                    currentBattleVm = currentBattleVm; // no-op
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input overlay", e);
        }
    }

    public int showRewardSelect(com.battleship.ui.RewardViewModel vm) {
        try {
            ensureScreen();
            drainInput();
            int selected = 0;
            int count = vm.cards().size();
            while (true) {
                screen.clear();
                screen.setCursorPosition(null);
                screen.doResizeIfNecessary();
                TerminalSize sz = screen.getTerminalSize();
                TextGraphics g = screen.newTextGraphics();
                g.setBackgroundColor(BG);
                g.setForegroundColor(TEXT);
                g.fill(' ');

                drawBanner(g, sz, "Reward!");

                int mx = 2;
                int my = 7;
                int mw = sz.getColumns() - 4;

                drawPanel(g, mx, my, mw, 6, BORDER_ALT, PANEL);
                int infoRow = my + 1;
                if (vm.playerStatusLine() != null && !vm.playerStatusLine().isBlank()) {
                    drawText(g, mx + 2, infoRow++, trim(vm.playerStatusLine(), mw - 4), TEXT, PANEL, false);
                }

                StringBuilder roster = new StringBuilder("Roster: ");
                if (vm.roster() != null && !vm.roster().isEmpty()) {
                    for (int i = 0; i < Math.min(3, vm.roster().size()); i++) {
                        com.battleship.ui.MageInfo m = vm.roster().get(i);
                        if (i > 0) roster.append(", ");
                        roster.append("Lv.").append(m.level()).append(" ").append(m.name()).append(m.elementSym());
                    }
                    if (vm.roster().size() > 3) roster.append(" ...");
                } else {
                    roster.append("(kosong)");
                }
                drawText(g, mx + 2, infoRow++, trim(roster.toString(), mw - 4), TEXT, PANEL, false);

                StringBuilder ammo = new StringBuilder("Ammo: ");
                if (vm.ammo() != null) {
                    boolean first = true;
                    for (var entry : vm.ammo().entrySet()) {
                        if (!first) ammo.append(" + ");
                        first = false;
                        ammo.append(entry.getKey().getDisplayName()).append("(")
                            .append(entry.getValue() < 0 ? "\u221E" : entry.getValue()).append(")");
                    }
                }
                drawText(g, mx + 2, infoRow++, trim(ammo.toString(), mw - 4), TEXT, PANEL, false);

                drawText(g, mx + 2, infoRow, "Potions: " + vm.potions(), TEXT, PANEL, false);

                int cardY = my + 8;
                int gap = 2;
                int cardW = (mw - 4 - gap * (count - 1)) / count;
                int cardH = 9;

                for (int i = 0; i < count; i++) {
                    com.battleship.ui.RewardCardInfo card = vm.cards().get(i);
                    boolean isSel = i == selected;
                    int cx = mx + 2 + i * (cardW + gap);
                    TextColor fill = isSel ? PANEL_SOFT : PANEL;
                    TextColor border = isSel ? ACCENT : BORDER_ALT;
                    drawPanel(g, cx, cardY, cardW, cardH, border, fill);

                    int row = cardY + 1;
                    drawText(g, cx + 2, row, "[" + (i + 1) + "]", isSel ? ACCENT : SUBTLE, fill, true);
                    row++;

                    drawText(g, cx + 2, row, trim(card.title(), cardW - 6), TEXT, fill, true);
                    row++;

                    if (card.typeDescription() != null && !card.typeDescription().isBlank()) {
                        drawText(g, cx + 2, row, trim(card.typeDescription(), cardW - 6), ACCENT, fill, false);
                    }
                    row++;
                    row++;

                    for (String line : wrap(card.description(), cardW - 6)) {
                        if (row >= cardY + cardH - 2) break;
                        drawText(g, cx + 2, row++, line, SUBTLE, fill, false);
                    }
                }

                int footerY = cardY + cardH + 1;
                drawPanel(g, mx + 2, footerY, mw - 4, 3, BORDER_ALT, PANEL_SOFT);
                drawText(g, mx + 4, footerY + 1, "Select reward (1-3, Esc to re-roll):", TEXT, PANEL_SOFT, false);

                screen.refresh();
                KeyStroke key = screen.readInput();
                if (key == null) continue;

                if (key.getKeyType() == KeyType.ArrowLeft) {
                    selected = (selected - 1 + count) % count;
                } else if (key.getKeyType() == KeyType.ArrowRight) {
                    selected = (selected + 1) % count;
                } else if (key.getKeyType() == KeyType.Enter) {
                    return selected + 1;
                } else if (key.getKeyType() == KeyType.Escape) {
                    return 0;
                } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                    int digit = Character.digit(key.getCharacter(), 10);
                    if (digit >= 1 && digit <= count) return digit;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca input reward select", e);
        }
    }

    public void showInfo(com.battleship.ui.TitleViewModel vm) {
        pause(vm.bannerTitle(), vm.body());
    }

    public void shutdown() {
        if (screen == null) {
            return;
        }
        try {
            if (started) {
                screen.stopScreen();
            }
            screen.close();
        } catch (IOException ignored) {
        } finally {
            screen = null;
            started = false;
        }
    }

    private void ensureScreen() throws IOException {
        if (screen != null) {
            screen.doResizeIfNecessary();
            return;
        }

        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        Terminal terminal;
        if (isWindows()) {
            SwingTerminalFrame terminalFrame = factory.createSwingTerminal();
            terminalFrame.setTitle("Runewater");
            terminalFrame.setSize(1120, 760);
            terminalFrame.setLocationRelativeTo(null);
            terminalFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
            terminalFrame.setVisible(true);
            terminal = terminalFrame;
        } else {
            terminal = factory.createTerminal();
        }

        screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.setCursorPosition(null);
        started = true;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void renderScreen(
            String title,
            List<String> body,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue
    ) throws IOException {
        screen.clear();
        screen.setCursorPosition(null);
        screen.doResizeIfNecessary();
        TerminalSize size = screen.getTerminalSize();
        TextGraphics g = screen.newTextGraphics();

        g.setBackgroundColor(BG);
        g.setForegroundColor(TEXT);
        g.fill(' ');

        drawBanner(g, size, title);
        renderGenericScreen(g, size, body, footer, groups, selectedValue, inputValue);

        screen.refresh(Screen.RefreshType.COMPLETE);
    }

    private void drawBanner(TextGraphics g, TerminalSize size, String title) {
        int width = size.getColumns() - 4;
        drawPanel(g, 2, 1, width, 5, BORDER, PANEL_SOFT);
        drawText(g, 5, 2, "RUNEWATER", SUCCESS, PANEL_SOFT, true);
        drawText(g, 5, 3, title, TEXT, PANEL_SOFT, false);
        drawText(g, size.getColumns() - 30, 3, "Arrow Keys | Enter", SUBTLE, PANEL_SOFT, false);
        drawText(g, size.getColumns() - 38, 2, "~ tide ~ rune ~ tide ~", BORDER_ALT, PANEL_SOFT, false);
    }

    private void renderGenericScreen(
            TextGraphics g,
            TerminalSize size,
            List<String> body,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue
    ) {
        int mainX = 2;
        int mainY = 7;
        int mainW = size.getColumns() - 4;
        int mainH = size.getRows() - 9;

        drawPanel(g, mainX, mainY, mainW, mainH, BORDER_ALT, PANEL);

        List<String> introLines = extractIntroLines(body);
        ChoiceGroup selectedGroup = findSelectedGroup(groups, selectedValue);

        if (shouldUseCardLayout(groups)) {
            renderCardSelection(g, mainX, mainY, mainW, mainH, introLines, footer, groups, selectedValue, inputValue);
            return;
        }

        int leftW = groups != null && !groups.isEmpty() ? Math.max(42, mainW - 32) : mainW - 4;
        int rightW = groups != null && !groups.isEmpty() ? mainW - leftW - 3 : 0;

        drawPanel(g, mainX + 2, mainY + 2, leftW, mainH - 7, BORDER, PANEL_ALT);
        drawText(g, mainX + 4, mainY + 3, "Info", SUCCESS, PANEL_ALT, true);

        int textRow = mainY + 5;
        int textWidth = leftW - 4;
        for (String line : introLines) {
            for (String wrapped : wrap(line, textWidth)) {
                if (textRow >= mainY + mainH - 8) {
                    break;
                }
                drawText(g, mainX + 4, textRow++, wrapped, TEXT, PANEL_ALT, false);
            }
        }

        if (selectedGroup != null && hasPreviewDetails(selectedGroup)) {
            if (textRow < mainY + mainH - 10) {
                textRow++;
            }
            drawText(g, mainX + 4, textRow++, "Selected", ACCENT, PANEL_ALT, true);
            drawText(g, mainX + 4, textRow++, selectedGroup.option().label(), TEXT, PANEL_ALT, true);
            for (String detail : selectedGroup.details()) {
                for (String wrapped : wrap(detail, textWidth)) {
                    if (textRow >= mainY + mainH - 8) {
                        break;
                    }
                    drawText(g, mainX + 4, textRow++, wrapped, SUBTLE, PANEL_ALT, false);
                }
            }
        }

        if (groups != null && !groups.isEmpty()) {
            int menuX = mainX + leftW + 3;
            drawPanel(g, menuX, mainY + 2, rightW - 1, mainH - 7, ACCENT, PANEL_SOFT);
            drawText(g, menuX + 2, mainY + 3, "Choose", ACCENT, PANEL_SOFT, true);

            int optionRow = mainY + 5;
            int labelWidth = rightW - 6;
            for (ChoiceGroup group : groups) {
                if (optionRow >= mainY + mainH - 8) {
                    break;
                }
                boolean selected = group.option().value() == selectedValue;
                TextColor bg = selected ? ACCENT : PANEL_SOFT;
                TextColor fg = selected ? PANEL : TEXT;
                String label = (selected ? "> " : "  ") + group.option().label();
                drawText(g, menuX + 2, optionRow++, pad(label, labelWidth), fg, bg, selected);
            }
        }

        if (footer != null && !footer.isBlank()) {
            drawPanel(g, mainX + 2, mainY + mainH - 4, mainW - 4, 3, BORDER_ALT, PANEL_SOFT);
            String footerText = inputValue.isBlank() ? footer : footer + " " + inputValue + "_";
            drawText(g, mainX + 4, mainY + mainH - 3, footerText, TEXT, PANEL_SOFT, false);
        }
    }

    private void renderCardSelection(
            TextGraphics g,
            int mainX,
            int mainY,
            int mainW,
            int mainH,
            List<String> introLines,
            String footer,
            List<ChoiceGroup> groups,
            int selectedValue,
            String inputValue
    ) {
        drawPanel(g, mainX + 2, mainY + 2, mainW - 4, 6, BORDER, PANEL_ALT);
        drawText(g, mainX + 4, mainY + 3, "Encounter", SUCCESS, PANEL_ALT, true);

        int introRow = mainY + 4;
        for (String line : introLines) {
            for (String wrapped : wrap(line, mainW - 10)) {
                if (introRow >= mainY + 7) {
                    break;
                }
                drawText(g, mainX + 4, introRow++, wrapped, TEXT, PANEL_ALT, false);
            }
        }

        int gap = 2;
        int cardY = mainY + 10;
        int cardW = (mainW - 8 - (gap * (groups.size() - 1))) / groups.size();

        for (int i = 0; i < groups.size(); i++) {
            ChoiceGroup group = groups.get(i);
            boolean selected = group.option().value() == selectedValue;
            int cardX = mainX + 2 + i * (cardW + gap);
            TextColor fill = selected ? PANEL_SOFT : PANEL;
            TextColor border = selected ? ACCENT : BORDER_ALT;
            drawPanel(g, cardX, cardY, cardW, mainH - 15, border, fill);
            drawText(g, cardX + 2, cardY + 1, "[" + group.option().value() + "]", selected ? ACCENT : SUBTLE, fill, true);
            drawText(g, cardX + 6, cardY + 1, trim(group.option().label(), cardW - 10), TEXT, fill, true);

            int row = cardY + 3;
            for (String detail : group.details()) {
                for (String wrapped : wrap(detail, cardW - 4)) {
                    if (row >= cardY + mainH - 18) {
                        break;
                    }
                    drawText(g, cardX + 2, row++, wrapped, selected ? TEXT : SUBTLE, fill, false);
                }
            }

            if (selected) {
                drawText(g, cardX + 2, cardY + mainH - 17, "> ready <", ACCENT, fill, true);
            }
        }

        if (footer != null && !footer.isBlank()) {
            drawPanel(g, mainX + 2, mainY + mainH - 4, mainW - 4, 3, BORDER_ALT, PANEL_SOFT);
            String footerText = inputValue.isBlank() ? footer : footer + " " + inputValue + "_";
            drawText(g, mainX + 4, mainY + mainH - 3, footerText, TEXT, PANEL_SOFT, false);
        }
    }

    private void renderBattleLayout(BattleViewModel vm, int highlight, List<String> overlayItems, String overlayTitle) {
        screen.clear();
        screen.setCursorPosition(null);
        TerminalSize size = screen.getTerminalSize();
        TextGraphics g = screen.newTextGraphics();

        g.setBackgroundColor(BG);
        g.setForegroundColor(TEXT);
        g.fill(' ');

        int W = size.getColumns();
        int H = size.getRows();
        int left = 2;
        int contentW = W - 8;

        drawText(g, left, 1, "RUNEWATER", SUCCESS, BG, true);
        if (vm.stageInfo() != null && !vm.stageInfo().isBlank()) {
            int stageX = left + contentW - vm.stageInfo().length();
            if (stageX > left) {
                drawText(g, stageX, 1, vm.stageInfo(), TEXT, BG, false);
            }
        }

        String sep = repeat("─", contentW);
        drawText(g, left, 2, sep, BORDER, BG, false);

        String enemyName = "Enemy: " + vm.enemyName();
        if (vm.enemyElementSym() != null && !vm.enemyElementSym().isBlank()) {
            enemyName += " " + vm.enemyElementSym();
        }
        drawText(g, left, 3, enemyName, TEXT, BG, false);

        String rageTag = vm.enemyEnraged() ? " [RAGE!]" : "";
        String traitTag = "";
        if (vm.enemyTraitName() != null && !vm.enemyTraitName().isBlank()) {
            traitTag = "[" + vm.enemyTraitName() + "]" + rageTag;
        } else if (vm.enemyEnraged()) {
            traitTag = rageTag;
        }
        if (!traitTag.isEmpty()) {
            int traitX = left + contentW - traitTag.length();
            if (traitX >= left + 4) {
                drawText(g, traitX, 3, traitTag, vm.enemyEnraged() ? ALERT : ACCENT, BG, true);
            }
        }

        drawBattleHpBar(g, left, 4, contentW, vm.enemyHp(), vm.enemyMaxHp(), vm.enemyStatus(), ALERT);
        drawText(g, left, 5, sep, BORDER, BG, false);

        int logTop = 6;
        int logBottom = H - 9;
        List<String> log = vm.battleLog();
        if (log != null) {
            int maxVisible = logBottom - logTop;
            int startIdx = Math.max(0, log.size() - maxVisible);
            int row = logTop;
            for (int i = startIdx; i < log.size() && row < logBottom; i++) {
                drawText(g, left, row++, log.get(i), TEXT, BG, false);
            }
        }

        if (overlayItems != null) {
            drawOverlayBox(g, left, left + contentW, logTop, logBottom, overlayTitle, overlayItems);
        }

        drawText(g, left, H - 8, sep, BORDER, BG, false);
        drawText(g, left, H - 7, "Crew: " + vm.playerName(), SUCCESS, BG, true);
        if (vm.playerShielded()) {
            String shieldStr = " [SHIELD]";
            drawText(g, left + 6 + vm.playerName().length(), H - 7, shieldStr, ACCENT, BG, true);
        }
        drawBattleHpBar(g, left, H - 6, contentW, vm.playerHp(), vm.playerMaxHp(), vm.playerStatus(), SUCCESS);
        drawText(g, left, H - 5, sep, BORDER, BG, false);

        StringBuilder cmdLine = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            String prefix = (i == highlight) ? ">" : " ";
            cmdLine.append(prefix).append("[").append(i).append("]").append(getCommandLabel(i));
            if (i < 5) cmdLine.append("  ");
        }
        drawText(g, left, H - 4, cmdLine.toString(), TEXT, BG, false);

        if (highlight >= 1 && highlight <= 5) {
            String focus = getFocusDetail(vm, highlight);
            if (focus != null && !focus.isEmpty()) {
                drawText(g, left, H - 3, "  " + focus, SUBTLE, BG, false);
            }
        }
    }

    private void drawBattleHpBar(TextGraphics g, int x, int y, int width, int current, int max, String statusTag, TextColor barColor) {
        int safeMax = Math.max(1, max);
        int barW = Math.min(width - 22, 30);
        int filled = (int) Math.round((double) current / safeMax * barW);
        drawBar(g, x, y, barW, filled, barColor);
        String hpText = String.format(" %d/%d", current, max);
        drawText(g, x + barW + 1, y, hpText, TEXT, BG, true);
        if (statusTag != null && !statusTag.isBlank()) {
            drawText(g, x + barW + 1 + hpText.length(), y, " " + statusTag, statusTag.contains("FROZEN") ? BORDER_ALT : ALERT, BG, true);
        }
    }

    private void drawBar(TextGraphics g, int x, int y, int width, int filled, TextColor fillColor) {
        for (int i = 0; i < width; i++) {
            boolean on = i < filled;
            drawText(g, x + i, y, " ", TEXT, on ? fillColor : PANEL, false);
        }
    }

    private void drawOverlayBox(TextGraphics g, int left, int right, int logTop, int logBottom, String title, List<String> items) {
        int contentW = right - left;
        int boxW = Math.min(44, contentW - 4);
        int boxH = Math.min(items.size() + 4, logBottom - logTop);
        int boxX = left + (contentW - boxW) / 2;
        int boxY = logTop + Math.max(0, ((logBottom - logTop) - boxH) / 2);

        drawPanel(g, boxX, boxY, boxW, boxH, BORDER, PANEL_ALT);

        drawText(g, boxX + 2, boxY + 1, title, ACCENT, PANEL_ALT, true);

        int y = boxY + 3;
        for (int i = 0; i < items.size() && y < boxY + boxH - 2; i++) {
            drawText(g, boxX + 2, y++, items.get(i), TEXT, PANEL_ALT, false);
        }

        drawText(g, boxX + 2, boxY + boxH - 2, "[Esc/0] Batal", SUBTLE, PANEL_ALT, false);
    }

    private static String getCommandLabel(int cmd) {
        return switch (cmd) {
            case 1 -> "Tembak";
            case 2 -> "Sihir";
            case 3 -> "Jurus";
            case 4 -> "Bertahan";
            case 5 -> "Potion";
            default -> "?";
        };
    }

    private static String getFocusDetail(BattleViewModel vm, int cmd) {
        return switch (cmd) {
            case 1 -> buildCannonFocus(vm);
            case 2 -> buildMagicFocus(vm);
            case 3 -> buildSpellFocus(vm);
            case 4 -> "Bertahan: -50% damage giliran ini";
            case 5 -> "Potion: " + vm.potions() + " tersisa, heal 50 HP";
            default -> "";
        };
    }

    private static String buildCannonFocus(BattleViewModel vm) {
        StringBuilder sb = new StringBuilder("Ammo:");
        for (CannonballType t : CannonballType.values()) {
            int count = vm.ammo().getOrDefault(t, 0);
            sb.append(" ").append(t.getDisplayName()).append("=");
            sb.append(count < 0 ? "∞" : count);
        }
        if (vm.enemyTraitName() != null && !vm.enemyTraitName().isBlank()) {
            sb.append(" | Trait: ").append(vm.enemyTraitName());
        }
        return sb.toString();
    }

    private static String buildMagicFocus(BattleViewModel vm) {
        List<MageInfo> roster = vm.roster();
        if (roster == null || roster.isEmpty()) return "Tidak ada Mage";
        StringBuilder sb = new StringBuilder("Mage: ");
        for (int i = 0; i < Math.min(3, roster.size()); i++) {
            MageInfo m = roster.get(i);
            sb.append(m.name()).append("(").append(m.elementSym()).append(") ");
        }
        if (roster.size() > 3) sb.append("+").append(roster.size() - 3).append(" lagi");
        return sb.toString();
    }

    private static String buildSpellFocus(BattleViewModel vm) {
        List<MageInfo> roster = vm.roster();
        if (roster == null || roster.isEmpty()) return "Tidak ada Mage";
        StringBuilder sb = new StringBuilder("Jurus: ");
        for (int i = 0; i < Math.min(3, roster.size()); i++) {
            MageInfo m = roster.get(i);
            sb.append(m.name()).append("=").append(m.spellUsed() ? "USED" : "READY").append(" ");
        }
        if (roster.size() > 3) sb.append("...");
        return sb.toString();
    }

    private static TextColor elementColor(String sym) {
        String s = sym == null ? "" : sym.trim();
        if (s.contains("FIRE"))   return TextColor.Factory.fromString("#ef4444");
        if (s.contains("WATER"))  return TextColor.Factory.fromString("#3b82f6");
        if (s.contains("STORM"))  return TextColor.Factory.fromString("#eab308");
        return TEXT;
    }

    private static String rarityStars(int weight) {
        if (weight <= 2) return "\u2605\u2605\u2605\u2605\u2605";
        if (weight <= 4) return "\u2605\u2605\u2605\u2605\u2606";
        if (weight <= 6) return "\u2605\u2605\u2605\u2606\u2606";
        if (weight <= 9) return "\u2605\u2605\u2606\u2606\u2606";
        return "\u2605\u2606\u2606\u2606\u2606";
    }

    private static String repeat(String s, int count) {
        if (count <= 0) return "";
        return s.repeat(count);
    }

    private ChoiceGroup findSelectedGroup(List<ChoiceGroup> groups, int selectedValue) {
        if (groups == null) {
            return null;
        }
        for (ChoiceGroup group : groups) {
            if (group.option().value() == selectedValue) {
                return group;
            }
        }
        return groups.isEmpty() ? null : groups.get(0);
    }

    private boolean shouldUseCardLayout(List<ChoiceGroup> groups) {
        if (groups == null || groups.isEmpty() || groups.size() > 3) {
            return false;
        }
        for (ChoiceGroup group : groups) {
            if (group.details().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPreviewDetails(ChoiceGroup group) {
        return group != null && !group.details().isEmpty();
    }

    private List<String> extractIntroLines(List<String> body) {
        List<String> intro = new ArrayList<>();
        for (String line : body) {
            Matcher matcher = OPTION_PATTERN.matcher(line.trim());
            if (!matcher.matches()) {
                intro.add(line);
            } else {
                break;
            }
        }
        return intro;
    }

    private void drawPanel(TextGraphics g, int x, int y, int width, int height, TextColor border, TextColor fill) {
        if (width <= 1 || height <= 1) {
            return;
        }
        g.setBackgroundColor(fill);
        g.setForegroundColor(border);
        g.fillRectangle(new TerminalPosition(x, y), new TerminalSize(width, height), ' ');
        g.drawRectangle(new TerminalPosition(x, y), new TerminalSize(width, height), '*');
    }

    private void drawText(TextGraphics g, int x, int y, String text, TextColor fg, TextColor bg, boolean bold) {
        g.setForegroundColor(fg);
        g.setBackgroundColor(bg);
        if (bold) {
            g.enableModifiers(SGR.BOLD);
        } else {
            g.disableModifiers(SGR.BOLD);
        }
        g.putString(x, y, text);
    }

    private List<ChoiceGroup> extractChoiceGroups(List<String> lines, int min, int max) {
        List<ChoiceGroup> groups = new ArrayList<>();
        ChoiceGroupBuilder current = null;

        for (String line : lines) {
            Matcher matcher = OPTION_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                int value = Integer.parseInt(matcher.group(1));
                if (value >= min && value <= max) {
                    if (current != null) {
                        groups.add(current.build());
                    }
                    current = new ChoiceGroupBuilder(new OptionLine(value, matcher.group(2).trim()));
                    continue;
                }
            }

            if (current != null && !line.isBlank()) {
                current.addDetail(line.trim());
            }
        }

        if (current != null) {
            groups.add(current.build());
        }

        if (groups.isEmpty()) {
            for (int value = min; value <= max; value++) {
                groups.add(new ChoiceGroup(new OptionLine(value, "Option " + value), List.of()));
            }
        }

        return groups;
    }

    private List<String> normalize(List<String> body) {
        return body == null ? Collections.emptyList() : body;
    }

    private List<String> wrap(String raw, int width) {
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            return List.of("");
        }

        List<String> wrapped = new ArrayList<>();
        String[] paragraphs = text.split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                wrapped.add("");
                continue;
            }

            String[] words = paragraph.trim().split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.isEmpty()) {
                    line.append(word);
                    continue;
                }

                if (line.length() + 1 + word.length() > width) {
                    wrapped.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line.append(' ').append(word);
                }
            }
            if (!line.isEmpty()) {
                wrapped.add(line.toString());
            }
        }
        return wrapped;
    }

    private String pad(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        return text + " ".repeat(width - text.length());
    }

    private String trim(String text, int width) {
        return text.length() <= width ? text : text.substring(0, Math.max(0, width - 3)) + "...";
    }

    private void drainInput() throws IOException {
        if (screen == null) {
            return;
        }
        while (screen.pollInput() != null) {
            // buang input lama
        }
    }

    private record OptionLine(int value, String label) { }

    private record ChoiceGroup(OptionLine option, List<String> details) { }

    private static final class ChoiceGroupBuilder {
        private final OptionLine option;
        private final List<String> details = new ArrayList<>();

        private ChoiceGroupBuilder(OptionLine option) {
            this.option = option;
        }

        private void addDetail(String detail) {
            details.add(detail);
        }

        private ChoiceGroup build() {
            return new ChoiceGroup(option, List.copyOf(details));
        }
    }
}
