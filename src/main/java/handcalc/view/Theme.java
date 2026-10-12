package handcalc.view;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

/**
 * Colours, fonts and button styles shared by every window.
 */
public final class Theme {

    public static final Color BACKGROUND = new Color(0xF5F5F7);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE0E0E6);
    public static final Color GRID_DOT = new Color(0xE3E3EA);
    public static final Color TEXT = new Color(0x1D1D1F);
    public static final Color MUTED = new Color(0x86868B);
    public static final Color ACCENT = new Color(0x4F46E5);
    public static final Color ACCENT_SOFT = new Color(0xECEBFE);
    public static final Color SUCCESS = new Color(0x1E8E3E);
    public static final Color SUCCESS_SOFT = new Color(0xE4F4E8);
    public static final Color ERROR = new Color(0xD93025);

    private Theme() {
    }

    /** Switches Swing to the modern look. Call once, before any window is created. */
    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("Panel.background", BACKGROUND);
    }

    public static Font font(int style, float size) {
        return UIManager.getFont("defaultFont") == null
                ? new Font(Font.SANS_SERIF, style, Math.round(size))
                : UIManager.getFont("defaultFont").deriveFont(style, size);
    }

    /** Filled, rounded call-to-action button. */
    public static JButton primary(JButton button) {
        button.putClientProperty("FlatLaf.style",
                "arc: 999; borderWidth: 0; focusWidth: 0; margin: 9,22,9,22;"
                        + "background: #4F46E5; foreground: #FFFFFF;"
                        + "hoverBackground: #4338CA; pressedBackground: #3730A3");
        button.setFont(font(Font.BOLD, 15f));
        return button;
    }

    /** Quiet outlined button. */
    public static JButton secondary(JButton button) {
        button.putClientProperty("FlatLaf.style",
                "arc: 999; focusWidth: 0; margin: 8,18,8,18;"
                        + "background: #FFFFFF; borderColor: #D2D2D7;"
                        + "hoverBackground: #F5F5F7; pressedBackground: #E8E8ED");
        button.setFont(font(Font.PLAIN, 14f));
        return button;
    }

    /** Quiet outlined button that stays tinted while it is switched on. */
    public static JToggleButton toggle(JToggleButton button) {
        button.putClientProperty("FlatLaf.style",
                "arc: 999; focusWidth: 0; margin: 8,18,8,18;"
                        + "background: #FFFFFF; borderColor: #D2D2D7;"
                        + "hoverBackground: #F5F5F7; pressedBackground: #E8E8ED;"
                        + "selectedBackground: #ECEBFE; selectedForeground: #4F46E5");
        button.setFont(font(Font.PLAIN, 14f));
        return button;
    }

    /** A chip that stands out from the others: the choice already made. */
    public static JButton currentChip(JButton button) {
        button.putClientProperty("FlatLaf.style",
                "arc: 14; borderWidth: 0; focusWidth: 0; margin: 4,14,4,14;"
                        + "background: #4F46E5; foreground: #FFFFFF;"
                        + "hoverBackground: #4338CA; pressedBackground: #3730A3");
        button.setFont(font(Font.BOLD, 20f));
        return button;
    }

    /** Small rounded tag-like button, e.g. for one recognized symbol. */
    public static JButton chip(JButton button) {
        button.putClientProperty("FlatLaf.style",
                "arc: 14; borderWidth: 0; focusWidth: 0; margin: 4,14,4,14;"
                        + "background: #ECEBFE; foreground: #4F46E5;"
                        + "hoverBackground: #DDDAFD; pressedBackground: #CFCBFB");
        button.setFont(font(Font.BOLD, 20f));
        return button;
    }

    public static <T extends JComponent> T muted(T component) {
        component.setForeground(MUTED);
        return component;
    }
}
