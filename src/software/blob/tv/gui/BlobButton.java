package software.blob.tv.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

/**
 * Swing button that doesn't look like crap
 */
public class BlobButton extends JLabel implements MouseListener {

    private static final String TAG = "BlobButton";
    private static final Color DEFAULT_COLOR = new Color(0xFF333344);
    private static final Color HOVER_COLOR = new Color(0xFF555566);
    private static final Color PRESS_COLOR = new Color(0xFF222233);
    private static final Cursor POINTER = new Cursor(Cursor.HAND_CURSOR);

    private Color _fill = DEFAULT_COLOR;
    private boolean _hovering = false;
    private boolean _pressed = false;
    private Runnable _onClick;

    public BlobButton(String name) {
        super(name);
        setHorizontalAlignment(CENTER);
        setAlignmentX(Button.CENTER_ALIGNMENT);
        setBorder(new EmptyBorder(4, 4, 4, 4));
        addMouseListener(this);
    }

    public void setOnClick(Runnable r) {
        _onClick = r;
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(isEnabled() ? _fill : Color.DARK_GRAY);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
        g.setColor(isEnabled() ? Color.WHITE : Color.GRAY);

        FontMetrics fm = g.getFontMetrics();
        Rectangle2D textSize = fm.getStringBounds(getText(), g);
        g.drawString(getText(), (int)(getWidth() - textSize.getWidth()) / 2, (int)(getHeight() - textSize.getHeight() / 2));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(_onClick != null)
            _onClick.run();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        _fill = PRESS_COLOR;
        _pressed = true;
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        _fill = (_hovering ? HOVER_COLOR : DEFAULT_COLOR);
        _pressed = false;
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setCursor(POINTER);
        _fill = (_pressed ? PRESS_COLOR : HOVER_COLOR);
        _hovering = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getDefaultCursor());
        _fill = (_pressed ? PRESS_COLOR : DEFAULT_COLOR);
        _hovering = false;
        repaint();
    }
}
