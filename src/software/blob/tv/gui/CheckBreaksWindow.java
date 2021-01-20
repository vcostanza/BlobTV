package software.blob.tv.gui;

import software.blob.tv.Config;
import software.blob.tv.Constants;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;
import software.blob.tv.obj.ShowInfo;
import software.blob.tv.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * GUI for checking episode breaks
 */
public class CheckBreaksWindow extends JPanel implements KeyListener {

    private static final String TAG = "CheckBreaksWindow";

    public interface BreakCallback {
        void forBreakTime(String key, double time);
    }

    private final JTextField _showName;
    private final JList<String> _breakList;
    private final BlobCheckBox _introCB, _midsCB, _creditsCB;
    private final BlobButton _thumbBtn;

    private File _showDir;
    private ShowInfo _curInfo;

    public CheckBreaksWindow() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBackground(Constants.TRANSLUCENT);

        JPanel topY = new JPanel();
        topY.setLayout(new BoxLayout(topY, BoxLayout.Y_AXIS));
        topY.setBackground(Constants.TRANSLUCENT);

        JPanel showPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        showPanel.setBackground(Constants.TRANSLUCENT);
        //showPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        _showName = new JTextField("", 24);
        _showName.setForeground(Constants.TXT1);
        _showName.setBackground(Constants.BG3);
        _showName.setHorizontalAlignment(JTextField.CENTER);
        _showName.addKeyListener(this);
        showPanel.add(_showName);
        topY.add(showPanel);

        // Which breaks to check
        JPanel cbPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cbPanel.setBackground(Constants.TRANSLUCENT);
        _introCB = new BlobCheckBox("Intro");
        cbPanel.add(_introCB);
        _midsCB = new BlobCheckBox("Mid-breaks");
        _midsCB.setSelected(true);
        cbPanel.add(_midsCB);
        _creditsCB = new BlobCheckBox("Credits");
        cbPanel.add(_creditsCB);
        topY.add(cbPanel);

        topPanel.add(topY);

        _thumbBtn = new BlobButton("Generate Thumbnails");
        _thumbBtn.setEnabled(false);
        _thumbBtn.setOnClick(this::generateThumbs);
        topPanel.add(_thumbBtn);

        add(topPanel);

        JPanel breakPanel = new JPanel(new FlowLayout());
        breakPanel.setBackground(Constants.TRANSLUCENT);
        //breakPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        _breakList = new JList<>();
        _breakList.setForeground(Constants.TXT1);
        _breakList.setBackground(Constants.BG3);
        _breakList.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2)
                    return;
                checkBreak();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        _breakList.addKeyListener(this);
        JScrollPane scrollPane = new JScrollPane(_breakList);
        scrollPane.setPreferredSize(new Dimension(512, 512));
        scrollPane.setBorder(null);
        breakPanel.add(scrollPane);
        add(breakPanel);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Constants.BG1);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Find the matching show based on user entry
     */
    private void beginBreakCheck() {
        // Search for nearest matching show
        String showName = _showName.getText().toLowerCase(Locale.US);
        if (showName.isEmpty())
            return;

        File showDir = Config.getFile("SHOW_DIR");
        File[] shows = showDir.listFiles();
        if (shows == null)
            return;

        _showDir = null;
        for (File s : shows) {
            String sName = s.getName().toLowerCase(Locale.US);
            if (sName.equals(showName)) {
                _showDir = s;
                break;
            } else if (sName.contains(showName))
                _showDir = s;
        }
        _thumbBtn.setEnabled(_showDir != null);
        if (_showDir == null) {
            Log.e(TAG, "Failed to find show: " + _showName.getText());
            return;
        }
        _showName.setText(_showDir.getName());

        // Load episodes
        File infoFile = new File(_showDir, "info.js");
        if(!infoFile.exists()) {
            Log.e(TAG, "Failed to find show info file: " + infoFile);
            return;
        }
        try {
            _curInfo = new ShowInfo(infoFile);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse info file:" + infoFile);
            _curInfo = null;
            return;
        }
        if (_curInfo.breaks == null) {
            Log.e(TAG, "Show \"" + _showDir.getName() + "\" has no breaks defined.");
            return;
        }

        // Load segments
        Playlist epList = new Playlist(_showDir);
        Collections.sort(epList);

        // Update list
        List<String> eps = new ArrayList<>();
        for (Segment s : epList) {
            String name = s.name;
            if (_curInfo.breaks.get(name) == null)
                name = "(NO BREAKS) " + name;
            eps.add(name);
        }
        for (String k : _curInfo.breaks.keySet()) {
            if (epList.findByName(k) == null)
                eps.add("(MISSING) " + k);
        }
        _breakList.setListData(eps.toArray(new String[0]));
    }

    /**
     * Loop through each break time in a set
     * @param br Segment break times
     * @param cb Callback to invoke for each time
     */
    private void forEachBreakTime(ShowInfo.Break br, BreakCallback cb) {
        for (Map.Entry<String, Double> e : br.entrySet()) {
            String key = e.getKey();
            double breakTime = br.get(key);
            if(key.equals("intro") && !_introCB.isSelected() ||
                    key.equals("credits") && !_creditsCB.isSelected() ||
                    key.startsWith("episode_") && !_midsCB.isSelected() || key.startsWith("episode_a"))
                continue;
            cb.forBreakTime(key, breakTime);
        }
    }

    /**
     * Preview a break in MPV
     */
    private void checkBreak() {
        String epName = String.valueOf(_breakList.getSelectedValue());
        beginBreakCheck();
        if(epName != null && _curInfo != null) {
            Log.d(TAG, "Playing breaks for " + epName);
            ShowInfo.Break br = _curInfo.breaks.get(epName);
            if (br == null) {
                Log.e(TAG, "Missing episode " + epName);
                return;
            }
            forEachBreakTime(br, (key, breakTime) -> {
                double startTime = Math.max(0, breakTime - 1);
                double endTime = breakTime + 4;
                Log.d(TAG, key + ": " + breakTime);
                try {
                    String[] cmd = new String[] { "mpv", "--osd-fractions",
                            "--start=" + startTime,
                            "--end=" + endTime,
                            "--title=\"" + epName + " -> " + key + ": " + breakTime + "\"",
                            _curInfo.getShowDir() + File.separator + epName + ".mp4"};
                    Process p = Runtime.getRuntime().exec(cmd);
                    StringBuilder sb = new StringBuilder();
                    for (String c : cmd)
                        sb.append(c).append(" ");
                    System.out.println(sb);
                    p.waitFor();
                } catch (Exception exc) {
                    Log.e(TAG, "Error: ", exc);
                }
            });
        }
    }

    /**
     * Generate thumbnails taken at each break for easily previewing which breaks are good or not
     */
    private void generateThumbs() {
        beginBreakCheck();
        if (_curInfo == null || _showDir == null)
            return;

        // Thumbnails are saved under the "Break Thumbnails" directory
        final File thumbDir = new File(_showDir, "Break Thumbnails");
        if (!thumbDir.exists() && !thumbDir.mkdirs())
            return;

        _thumbBtn.setEnabled(false);

        for (Map.Entry<String, ShowInfo.Break> e : _curInfo.breaks.entrySet()) {

            final String name = e.getKey();
            ShowInfo.Break br = e.getValue();

            final File vid = new File(_showDir, name + ".mp4");
            if (!vid.exists())
                continue;

            forEachBreakTime(br, (key, breakTime) -> {

                String imgName = name + " " + key + " [" + breakTime + "].png";
                File imgFile = new File(thumbDir, imgName);
                if (imgFile.exists())
                    return;

                Log.d(TAG, "Generating thumb for " + name + " -> " + key + ": " + breakTime);
                try {
                    ProcessBuilder pb = new ProcessBuilder("ffmpeg",
                            "-ss", String.valueOf(breakTime),
                            "-i", vid.getAbsolutePath(),
                            "-frames:v", "1",
                            imgFile.getAbsolutePath());
                    //pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    //pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    pb.start();
                } catch (Exception exc) {
                    Log.e(TAG, "Error: ", exc);
                }
            });
        }

        // Show the break thumbnails folder
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(thumbDir);
        } catch (Exception ignore) {
        }

        _thumbBtn.setEnabled(true);
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
            if (_breakList.isFocusOwner())
                checkBreak();
            else
                beginBreakCheck();
        }
    }
}
