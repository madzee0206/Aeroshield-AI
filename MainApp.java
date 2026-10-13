import javax.swing.SwingUtilities;
import ui.MainFrame;

/** Entry point. The GUI is created on the Swing event-dispatch thread (UNIT 6). */
public class MainApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
