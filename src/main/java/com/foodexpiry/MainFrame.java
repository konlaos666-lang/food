package com.foodexpiry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class MainFrame extends JFrame {

    private final FoodDAO dao = new FoodDAO();

    // ---------- Theme colors ----------
    private static final Color PRIMARY       = new Color(99, 102, 241);
    private static final Color PRIMARY_DARK  = new Color(67, 56, 202);
    private static final Color VIOLET        = new Color(139, 92, 246);
    private static final Color SIDEBAR_TOP   = new Color(30, 27, 75);
    private static final Color SIDEBAR_BOT   = new Color(55, 48, 140);
    private static final Color SOFT          = new Color(238, 240, 255);
    private static final Color SOFT_HOVER    = new Color(224, 231, 255);
    private static final Color BG            = new Color(244, 245, 251);
    private static final Color TEXT          = new Color(30, 32, 50);
    private static final Color MUTED         = new Color(107, 114, 128);
    private static final Color BORDER        = new Color(228, 231, 242);
    private static final Color ROW_ALT       = new Color(249, 250, 254);
    private static final Color SELECT_BG     = new Color(214, 222, 255);

    private static final Color OK            = new Color(16, 185, 129);
    private static final Color WARN          = new Color(245, 158, 11);
    private static final Color BAD           = new Color(244, 63, 94);
    private static final Color GRAY_BTN      = new Color(120, 126, 145);

    private static final String FONT = "Segoe UI";

    // ---------- Status ----------
    private static final String S_NORMAL  = "Normal";
    private static final String S_SOON    = "Expiring Soon";
    private static final String S_TODAY   = "Expires Today";
    private static final String S_EXPIRED = "Expired";

    // ---------- Model columns ----------
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_QTY = 2;
    private static final int COL_DATE = 3;
    private static final int COL_DAYS = 4;
    private static final int COL_STATUS = 5;

    //Pages
    private static final String PAGE_DASHBOARD = "dashboard";
    private static final String PAGE_ADD = "add";

    private CardLayout cardLayout;
    private FadePanel pages;
    private RoundedButton dashboardMenu;
    private RoundedButton addMenu;
    private Timer fadeTimer;
    private ToastPane toast;

    //  Animation
    private float phase = 0f;// เวลาของแอนิเมชั่นวนซ้ำ
    private final List<JComponent> animated = new ArrayList<>();
    private int hoverRow = -1;

    // ---------- Dashboard ----------
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel totalValue;
    private JLabel soonValue;
    private JLabel expiredValue;
    private JLabel messageLabel;
    private RoundedButton editButton;
    private RoundedButton deleteButton;

    // Add / Edit page
    private JLabel addPageTitle;
    private JLabel addPageSubtitle;
    private JLabel formTitle;
    private JTextField nameField;
    private JTextField quantityField;
    private JTextField dateField;
    private RoundedButton saveButton;
    private RoundedButton clearButton;
    private RoundedButton cancelButton;
    private Integer editingId = null;   // null = โหมดเพิ่ม, มีค่า = โหมดแก้ไข

    public MainFrame() {

        setTitle("Food Expiry Tracker");
        setSize(1280, 820);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        toast = new ToastPane();
        setGlassPane(toast);
        toast.setVisible(true);

        add(createSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        pages = new FadePanel(cardLayout);
        pages.setBackground(BG);
        pages.add(createDashboardPage(), PAGE_DASHBOARD);
        pages.add(createAddPage(), PAGE_ADD);
        add(pages, BorderLayout.CENTER);

        refreshTable();
        updateButtonStates();
        resetForm();
        showPage(PAGE_DASHBOARD);

        // ตัวขับแอนิเมชันที่วนซ้ำ(แบนเนอร์, ไอคอน, จุดกะพริบ)
        Timer ticker = new Timer(40, e -> {
            phase += 0.04f;
            for (JComponent c : animated) {
                if (c.isShowing()) {
                    c.repaint();
                }
            }
        });
        ticker.start();
    }

    // NAVIGATION
    private void showPage(String name) {

        cardLayout.show(pages, name);
        dashboardMenu.setActive(PAGE_DASHBOARD.equals(name));
        addMenu.setActive(PAGE_ADD.equals(name));

        // เปลี่ยนหน้าแบบ Fade-in
        if (fadeTimer != null) {
            fadeTimer.stop();
        }
        pages.overlay = 1f;
        fadeTimer = Anim.run(380, p -> {
            pages.overlay = 1f - p;
            pages.repaint();
        });
    }

    // SIDEBAR
    private JPanel createSidebar() {

        JPanel sidebar = new GradientPanel(SIDEBAR_TOP, SIDEBAR_BOT);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(30, 18, 22, 18));

        JLabel logo = new JLabel("FOOD TRACKER");
        logo.setFont(font(Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("EXPIRY MANAGEMENT");
        subtitle.setFont(font(Font.PLAIN, 11));
        subtitle.setForeground(new Color(196, 202, 255));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(42));

        JLabel menu = new JLabel("MENU");
        menu.setForeground(new Color(165, 172, 235));
        menu.setFont(font(Font.BOLD, 12));
        menu.setBorder(new EmptyBorder(0, 8, 0, 0));
        menu.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(menu);
        sidebar.add(Box.createVerticalStrut(12));

        dashboardMenu = createMenuButton("Dashboard");
        addMenu = createMenuButton("Add Food");

        dashboardMenu.addActionListener(e -> {
            resetForm();
            showPage(PAGE_DASHBOARD);
        });

        addMenu.addActionListener(e -> {
            resetForm();
            showPage(PAGE_ADD);
            nameField.requestFocusInWindow();
        });

        sidebar.add(dashboardMenu);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(addMenu);
        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("Food Expiry Tracker v3.0");
        footer.setForeground(new Color(196, 202, 255));
        footer.setFont(font(Font.PLAIN, 12));
        footer.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(footer);

        return sidebar;
    }

    private RoundedButton createMenuButton(String text) {

        RoundedButton b = new RoundedButton(text,
            new Color(255, 255, 255, 0), new Color(255, 255, 255, 30));
        b.activeBg = new Color(255, 255, 255, 52);
        b.indicator = true;
        b.arc = 14;
        b.setFont(font(Font.BOLD, 15));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(13, 20, 13, 10));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    // DASHBOARD PAGE
    private JPanel createDashboardPage() {

        JPanel page = new JPanel(new BorderLayout(0, 20));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 34, 22, 34));

        JPanel north = new JPanel(new BorderLayout(0, 18));
        north.setOpaque(false);

        BannerPanel banner = new BannerPanel("Dashboard",
            greeting() + "! Here is the overview of your food inventory.",
            LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        animated.add(banner);

        north.add(banner, BorderLayout.NORTH);
        north.add(createDashboardCards(), BorderLayout.CENTER);

        page.add(north, BorderLayout.NORTH);
        page.add(createTablePanel(), BorderLayout.CENTER);

        return page;
    }

    private String greeting() {
        int h = LocalTime.now().getHour();
        if (h < 12) return "Good morning";
        if (h < 17) return "Good afternoon";
        return "Good evening";
    }

    private JPanel createDashboardCards() {

        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(700, 128));

        totalValue = new JLabel("0");
        soonValue = new JLabel("0");
        expiredValue = new JLabel("0");

        cards.add(createCard("TOTAL FOODS", totalValue,
            "All registered items", PRIMARY, "#", 0));

        cards.add(createCard("EXPIRING SOON", soonValue,
            "Today or within 3 days", WARN, "!", 2));

        cards.add(createCard("EXPIRED", expiredValue,
            "Past expiry date", BAD, "x", 3));

        return cards;
    }

    private JPanel createCard(String title, JLabel value, String description,
                              Color accent, String glyph, int filterIndex) {

        RoundedPanel card = new RoundedPanel(Color.WHITE, 20);
        card.accent = accent;
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(14, 22, 20, 14));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Click to filter the table");

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(font(Font.BOLD, 12));
        titleLabel.setForeground(MUTED);

        value.setFont(font(Font.BOLD, 36));
        value.setForeground(accent);

        JLabel desc = new JLabel(description);
        desc.setFont(font(Font.PLAIN, 12));
        desc.setForeground(MUTED);

        text.add(titleLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(value);
        text.add(Box.createVerticalStrut(2));
        text.add(desc);

        IconBadge badge = new IconBadge(accent, glyph);
        animated.add(badge);

        card.add(text, BorderLayout.CENTER);
        card.add(badge, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.setHover(true); }
            @Override public void mouseExited(MouseEvent e) { card.setHover(false); }
            @Override public void mouseClicked(MouseEvent e) {
                statusFilter.setSelectedIndex(filterIndex);
            }
        });

        return card;
    }

    // ---------- table ----------
    private JPanel createTablePanel() {

        RoundedPanel panel = new RoundedPanel(Color.WHITE, 20);
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(20, 24, 22, 24));

        // top bar
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        JLabel title = new JLabel("Food Inventory");
        title.setFont(font(Font.BOLD, 20));
        title.setForeground(TEXT);

        searchField = new HintField("Search food name...");
        styleField(searchField);
        searchField.setFont(font(Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(250, 40));

        statusFilter = new JComboBox<>(new String[]{
            "All status", "Normal", "Expiring Soon", "Expired"
        });
        statusFilter.setFont(font(Font.PLAIN, 14));
        statusFilter.setPreferredSize(new Dimension(150, 40));
        statusFilter.setBackground(Color.WHITE);
        statusFilter.addActionListener(e -> applyFilter());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(searchField);
        right.add(statusFilter);

        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // table
        model = new DefaultTableModel(
            new Object[]{"ID", "Food Name", "Quantity",
                         "Expiry Date", "Days Left", "Status"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case COL_ID:
                    case COL_QTY:
                    case COL_DAYS:
                        return Integer.class;
                    case COL_DATE:
                        return LocalDate.class;
                    default:
                        return String.class;
                }
            }
        };

        table = new JTable(model) {
            // ข้อความเมื่อตารางว่าง
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(150, 156, 178));
                    g2.setFont(font(Font.PLAIN, 15));
                    String msg = model.getRowCount() == 0
                        ? "No food items yet. Add one from the \"Add Food\" page."
                        : "No items match your search or filter.";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2,
                        Math.max(60, getVisibleRect().height / 3));
                    g2.dispose();
                }
            }
        };
        table.setFont(font(Font.PLAIN, 15));
        table.setRowHeight(46);
        table.setSelectionBackground(SELECT_BG);
        table.setSelectionForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(236, 239, 248));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        animated.add(table);   // ให้จุดกะพริบในป้ายสถานะขยับ

        JTableHeader header = table.getTableHeader();
        header.setFont(font(Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 44));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setSortKeys(List.of(
            new RowSorter.SortKey(COL_DAYS, SortOrder.ASCENDING)));

        CellRenderer cell = new CellRenderer();
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultRenderer(Integer.class, cell);
        table.setDefaultRenderer(String.class, cell);
        table.setDefaultRenderer(LocalDate.class, cell);

        // ซ่อนคอลัมน์ ID
        table.removeColumn(table.getColumnModel().getColumn(COL_ID));

        table.getColumn("Status").setCellRenderer(new StatusRenderer());
        table.getColumn("Food Name").setPreferredWidth(320);
        table.getColumn("Quantity").setPreferredWidth(90);
        table.getColumn("Expiry Date").setPreferredWidth(140);
        table.getColumn("Days Left").setPreferredWidth(100);
        table.getColumn("Status").setPreferredWidth(170);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        panel.add(scroll, BorderLayout.CENTER);

        // bottom bar: count + Edit / Delete
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);

        messageLabel = new JLabel("Ready");
        messageLabel.setFont(font(Font.PLAIN, 13));
        messageLabel.setForeground(MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        editButton = new RoundedButton("Edit", PRIMARY);
        deleteButton = new RoundedButton("Delete", BAD);
        editButton.addActionListener(e -> startEdit());
        deleteButton.addActionListener(e -> deleteFood());
        actions.add(editButton);
        actions.add(deleteButton);

        bottom.add(messageLabel, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        // listeners
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    startEdit();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                table.repaint();
            }
        });

        // ไฮไลต์แถวตรงเมาส์ชี้
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoverRow) {
                    hoverRow = r;
                    table.repaint();
                }
            }
        });

        return panel;
    }

    //กรองตามคำค้นหา + สถานะ พร้อมกัน 
    private void applyFilter() {

        final String keyword = searchField.getText().trim().toLowerCase();
        final int mode = statusFilter.getSelectedIndex();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {

                if (!keyword.isEmpty()
                        && !entry.getStringValue(COL_NAME).toLowerCase().contains(keyword)) {
                    return false;
                }

                String status = entry.getStringValue(COL_STATUS);

                switch (mode) {
                    case 1:  return S_NORMAL.equals(status);
                    case 2:  return S_SOON.equals(status) || S_TODAY.equals(status);
                    case 3:  return S_EXPIRED.equals(status);
                    default: return true;
                }
            }
        });

        updateMessage();
    }

    // ADD / EDIT PAGE
    private JPanel createAddPage() {

        JPanel page = new JPanel(new BorderLayout(0, 22));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 34, 22, 34));

        BannerPanel banner = new BannerPanel("Add Food",
            "Register a new food item and its expiry date", "");
        animated.add(banner);
        addPageTitle = banner.titleLabel;
        addPageSubtitle = banner.subtitleLabel;
        page.add(banner, BorderLayout.NORTH);

        // form card
        RoundedPanel card = new RoundedPanel(Color.WHITE, 20);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 30, 32, 30));

        formTitle = new JLabel("Food Information");
        formTitle.setFont(font(Font.BOLD, 20));
        formTitle.setForeground(TEXT);
        formTitle.setAlignmentX(LEFT_ALIGNMENT);

        nameField = new HintField("e.g. Milk, Eggs, Chicken breast");
        quantityField = new HintField("e.g. 2");
        dateField = new HintField("YYYY-MM-DD  e.g. " + LocalDate.now().plusDays(7));

        card.add(formTitle);
        card.add(Box.createVerticalStrut(20));
        card.add(wrapField("Food Name", nameField, null));
        card.add(Box.createVerticalStrut(16));
        card.add(wrapField("Quantity", quantityField, null));
        card.add(Box.createVerticalStrut(16));
        card.add(wrapField("Expiry Date", dateField, createQuickDatePanel()));
        card.add(Box.createVerticalStrut(26));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);

        saveButton = new RoundedButton("Add Food", PRIMARY);
        clearButton = new RoundedButton("Clear", GRAY_BTN);
        cancelButton = new RoundedButton("Cancel", GRAY_BTN);

        saveButton.addActionListener(e -> saveFood());
        clearButton.addActionListener(e -> {
            resetForm();
            nameField.requestFocusInWindow();
        });
        cancelButton.addActionListener(e -> {
            resetForm();
            showPage(PAGE_DASHBOARD);
        });

        actions.add(saveButton);
        actions.add(clearButton);
        actions.add(cancelButton);
        card.add(actions);

        // Enter = บันทึก
        java.awt.event.ActionListener enter = e -> saveFood();
        nameField.addActionListener(enter);
        quantityField.addActionListener(enter);
        dateField.addActionListener(enter);

        card.setPreferredSize(new Dimension(720, card.getPreferredSize().height));

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 1;
        gc.weighty = 1;
        holder.add(card, gc);

        page.add(holder, BorderLayout.CENTER);

        return page;
    }

    private JPanel createQuickDatePanel() {

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel label = new JLabel("Quick pick:");
        label.setFont(font(Font.PLAIN, 12));
        label.setForeground(MUTED);
        p.add(label);

        p.add(createChip("Today", 0));
        p.add(createChip("+3 days", 3));
        p.add(createChip("+7 days", 7));
        p.add(createChip("+30 days", 30));

        return p;
    }

    private JButton createChip(String text, int days) {

        RoundedButton chip = new RoundedButton(text, SOFT, SOFT_HOVER);
        chip.setForeground(PRIMARY_DARK);
        chip.setFont(font(Font.BOLD, 12));
        chip.setBorder(new EmptyBorder(4, 10, 4, 10));
        chip.arc = 10;
        chip.addActionListener(e ->
            dateField.setText(LocalDate.now().plusDays(days).toString()));
        return chip;
    }

    private JPanel wrapField(String label, JTextField field, JComponent extra) {

        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel(label);
        title.setFont(font(Font.BOLD, 13));
        title.setForeground(MUTED);

        styleField(field);
        field.setPreferredSize(new Dimension(100, 46));

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        if (extra != null) {
            wrapper.add(extra, BorderLayout.SOUTH);
        }

        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            wrapper.getPreferredSize().height));

        return wrapper;
    }

    private void styleField(JTextField field) {

        field.setFont(font(Font.PLAIN, 16));
        field.setForeground(TEXT);
        field.setCaretColor(PRIMARY);
        field.setBorder(fieldBorder(BORDER));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(fieldBorder(PRIMARY));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(fieldBorder(BORDER));
            }
        });
    }

    private javax.swing.border.Border fieldBorder(Color c) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c, 1),
            new EmptyBorder(5, 12, 5, 12));
    }

    // RENDERERS
    private class HeaderRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focused, int row, int column) {

            super.getTableCellRendererComponent(t, value, false, false, row, column);
            setBackground(SOFT);
            setForeground(PRIMARY_DARK);
            setFont(font(Font.BOLD, 14));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY),
                new EmptyBorder(0, 12, 0, 12)));

            String name = t.getColumnModel().getColumn(column)
                .getHeaderValue().toString();
            boolean center = name.equals("Quantity") || name.equals("Days Left")
                || name.equals("Status");
            setHorizontalAlignment(center ? SwingConstants.CENTER : SwingConstants.LEFT);
            return this;
        }
    }

    private Color rowColor(int row) {
        if (row == hoverRow) {
            return SOFT;
        }
        return row % 2 == 0 ? Color.WHITE : ROW_ALT;
    }

    private class CellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focused, int row, int column) {

            super.getTableCellRendererComponent(t, value, selected, false, row, column);

            setBorder(new EmptyBorder(0, 12, 0, 12));
            setFont(font(Font.PLAIN, 15));

            boolean number = (t.getColumnClass(column) == Integer.class);
            setHorizontalAlignment(number ? SwingConstants.CENTER : SwingConstants.LEFT);

            if (!selected) {
                setBackground(rowColor(row));
                setForeground(TEXT);

                if (t.getColumnName(column).equals("Days Left")
                        && value instanceof Integer) {
                    int d = (Integer) value;
                    setFont(font(Font.BOLD, 15));
                    if (d < 0) {
                        setForeground(new Color(190, 18, 60));
                    } else if (d <= 3) {
                        setForeground(new Color(180, 83, 9));
                    }
                }
            }
            return this;
        }
    }

    // ป้ายสถานะแบบ Badge มีจุดกะพริบ (เฉพาะ Expired / ใกล้หมดอายุ) 
    private class StatusRenderer extends DefaultTableCellRenderer {

        private Color badgeBg = null;
        private Color dotColor = null;
        private boolean pulse = false;
        private Color rowBg = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean selected, boolean focused, int row, int column) {

            super.getTableCellRendererComponent(t, value, selected, false, row, column);

            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(font(Font.BOLD, 12));
            setBorder(new EmptyBorder(0, 14, 0, 0));

            rowBg = selected ? t.getSelectionBackground() : rowColor(row);

            String status = value == null ? "" : value.toString();

            switch (status) {
                case S_NORMAL:
                    setForeground(new Color(4, 120, 87));
                    badgeBg = new Color(209, 250, 229);
                    dotColor = OK;
                    pulse = false;
                    break;
                case S_SOON:
                case S_TODAY:
                    setForeground(new Color(180, 83, 9));
                    badgeBg = new Color(254, 243, 199);
                    dotColor = WARN;
                    pulse = true;
                    break;
                case S_EXPIRED:
                    setForeground(new Color(190, 18, 60));
                    badgeBg = new Color(255, 228, 230);
                    dotColor = BAD;
                    pulse = true;
                    break;
                default:
                    setForeground(TEXT);
                    badgeBg = null;
                    dotColor = null;
                    pulse = false;
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(rowBg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (badgeBg != null) {
                int h = getHeight() - 16;
                g2.setColor(badgeBg);
                g2.fillRoundRect(12, 8, getWidth() - 24, h, h, h);

                // จุดสถานะ + วงกะพริบ
                int cx = 30;
                int cy = getHeight() / 2;
                if (pulse) {
                    float k = (float) (0.5 + 0.5 * Math.sin(phase * 3.2));
                    int r = (int) (4 + 4 * k);
                    g2.setColor(withAlpha(dotColor, (int) (90 * (1 - k))));
                    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                }
                g2.setColor(dotColor);
                g2.fillOval(cx - 3, cy - 3, 7, 7);
            }
            g2.dispose();

            super.paintComponent(g);
        }
    }

    // ACTIONS
    // บันทึก: เพิ่มใหม่ หรือแก้ไข ขึ้นอยู่กับโหมด
    private void saveFood() {

        try {
            Food input = readForm();

            if (editingId == null) {

                dao.addFood(input);
                refreshTable();
                resetForm();
                toast("Added \"" + input.getName() + "\" successfully", true);
                nameField.requestFocusInWindow();

            } else {

                Food food = new Food(editingId, input.getName(),
                    input.getQuantity(), input.getExpiryDate());

                dao.updateFood(food);
                refreshTable();
                resetForm();
                showPage(PAGE_DASHBOARD);
                toast("Updated \"" + food.getName() + "\" successfully", true);
            }

        } catch (Exception ex) {
            showError(editingId == null ? "Cannot add food" : "Cannot update food", ex);
        }
    }

    // ไปหน้า Add Food ในโหมดแก้ไข โดยโหลดข้อมูลจากแถวที่เลือก
    private void startEdit() {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {
            toast("Please select a food item first.", false);
            return;
        }

        int row = table.convertRowIndexToModel(viewRow);

        editingId = Integer.parseInt(model.getValueAt(row, COL_ID).toString());

        nameField.setText(model.getValueAt(row, COL_NAME).toString());
        quantityField.setText(model.getValueAt(row, COL_QTY).toString());
        dateField.setText(model.getValueAt(row, COL_DATE).toString());

        applyFormMode();
        showPage(PAGE_ADD);
        nameField.requestFocusInWindow();
    }

    private void deleteFood() {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {
            toast("Please select a food item first.", false);
            return;
        }

        int row = table.convertRowIndexToModel(viewRow);
        String name = model.getValueAt(row, COL_NAME).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete \"" + name + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int id = Integer.parseInt(model.getValueAt(row, COL_ID).toString());

            dao.deleteFood(id);
            refreshTable();
            toast("Deleted \"" + name + "\"", true);

        } catch (Exception ex) {
            showError("Cannot delete food", ex);
        }
    }

    private Food readForm() {

        String name = nameField.getText().trim();
        String quantityText = quantityField.getText().trim();
        String dateText = dateField.getText().trim();

        if (name.isEmpty() || quantityText.isEmpty() || dateText.isEmpty()) {
            throw new IllegalArgumentException("Please fill in all fields.");
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Quantity must be a whole number (e.g. 2).");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "Quantity must be greater than zero.");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Please enter the date as YYYY-MM-DD (e.g. "
                + LocalDate.now().plusDays(7) + ").");
        }

        return new Food(name, quantity, date);
    }

    // REFRESH / FORM STATE
    private void refreshTable() {

        try {
            List<Food> foods = dao.getAllFoods();

            model.setRowCount(0);

            int total = 0;
            int soon = 0;
            int expired = 0;
            LocalDate today = LocalDate.now();

            for (Food food : foods) {

                total++;

                int days = (int) ChronoUnit.DAYS.between(today, food.getExpiryDate());

                String status;

                if (days < 0) {
                    status = S_EXPIRED;
                    expired++;
                } else if (days == 0) {
                    status = S_TODAY;
                    soon++;
                } else if (days <= 3) {
                    status = S_SOON;
                    soon++;
                } else {
                    status = S_NORMAL;
                }

                model.addRow(new Object[]{
                    food.getId(),
                    food.getName(),
                    food.getQuantity(),
                    food.getExpiryDate(),
                    days,
                    status
                });
            }

            animateCount(totalValue, total);
            animateCount(soonValue, soon);
            animateCount(expiredValue, expired);

            updateMessage();

        } catch (SQLException ex) {
            showError("Cannot load food data", ex);
        }
    }

    // ตัวเลขนับขึ้น/ลงไปยังค่าใหม่แบบนุ่มนวล
    private void animateCount(JLabel label, int target) {

        Object cur = label.getClientProperty("val");
        final int from = cur instanceof Integer ? (Integer) cur : 0;

        Object old = label.getClientProperty("timer");
        if (old instanceof Timer) {
            ((Timer) old).stop();
        }

        label.putClientProperty("val", target);

        if (from == target) {
            label.setText(String.valueOf(target));
            return;
        }

        Timer t = Anim.run(750, p ->
            label.setText(String.valueOf(Math.round(from + (target - from) * p))));
        label.putClientProperty("timer", t);
    }

    // ล้างฟอร์มและกลับเป็นโหมดเพิ่ม
    private void resetForm() {

        editingId = null;

        nameField.setText("");
        quantityField.setText("");
        dateField.setText("");

        if (table != null) {
            table.clearSelection();
        }
        applyFormMode();
    }

    //ปรับหัวข้อ/ปุ่มของหน้า Add ตามโหมด Add หรือ Edit 
    private void applyFormMode() {

        boolean editing = editingId != null;

        addPageTitle.setText(editing ? "Edit Food" : "Add Food");
        addPageSubtitle.setText(editing
            ? "Update the details of the selected food item"
            : "Register a new food item and its expiry date");
        formTitle.setText(editing ? "Edit Information" : "Food Information");
        saveButton.setText(editing ? "Save Changes" : "Add Food");

        clearButton.setVisible(!editing);
        cancelButton.setVisible(editing);
    }

    private void updateButtonStates() {

        boolean selected = table != null && table.getSelectedRow() != -1;

        editButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
    }


    // MESSAGES
    private void updateMessage() {

        int shown = table.getRowCount();
        int all = model.getRowCount();

        messageLabel.setText(shown == all
            ? "Showing " + all + " food items"
            : "Showing " + shown + " of " + all + " food items");
    }

    private void toast(String text, boolean success) {
        toast.show(text, success, success ? 2200 : 3800);
    }

    private void showError(String title, Exception ex) {

        String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        toast(title + ": " + msg.replace('\n', ' '), false);
    }

    // ตัวช่วยทำ Animation ด้วย Swing Timer
    private static class Anim {

        static float ease(float p) {
            float q = 1f - p;
            return 1f - q * q * q;
        }

        //step รับค่าความคืบหน้า 0..1
        static Timer run(int durationMs, Consumer<Float> step) {

            final long start = System.nanoTime();
            Timer t = new Timer(15, null);
            t.setInitialDelay(0);

            t.addActionListener(e -> {
                float raw = Math.min(1f, (System.nanoTime() - start) / (durationMs * 1_000_000f));
                step.accept(ease(raw));
                if (raw >= 1f) {
                    ((Timer) e.getSource()).stop();
                }
            });

            t.start();
            return t;
        }
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
            clamp(a.getRed() + (b.getRed() - a.getRed()) * t),
            clamp(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            clamp(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            clamp(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
            Math.max(0, Math.min(255, alpha)));
    }

    private static Font font(int style, int size) {
        return new Font(FONT, style, size);
    }


    // แผงที่เปลี่ยนหน้าแล้วมีเลเยอร์สีพื้นจางออก (Fade-in)
    private static class FadePanel extends JPanel {

        float overlay = 0f;

        FadePanel(LayoutManager lm) {
            super(lm);
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (overlay > 0.01f) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(withAlpha(BG, (int) (overlay * 255)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false;
        }
    }

    //พื้นหลังไล่สี (ใช้กับ Sidebar) 
    private static class GradientPanel extends JPanel {

        private final Color top;
        private final Color bottom;

        GradientPanel(Color top, Color bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // แบนเนอร์หัวหน้า ไล่สี + วงกลมลอยเคลื่อนไหว 
    private class BannerPanel extends JPanel {

        final JLabel titleLabel;
        final JLabel subtitleLabel;

        // x, y (สัดส่วน), รัศมี, ความเร็ว, เฟส
        private final float[][] bubbles = {
            {0.80f, 0.25f, 74f, 0.9f, 0.0f},
            {0.93f, 0.85f, 46f, 1.3f, 1.7f},
            {0.58f, 0.95f, 96f, 0.6f, 3.1f},
            {0.30f, 0.05f, 38f, 1.1f, 4.2f},
            {0.45f, 0.60f, 26f, 1.6f, 5.0f}
        };

        BannerPanel(String title, String subtitle, String right) {

            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(24, 32, 30, 32));
            setPreferredSize(new Dimension(700, 124));

            JPanel titles = new JPanel();
            titles.setOpaque(false);
            titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

            titleLabel = new JLabel(title);
            titleLabel.setFont(font(Font.BOLD, 32));
            titleLabel.setForeground(Color.WHITE);

            subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(font(Font.PLAIN, 15));
            subtitleLabel.setForeground(new Color(226, 230, 255));

            titles.add(titleLabel);
            titles.add(Box.createVerticalStrut(4));
            titles.add(subtitleLabel);

            add(titles, BorderLayout.WEST);

            if (right != null && !right.isEmpty()) {
                JLabel r = new JLabel(right);
                r.setFont(font(Font.BOLD, 14));
                r.setForeground(Color.WHITE);
                add(r, BorderLayout.EAST);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight() - 6;

            Shape shape = new RoundRectangle2D.Float(0, 0, w - 1, h, 24, 24);
            g2.setClip(shape);

            g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, w, h, VIOLET));
            g2.fill(shape);

            for (float[] b : bubbles) {
                double t = phase * b[3] + b[4];
                float cx = (float) ((b[0] + 0.03 * Math.sin(t)) * w);
                float cy = (float) ((b[1] + 0.10 * Math.cos(t * 0.8)) * h);
                float r = b[2];
                g2.setColor(new Color(255, 255, 255, 24));
                g2.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            }

            g2.dispose();
        }
    }

    // แผงมุมมนมีเงา และเปลี่ยนเงา
    private static class RoundedPanel extends JPanel {

        private static final int SHADOW = 6;

        private final Color fill;
        private final int arc;
        private float hover = 0f;
        private float target = 0f;
        private Timer timer;

        Color accent = PRIMARY;

        RoundedPanel(Color fill, int arc) {
            this.fill = fill;
            this.arc = arc;
            setOpaque(false);
        }

        void setHover(boolean on) {
            target = on ? 1f : 0f;
            if (timer == null) {
                timer = new Timer(16, e -> {
                    hover += (target - hover) * 0.22f;
                    if (Math.abs(target - hover) < 0.02f) {
                        hover = target;
                        ((Timer) e.getSource()).stop();
                    }
                    repaint();
                });
            }
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 1;
            int h = getHeight() - 1 - SHADOW;

            // เงา
            for (int i = 1; i <= SHADOW; i++) {
                int alpha = (int) ((5 + 14 * hover) * (SHADOW - i + 1) / (float) SHADOW);
                g2.setColor(new Color(50, 50, 120, alpha));
                g2.fillRoundRect(i, i + 1, w - 2 * i, h, arc, arc);
            }

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(lerp(BORDER, accent, hover));
            g2.drawRoundRect(0, 0, w, h, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    //วงกลมไอคอนในการ์ดสรุปกับวงคลื่นขยายออก
    private class IconBadge extends JComponent {

        private final Color color;
        private final String glyph;

        IconBadge(Color color, String glyph) {
            this.color = color;
            this.glyph = glyph;
            setPreferredSize(new Dimension(68, 68));
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int d = 46;

            // วงคลื่น
            float k = (phase * 0.5f) % 1f;
            int rr = (int) (d / 2 + k * 14);
            g2.setColor(withAlpha(color, (int) (70 * (1 - k))));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - rr, cy - rr, rr * 2, rr * 2);

            g2.setColor(withAlpha(color, 38));
            g2.fillOval(cx - d / 2, cy - d / 2, d, d);

            g2.setColor(color);
            g2.setFont(font(Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(glyph,
                cx - fm.stringWidth(glyph) / 2,
                cy - fm.getHeight() / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ปุ่มมุมมน เปลี่ยนสี hover / active แบบนุ่มนวล
    private static class RoundedButton extends JButton {

        private final Color base;
        private final Color hover;
        private boolean over = false;
        private boolean active = false;
        private float hoverT = 0f;
        private float actT = 0f;
        private final Timer anim;

        int arc = 12;
        Color activeBg = null;
        boolean indicator = false;

        RoundedButton(String text, Color base) {
            this(text, base, base.darker());
        }

        RoundedButton(String text, Color base, Color hover) {
            super(text);
            this.base = base;
            this.hover = hover;

            setFont(font(Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(new EmptyBorder(10, 22, 10, 22));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            anim = new Timer(16, e -> step());

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; anim.start(); }
                @Override public void mouseExited(MouseEvent e) { over = false; anim.start(); }
            });
        }

        private void step() {
            float th = over ? 1f : 0f;
            float ta = active ? 1f : 0f;

            hoverT += (th - hoverT) * 0.25f;
            actT += (ta - actT) * 0.20f;

            if (Math.abs(th - hoverT) < 0.02f) hoverT = th;
            if (Math.abs(ta - actT) < 0.02f) actT = ta;

            if (hoverT == th && actT == ta) {
                anim.stop();
            }
            repaint();
        }

        void setActive(boolean a) {
            this.active = a;
            anim.start();
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            Color c;
            if (!isEnabled()) {
                c = new Color(214, 218, 232);
            } else {
                c = lerp(base, hover, hoverT);
                if (activeBg != null) {
                    c = lerp(c, activeBg, actT);
                }
                if (!indicator && getModel().isPressed()) {
                    c = c.darker();
                }
            }

            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            if (indicator && actT > 0.01f) {
                int bh = (int) (getHeight() / 2 * actT);
                g2.setColor(new Color(165, 180, 252));
                g2.fillRoundRect(0, (getHeight() - bh) / 2, 4, bh, 4, 4);
            }
            g2.dispose();

            super.paintComponent(g);
        }

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            setCursor(new Cursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
    }

    // ช่องกรอกที่มีข้อความจางๆ บอกใบ้
    private static class HintField extends JTextField {

        private final String hint;

        HintField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(160, 166, 184));
                g2.setFont(getFont().deriveFont(Font.PLAIN));

                Insets in = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, in.left, y);
                g2.dispose();
            }
        }
    }

    private static class ToastPane extends JComponent {

        private static final int IN = 320;
        private static final int OUT = 320;

        private String text = null;
        private boolean success = true;
        private int hold = 2200;
        private long start = 0;
        private Timer timer;

        ToastPane() {
            setOpaque(false);
        }

        @Override
        public boolean contains(int x, int y) {
            return false;   // ให้เมาส์ทะลุไปยังหน้าจอด้านล่าง
        }

        void show(String text, boolean success, int holdMs) {

            this.text = text;
            this.success = success;
            this.hold = holdMs;
            this.start = System.currentTimeMillis();

            if (timer == null) {
                timer = new Timer(16, e -> {
                    long el = System.currentTimeMillis() - start;
                    if (el > IN + hold + OUT) {
                        this.text = null;
                        ((Timer) e.getSource()).stop();
                    }
                    repaint();
                });
            }
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {

            if (text == null) {
                return;
            }

            long el = System.currentTimeMillis() - start;
            float p;
            if (el < IN) {
                p = Anim.ease(el / (float) IN);
            } else if (el < IN + hold) {
                p = 1f;
            } else {
                p = 1f - Anim.ease(Math.min(1f, (el - IN - hold) / (float) OUT));
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font f = font(Font.BOLD, 14);
            FontMetrics fm = g2.getFontMetrics(f);

            int maxW = Math.min(620, getWidth() - 60);
            String t = text;
            while (t.length() > 4 && fm.stringWidth(t) > maxW - 76) {
                t = t.substring(0, t.length() - 4) + "...";
            }

            int w = fm.stringWidth(t) + 76;
            int h = 54;
            int x = getWidth() - w - 32 + (int) ((1 - p) * 70);
            int y = getHeight() - h - 32;

            // เงา
            for (int i = 0; i < 6; i++) {
                g2.setColor(new Color(0, 0, 0, (int) (16 * p * (1 - i / 6f))));
                g2.fillRoundRect(x - i, y - i + 4, w + 2 * i, h + 2 * i, 20 + i, 20 + i);
            }

            g2.setColor(new Color(30, 27, 75, (int) (246 * p)));
            g2.fillRoundRect(x, y, w, h, 18, 18);

            // ไอคอนวงกลม
            Color kind = success ? OK : BAD;
            int cx = x + 30;
            int cy = y + h / 2;
            g2.setColor(withAlpha(kind, (int) (255 * p)));
            g2.fillOval(cx - 12, cy - 12, 24, 24);

            g2.setColor(new Color(255, 255, 255, (int) (255 * p)));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (success) {
                g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                g2.drawLine(cx - 1, cy + 4, cx + 6, cy - 4);
            } else {
                g2.drawLine(cx, cy - 6, cx, cy + 1);
                g2.fillOval(cx - 1, cy + 4, 3, 3);
            }

            g2.setFont(f);
            g2.setColor(new Color(255, 255, 255, (int) (255 * p)));
            g2.drawString(t, x + 56, cy + (fm.getAscent() - fm.getDescent()) / 2);

            g2.dispose();
        }
    }
}
