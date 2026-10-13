package ui;

import components.GlassPanel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import io.ScenarioStore;
import simulation.Scenario;
import simulation.SimulationEngine;
import theme.*;

/** Settings: clusters, reserve, drone batteries, strategy, seed, save / load. */
public class SettingsScreen extends GlassPanel {
    private final Scenario sc;
    private final SimulationEngine eng;
    private final Runnable afterChange;
    private final JSpinner kSpin = new JSpinner(new SpinnerNumberModel(5, 1, 8, 1));
    private final JSpinner reserveSpin = new JSpinner(new SpinnerNumberModel(20, 0, 60, 5));
    private final JSpinner seedSpin = new JSpinner(new SpinnerNumberModel(2, 1, 9999, 1));
    private final JTextField batteries = new JTextField("100,95,90,70", 18);
    private final JComboBox<String> strategy = new JComboBox<>(new String[]{"Greedy cost-based (proposed)", "Round-robin (baseline)"});
    private final JLabel status = new JLabel(" ");

    public SettingsScreen(Scenario sc, SimulationEngine eng, Runnable afterChange) {
        super(new BorderLayout(), 18);
        this.sc = sc;
        this.eng = eng;
        this.afterChange = afterChange;
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("SETTINGS");
        title.setFont(Fonts.TITLE);
        title.setForeground(Palette.TEXT);
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int y = 0;
        addRow(form, y++, "Number of clusters (K)", kSpin);
        addRow(form, y++, "Battery reserve for return (%)", reserveSpin);
        addRow(form, y++, "Drone batteries % (comma separated, 1-6 drones)", batteries);
        addRow(form, y++, "Assignment strategy to simulate", strategy);
        addRow(form, y++, "Scenario seed", seedSpin);
        Theme.styleInput(batteries);
        batteries.setCaretColor(Palette.TEAL);
        batteries.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER), BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton apply = Theme.button("APPLY", Palette.TEAL);
        JButton gen = Theme.button("NEW SCENARIO", new Color(0x7AA2F7));
        gen.setPreferredSize(new Dimension(130, 34));
        JButton save = Theme.button("SAVE", Palette.AMBER);
        JButton load = Theme.button("LOAD", Palette.AMBER);
        buttons.add(apply); buttons.add(gen); buttons.add(save); buttons.add(load);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2; gc.anchor = GridBagConstraints.WEST; gc.insets = new Insets(18, 0, 6, 0);
        form.add(buttons, gc);
        gc.gridy = y; gc.insets = new Insets(4, 0, 0, 0);
        status.setFont(Fonts.BODY);
        status.setForeground(Palette.AMBER);
        form.add(status, gc);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrap.setOpaque(false);
        wrap.add(form);
        add(wrap, BorderLayout.CENTER);

        apply.addActionListener(e -> apply());
        gen.addActionListener(e -> {
            eng.reset();
            sc.newScenario(((Number) seedSpin.getValue()).longValue());
            sc.replanAsync(() -> SwingUtilities.invokeLater(afterChange));
            status.setText("New synthetic scenario generated.");
        });
        save.addActionListener(e -> saveFile());
        load.addActionListener(e -> loadFile());
    }

    private void addRow(JPanel form, int y, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y; c.insets = new Insets(8, 0, 8, 18); c.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(label);
        l.setFont(Fonts.BODY);
        l.setForeground(Palette.MUTED);
        c.gridx = 0;
        form.add(l, c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JSpinner) {
            field.setPreferredSize(new Dimension(100, 28));
            JTextField tf = ((JSpinner.DefaultEditor) ((JSpinner) field).getEditor()).getTextField();
            Theme.styleInput(tf);
            tf.setCaretColor(Palette.TEAL);
            field.setBorder(BorderFactory.createLineBorder(Palette.BORDER));
        } else if (field instanceof JComboBox) {
            Theme.styleInput(field);
        }
        form.add(field, c);
    }

    private void apply() {
        try {
            String[] parts = batteries.getText().split(",");
            double[] b = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                b[i] = Double.parseDouble(parts[i].trim());        // may throw NumberFormatException (unchecked)
            }
            eng.reset();
            sc.configure(((Number) kSpin.getValue()).intValue(), ((Number) reserveSpin.getValue()).doubleValue(),
                    b, strategy.getSelectedIndex() == 1);
            sc.replanAsync(() -> SwingUtilities.invokeLater(afterChange));
            status.setForeground(Palette.TEAL);
            status.setText("Settings applied - re-planning in the background...");
        } catch (NumberFormatException ex) {
            status.setForeground(Palette.DANGER);
            status.setText("Batteries must be numbers such as 100,95,80");
        } catch (IllegalArgumentException ex) {
            status.setForeground(Palette.DANGER);
            status.setText(ex.getMessage());
        }
    }

    private void saveFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ScenarioStore.save(sc, fc.getSelectedFile());
                status.setForeground(Palette.TEAL);
                status.setText("Saved to " + fc.getSelectedFile().getName());
            } catch (IOException ex) {
                status.setForeground(Palette.DANGER);
                status.setText("Could not save: " + ex.getMessage());
            }
        }
    }

    private void loadFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                eng.reset();
                ScenarioStore.load(sc, f);
                sc.replanAsync(() -> SwingUtilities.invokeLater(afterChange));
                status.setForeground(Palette.TEAL);
                status.setText("Loaded " + f.getName());
            } catch (IOException ex) {
                status.setForeground(Palette.DANGER);
                status.setText("Could not load: " + ex.getMessage());
            }
        }
    }
}
