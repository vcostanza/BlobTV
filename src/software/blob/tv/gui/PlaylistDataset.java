package software.blob.tv.gui;

import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.general.PieDataset;
import software.blob.tv.obj.Playlist;
import software.blob.tv.obj.Segment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Playlist data set for pie chart
 */
public class PlaylistDataset implements PieDataset {

    private static final String TAG = "PlaylistDataset";

    private Map<String, Integer> _results;
    private List<String> _keys;

    public PlaylistDataset(Playlist playlist) {
        Playlist pl = new Playlist(playlist);
        _results = new HashMap<String, Integer>();
        _keys = new ArrayList<String>();

        while (!pl.isEmpty()) {
            String key = "";
            Segment s1 = pl.get(0);
            int occurences = 0;
            for (int j = 0; j < pl.size(); j++) {
                Segment s2 = pl.get(j);
                int same = s1.compareName(s2);
                if (s1.name.equals(s2.name) || same > 3) {
                    if (!s1.name.equals(s2.name) && (same < key.length() || key.isEmpty()))
                        key = s1.name.substring(0, same);
                    occurences++;
                    pl.remove(j--);
                }
            }
            if (key.isEmpty() || occurences <= 5)
                key = "Other";
            _results.put(key, occurences + (_results.containsKey(key) ? _results.get(key) : 0));
            if (!_keys.contains(key))
                _keys.add(key);
        }
    }

    @Override
    public Comparable getKey(int index) {
        return _keys.get(index);
    }

    @Override
    public int getIndex(Comparable key) {
        return _keys.indexOf(String.valueOf(key));
    }

    @Override
    public List getKeys() {
        return _keys;
    }

    @Override
    public Number getValue(Comparable key) {
        return _results.get(String.valueOf(key));
    }

    @Override
    public int getItemCount() {
        return _keys.size();
    }

    @Override
    public Number getValue(int index) {
        return _results.get(_keys.get(index));
    }

    @Override
    public void addChangeListener(DatasetChangeListener listener) {

    }

    @Override
    public void removeChangeListener(DatasetChangeListener listener) {

    }

    @Override
    public DatasetGroup getGroup() {
        return null;
    }

    @Override
    public void setGroup(DatasetGroup group) {

    }
}
