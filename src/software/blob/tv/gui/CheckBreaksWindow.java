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

    private JTextField _showName;
    private JList _breakList;
    private BlobCheckBox _introCB, _midsCB, _creditsCB;
    private ShowInfo _curInfo;

    public CheckBreaksWindow() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel showPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        showPanel.setBackground(Constants.TRANSLUCENT);
        //showPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        _showName = new JTextField("", 24);
        _showName.setForeground(Constants.TXT1);
        _showName.setBackground(Constants.BG3);
        _showName.setHorizontalAlignment(JTextField.CENTER);
        _showName.addKeyListener(this);
        showPanel.add(_showName);
        add(showPanel);

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
        add(cbPanel);

        JPanel breakPanel = new JPanel(new FlowLayout());
        breakPanel.setBackground(Constants.TRANSLUCENT);
        //breakPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        _breakList = new JList<>();
        _breakList.setForeground(Constants.TXT1);
        _breakList.setBackground(Constants.BG3);
        _breakList.addMouseListener(_breakListClick);
        _breakList.addKeyListener(this);
        JScrollPane scrollPane = new JScrollPane(_breakList);
        scrollPane.setPreferredSize(new Dimension (480, 480));
        breakPanel.add(scrollPane);
        add(breakPanel);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Constants.BG1);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void beginBreakCheck() {
        // Search for nearest matching show
        String showName = _showName.getText().toLowerCase(Locale.US);
        if (showName.isEmpty())
            return;
        File showDir = Config.getFile("SHOW_DIR");
        File[] shows = showDir.listFiles();
        if (shows == null)
            return;
        File show = null;
        for (File s : shows) {
            String sName = s.getName().toLowerCase(Locale.US);
            if (sName.equals(showName)) {
                show = s;
                break;
            } else if (sName.contains(showName))
                show = s;
        }
        if (show == null) {
            Log.e(TAG, "Failed to find show: " + _showName.getText());
            return;
        }
        _showName.setText(show.getName());

        // Load episodes
        File infoFile = new File(show, "info.js");
        if(!infoFile.exists()) {
            Log.e(TAG, "Failed to find show info file: " + infoFile);
            return;
        }
        try {
            _curInfo = new ShowInfo(infoFile);
        } catch (Exception e) {
            _curInfo = null;
            return;
        }
        // Load segments
        Playlist epList = new Playlist(show);
        Collections.sort(epList, new Comparator<Segment>() {
            @Override
            public int compare(Segment o1, Segment o2) {
                if (o1.format != Segment.Format.SHOW || o2.format != Segment.Format.SHOW)
                    return o1.name.compareTo(o2.name);
                int tComp = o1.epType.compareTo(o2.epType);
                int sComp = Integer.compare(o1.season, o2.season);
                int eComp = Integer.compare(o1.episode, o2.episode);
                int pComp = Character.compare(o1.part, o2.part);
                if (tComp == 0) {
                    if (sComp == 0) {
                        if (eComp == 0)
                            return pComp;
                        return eComp;
                    }
                    return sComp;
                }
                return tComp;
            }
        });
        // Update list
        List<String> eps = new ArrayList<String>();
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
        _breakList.setListData(eps.toArray(new String[eps.size()]));
    }

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
            for(String key : br.keySet()) {
                if(key.equals("intro") && !_introCB.isSelected() ||
                        key.equals("credits") && !_creditsCB.isSelected() ||
                        key.startsWith("episode_") && !_midsCB.isSelected() || key.startsWith("episode_a"))
                    continue;
                double breakTime = br.get(key);
                Log.d(TAG, key + ": " + breakTime);
                try {
                    String[] cmd = new String[] { "mpv", "--osd-fractions", "--start",
                            String.valueOf(breakTime-1), "--end", String.valueOf(breakTime+4),
                            "--title", epName + " -> " + key + ": " + breakTime,
                            _curInfo.getShowDir() + File.separator + epName + ".mp4"};
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                } catch (Exception exc) {
                    Log.e(TAG, "Error: ", exc);
                }
            }
        }
    }

    private final MouseListener _breakListClick = new MouseListener() {
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2)
                return;
            checkBreak();
        }
        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
    };

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
