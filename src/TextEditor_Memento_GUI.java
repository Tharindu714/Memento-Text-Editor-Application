import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/* ------------------ Memento (Snapshot) ------------------ */
final class Snapshot {
    private final String content;
    private final long timestamp;

    public Snapshot(String content) {
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public String getPreview() {
        String p = content.replaceAll("\n", " ");
        if (p.length() > 60) p = p.substring(0, 60) + "…";
        return p.isEmpty() ? "(empty)" : p;
    }
    public String getFormattedTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }
}

/* ------------------ Originator ------------------ */
class EditorOriginator {
    private String state = "";

    public void setState(String s) { this.state = s; }
    public String getState() { return state; }
    public Snapshot createSnapshot() { return new Snapshot(state); }
    public void restore(Snapshot m) { this.state = m.getContent(); }
}

/* ------------------ Caretaker (History Manager) ------------------ */
class HistoryManager {
    private final List<Snapshot> history = new ArrayList<>();
    private int currentIndex = -1; // points to current snapshot in history
    private final int maxSize;

    public HistoryManager(int maxSize) { this.maxSize = Math.max(5, maxSize); }

    public synchronized void add(Snapshot m) {
        // if we're not at the end, drop forward history (overwrite branch)
        if (currentIndex < history.size() - 1) {
            history.subList(currentIndex + 1, history.size()).clear();
        }
        history.add(m);
        currentIndex = history.size() - 1;
        // enforce max size
        if (history.size() > maxSize) {
            int removeCount = history.size() - maxSize;
            if (removeCount > 0) {
                history.subList(0, removeCount).clear();
            }
            currentIndex = history.size() - 1;
        }
    }

    public synchronized boolean canUndo() { return currentIndex <= 0; }
    public synchronized boolean canRedo() { return currentIndex >= history.size() - 1; }

    public synchronized Snapshot undo() {
        if (canUndo()) return null;
        currentIndex--;
        return history.get(currentIndex);
    }

    public synchronized Snapshot redo() {
        if (canRedo()) return null;
        currentIndex++;
        return history.get(currentIndex);
    }

    public synchronized Snapshot getCurrent() {
        if (currentIndex < 0 || currentIndex >= history.size()) return null;
        return history.get(currentIndex);
    }

    public synchronized List<Snapshot> listAll() { return new ArrayList<>(history); }
    public synchronized int getCurrentIndex() { return currentIndex; }
    public synchronized void clear() { history.clear(); currentIndex = -1; }
}

/* ------------------ GUI ------------------ */
class EditorFrame extends JFrame {
    private final EditorOriginator originator = new EditorOriginator();
    private final HistoryManager history = new HistoryManager(60); // cap to 60 snapshots by default

    private final JTextArea textArea = new JTextArea();
    private final DefaultListModel<String> historyListModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyListModel);
    private final JLabel statusLabel = new JLabel("Ready");
    private final Timer idleTimer;
    private boolean suppressAutoSave = false;

    public EditorFrame() {
        setTitle("Smart Editor — Memento Undo Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(new EditorHeader(), BorderLayout.NORTH);

        // center split: editor | history
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.7);
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new EmptyBorder(12,12,12,12));
        textArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        left.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // bottom controls under editor
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton saveBtn = new JButton("Save Snapshot");
        JButton undoBtn = new JButton("Undo");
        JButton redoBtn = new JButton("Redo");
        JButton clearHist = new JButton("Clear History");
        controls.add(saveBtn); controls.add(undoBtn); controls.add(redoBtn); controls.add(clearHist);
        controls.add(new JLabel("  Max history:"));
        JSpinner maxSpinner = new JSpinner(new SpinnerNumberModel(60, 5, 500, 5));
        controls.add(maxSpinner);
        controls.add(statusLabel);
        left.add(controls, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(new EmptyBorder(12,12,12,12));
        right.add(new JLabel("History (click to restore):"), BorderLayout.NORTH);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        right.add(new JScrollPane(historyList), BorderLayout.CENTER);

        split.setLeftComponent(left);
        split.setRightComponent(right);
        add(split, BorderLayout.CENTER);

        // idle debounce: save snapshot after 1200 ms of inactivity
        idleTimer = new Timer(1200, e -> {
            if (!suppressAutoSave) saveSnapshotAuto();
        });
        idleTimer.setRepeats(false);

        // Document listener for modifications
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onUserEdit(); }
            @Override public void removeUpdate(DocumentEvent e) { onUserEdit(); }
            @Override public void changedUpdate(DocumentEvent e) { onUserEdit(); }
        });

        // button actions
        saveBtn.addActionListener(e -> saveSnapshotManual());
        undoBtn.addActionListener(e -> performUndo());
        redoBtn.addActionListener(e -> performRedo());
        clearHist.addActionListener(e -> { history.clear(); rebuildHistoryList(); updateStatus("History cleared"); });

        maxSpinner.addChangeListener(e -> {
            int val = (Integer) maxSpinner.getValue();
            // new HistoryManager would be needed to change cap; for demo, show status
            updateStatus("Max history (display note): " + val + " (restart app to change cap) ");
        });

        historyList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = historyList.getSelectedIndex();
                    if (idx >= 0) restoreByIndex(idx);
                }
            }
        });

        // keyboard shortcuts
        InputMap im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textArea.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "redo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "saveSnapshot");
        am.put("undo", new AbstractAction() { public void actionPerformed(ActionEvent e) { performUndo(); } });
        am.put("redo", new AbstractAction() { public void actionPerformed(ActionEvent e) { performRedo(); } });
        am.put("saveSnapshot", new AbstractAction() { public void actionPerformed(ActionEvent e) { saveSnapshotManual(); } });

        // initialize with empty snapshot
        originator.setState("");
        history.add(originator.createSnapshot());
        rebuildHistoryList();
        updateStatus("Ready — snapshot saved");
    }

    private void onUserEdit() {
        // restart idle timer on each edit
        idleTimer.restart();
        updateStatus("Editing...");
    }

    private void saveSnapshotAuto() {
        SwingUtilities.invokeLater(() -> {
            String content = textArea.getText();
            Snapshot s = new Snapshot(content);
            history.add(s);
            originator.setState(content);
            rebuildHistoryList();
            updateStatus("Auto-snapshot saved: " + s.getFormattedTime());
        });
    }

    private void saveSnapshotManual() {
        idleTimer.stop();
        String content = textArea.getText();
        Snapshot s = new Snapshot(content);
        history.add(s);
        originator.setState(content);
        rebuildHistoryList();
        updateStatus("Manual snapshot saved: " + s.getFormattedTime());
    }

    private void performUndo() {
        if (history.canUndo()) { updateStatus("No earlier snapshot to undo"); return; }
        suppressAutoSave = true;
        Snapshot m = history.undo();
        originator.restore(m);
        textArea.setText(m.getContent());
        rebuildHistoryList();
        suppressAutoSave = false;
        updateStatus("Undone to: " + m.getFormattedTime());
    }

    private void performRedo() {
        if (history.canRedo()) { updateStatus("No later snapshot to redo"); return; }
        suppressAutoSave = true;
        Snapshot m = history.redo();
        originator.restore(m);
        textArea.setText(m.getContent());
        rebuildHistoryList();
        suppressAutoSave = false;
        updateStatus("Redone to: " + m.getFormattedTime());
    }

    private void restoreByIndex(int idx) {
        List<Snapshot> list = history.listAll();
        if (idx < 0 || idx >= list.size()) return;
        Snapshot m = list.get(idx);
        suppressAutoSave = true;
        originator.restore(m);
        textArea.setText(m.getContent());
        rebuildHistoryList();
        suppressAutoSave = false;
        updateStatus("Restored snapshot: " + m.getFormattedTime());
    }

    private void rebuildHistoryList() {
        historyListModel.clear();
        List<Snapshot> list = history.listAll();
        int idx = history.getCurrentIndex();
        for (int i = 0; i < list.size(); i++) {
            Snapshot s = list.get(i);
            String label = String.format("%02d %s — %s", i, s.getFormattedTime(), s.getPreview());
            historyListModel.addElement(label);
        }
        if (idx >= 0) historyList.setSelectedIndex(idx);
    }

    private void updateStatus(String s) {
        statusLabel.setText(s);
    }
}

/* ------------------ Attractive Header (Smart-home styled) ------------------ */
class EditorHeader extends JPanel {
    public EditorHeader() {
        setPreferredSize(new Dimension(0, 84));
        setLayout(new BorderLayout());
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, new Color(95, 39, 205), w, h, new Color(238, 90, 123));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // title
        g2.setColor(Color.white);
        g2.setFont(new Font("Poppins", Font.BOLD, 24));
        g2.drawString("Smart Editor — Undo with Memento", 18, 34);

        // subtitle
        g2.setFont(new Font("Inter", Font.PLAIN, 13));
        g2.drawString("Snapshots, multi-step undo/redo, and safe history management", 18, 54);

        // doodles
        g2.setFont(new Font("Serif", Font.BOLD, 34));
        g2.drawString("✎", w - 90, 44);
        g2.drawString("⌘", w - 50, 70);
    }
}

public class TextEditor_Memento_GUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EditorFrame().setVisible(true));
    }
}