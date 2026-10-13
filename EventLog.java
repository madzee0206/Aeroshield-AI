package simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe event feed. The planning thread and the Swing thread both touch it,
 * so every method is synchronized (UNIT 6: multithreading).
 */
public class EventLog {
    private final List<String> lines = new ArrayList<>();

    public synchronized void add(double simSeconds, String message) {
        int s = (int) simSeconds;
        lines.add(String.format("[%02d:%02d] %s", s / 60, s % 60, message));
    }

    public synchronized int size() { return lines.size(); }

    public synchronized void clear() { lines.clear(); }

    public synchronized List<String> last(int max) {
        int from = Math.max(0, lines.size() - max);
        return new ArrayList<>(lines.subList(from, lines.size()));
    }
}
