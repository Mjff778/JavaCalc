package handcalc.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The header above the calculator: the app title and the history window.
 */
public class HeaderBar extends GlassPanel {

    public HeaderBar(Runnable openHistory) {
        super(new BorderLayout(16, 0), null, 2);

        JLabel title = new JLabel("Handwritten Calculator");
        title.setFont(Theme.font(Font.BOLD, 20f));
        title.setForeground(Theme.TEXT);

        RoundedPanel logo = new RoundedPanel(null, Theme.ACCENT, null, 14);
        logo.setPreferredSize(new Dimension(14, 14));
        JPanel logoHolder = new JPanel(new GridBagLayout());
        logoHolder.setOpaque(false);
        logoHolder.add(logo);
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.add(logoHolder, BorderLayout.WEST);
        brand.add(title, BorderLayout.CENTER);

        JButton history = Theme.secondary(new JButton("History…"));
        history.setName("historyButton");
        history.addActionListener(e -> openHistory.run());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(history);

        add(brand, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }
}
