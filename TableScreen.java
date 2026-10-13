package ui;

import components.GlassPanel;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import theme.*;

/** Generic glass table used by the Fleet and Reports screens. The data comes from a Supplier (lambda). */
public class TableScreen extends GlassPanel {
    private final String[] columns;
    private final Supplier<Object[][]> source;
    private final DefaultTableModel model;
    private Object[][] last;

    public TableScreen(String title, String[] columns, Supplier<Object[][]> source) {
        super(new BorderLayout(10, 10), 18);
        this.columns = columns;
        this.source = source;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel(title);
        heading.setFont(Fonts.TITLE);
        heading.setForeground(Palette.TEXT);
        add(heading, BorderLayout.NORTH);

        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.setShowGrid(false);
        table.setRowHeight(30);
        table.setFont(Fonts.BODY);
        table.setForeground(Palette.TEXT);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Theme.alpha(Palette.TEAL, 50));
        table.setSelectionForeground(Palette.TEXT);

        DefaultTableCellRenderer cells = new DefaultTableCellRenderer();
        cells.setOpaque(false);
        cells.setForeground(Palette.TEXT);
        table.setDefaultRenderer(Object.class, cells);

        DefaultTableCellRenderer head = new DefaultTableCellRenderer();
        head.setOpaque(true);
        head.setBackground(new Color(0x0B1320));
        head.setForeground(Palette.TEAL);
        head.setFont(Fonts.LABEL);
        table.getTableHeader().setDefaultRenderer(head);

        JScrollPane sp = new JScrollPane(table);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        add(sp, BorderLayout.CENTER);
        refresh();
    }

    public void refresh() {
        Object[][] data = source.get();
        if (Arrays.deepEquals(data, last)) {
            return;                              // nothing changed: keep scroll position / selection
        }
        last = data;
        model.setDataVector(data, columns);
    }
}
