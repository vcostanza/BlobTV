package software.blob.tv.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import software.blob.tv.CLInterface;
import software.blob.tv.Constants;
import software.blob.tv.obj.Playlist;

import javax.swing.*;
import java.awt.*;

/**
 * GUI for schedule builder
 */
public class BuilderGUI extends JPanel {

    private static final String TAG = "BuilderGUI";

    public BuilderGUI() {
        setBorder(BorderFactory.createLineBorder(Color.black));

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final BlobButton genSched = new BlobButton("Generate Schedule");
        add(genSched);
        genSched.setOnClick(() -> {
            genSched.setText("Generating...");
            genSched.setEnabled(false);
            Thread thr = new Thread() {
                public void start() {
                    CLInterface.genRandomSched();
                    genSched.setEnabled(true);
                    genSched.setText("Generate Schedule");
                }
            };
            thr.start();
        });

        final BlobButton checkBreaks = new BlobButton("Check Breaks");
        add(checkBreaks);
        checkBreaks.setOnClick(this::openBreaksWindow);

        final BlobButton commGraph = new BlobButton("Commercial Graph");
        add(commGraph);
        commGraph.setOnClick(this::graphCommercials);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(360, 240);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Constants.BG1);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void graphCommercials() {
        JFreeChart chart = ChartFactory.createPieChart("Commercials", new PlaylistDataset(new Playlist("Commercials")));
        ChartFrame frame = new ChartFrame("Commercials", chart, true);
        frame.pack();
        frame.setVisible(true);
    }

    private void openBreaksWindow() {
        JFrame f = new JFrame("Check Breaks");
        f.add(new CheckBreaksWindow());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
