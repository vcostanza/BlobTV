package software.blob.tv.gui;

import software.blob.tv.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

/**
 * Customized check box for BlobTV GUI
 */
public class BlobCheckBox extends JCheckBox implements MouseListener {

    private static final Color DEFAULT_BG = Constants.BG1;
    private static final Color HOVER_BG = Constants.BG2;
    private static final Cursor POINTER = new Cursor(Cursor.HAND_CURSOR);

    private boolean _hovering = false;
    private int _boxPadding = 3;

    public BlobCheckBox(String name) {
        super(name);
        addMouseListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(DEFAULT_BG);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (_hovering) {
            g.setColor(HOVER_BG);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
        }

        g.setColor(Constants.BG4);
        int boxSize = getHeight() - (_boxPadding * 2);
        g.fillRoundRect(_boxPadding, _boxPadding, boxSize, boxSize, 4, 4);

        if (isSelected()) {
            int checkSize = getHeight() - (_boxPadding * 4);
            g.setColor(Constants.BG8);
            g.fillRect(_boxPadding * 2, _boxPadding * 2, checkSize, checkSize);
        }

        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D textSize = fm.getStringBounds(getText(), g);
        g.drawString(getText(), getHeight(), (int)(getHeight() - textSize.getHeight() / 2));
    }

    public void setBoxPadding(int padding) {
        _boxPadding = padding;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setCursor(POINTER);
        _hovering = true;
        repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setCursor(Cursor.getDefaultCursor());
        _hovering = false;
        repaint();
    }
}
