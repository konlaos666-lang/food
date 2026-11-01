/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.foodexpiry;

/**
 *
 * @author PC
 */
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.sql.SQLException;
import java.util.List;

public class MainFrame extends JFrame {

    private final FoodDAO dao = new FoodDAO();

    private final Color GREEN = new Color(35, 135, 90);
    private final Color DARK_GREEN = new Color(25, 95, 65);
    private final Color LIGHT_GREEN = new Color(232, 246, 237);
    private final Color BG = new Color(246, 249, 247);
    private final Color TEXT = new Color(45, 55, 48);
    private final Color MUTED = new Color(120, 130, 123);
    private final Color BORDER = new Color(225, 232, 227);

    private JTextField nameField;
    private JTextField quantityField;
    private JTextField dateField;
    private JTextField searchField;

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    private JLabel totalValue;
    private JLabel soonValue;
    private JLabel expiredValue;
    private JLabel messageLabel;

    public MainFrame() {

        setTitle("Food Expiry Tracker");
        setSize(1200, 760);
        setMinimumSize(new Dimension(950, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(createSidebar(), BorderLayout.WEST);
        add(createMainContent(), BorderLayout.CENTER);

        refreshTable();
    }

    // ================= SIDEBAR =================

    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBackground(DARK_GREEN);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(25, 15, 20, 15));

        JLabel logo = new JLabel("FOOD TRACKER");
        logo.setFont(new Font("Arial", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("EXPIRY MANAGEMENT");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 10));
        subtitle.setForeground(new Color(205, 230, 213));

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(45));

        JLabel menu = new JLabel("MAIN MENU");
        menu.setForeground(new Color(190, 220, 198));
        menu.setFont(new Font("Arial", Font.BOLD, 11));

        sidebar.add(menu);
        sidebar.add(Box.createVerticalStrut(15));

        JButton dashboard = createMenuButton("Dashboard");
        JButton inventory = createMenuButton("Food Inventory");
        JButton addFood = createMenuButton("Add Food");

        dashboard.addActionListener(e -> refreshTable());

        inventory.addActionListener(e -> {
            table.requestFocusInWindow();
        });

        addFood.addActionListener(e -> {
            nameField.requestFocusInWindow();
        });

        sidebar.add(dashboard);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(inventory);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(addFood);

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("Food Expiry Tracker v1.0");
        footer.setForeground(new Color(205, 230, 213));
        footer.setFont(new Font("Arial", Font.PLAIN, 11));

        sidebar.add(footer);

        return sidebar;
    }

    private JButton createMenuButton(String text) {

        JButton button = new JButton(text);

        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(DARK_GREEN);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 10, 12, 10));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(GREEN);
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(DARK_GREEN);
            }
        });

        return button;
    }

    // ================= MAIN CONTENT =================

    private JPanel createMainContent() {

        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(25, 28, 25, 28));

        main.add(createHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(createDashboardCards());
        content.add(Box.createVerticalStrut(20));
        content.add(createFormPanel());
        content.add(Box.createVerticalStrut(18));
        content.add(createTablePanel());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        main.add(scroll, BorderLayout.CENTER);

        return main;
    }

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Manage your food inventory and expiry dates"
        );
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);

        titles.add(title);
        titles.add(Box.createVerticalStrut(5));
        titles.add(subtitle);

        JLabel date = new JLabel(LocalDate.now().toString());
        date.setFont(new Font("Arial", Font.BOLD, 13));
        date.setForeground(GREEN);

        header.add(titles, BorderLayout.WEST);
        header.add(date, BorderLayout.EAST);

        return header;
    }

    // ================= DASHBOARD CARDS =================

    private JPanel createDashboardCards() {

        JPanel cards = new JPanel(new GridLayout(1, 3, 15, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        totalValue = new JLabel("0");
        soonValue = new JLabel("0");
        expiredValue = new JLabel("0");

        cards.add(createCard(
            "TOTAL FOODS",
            totalValue,
            "All registered food items",
            GREEN
        ));

        cards.add(createCard(
            "EXPIRING SOON",
            soonValue,
            "Within the next 3 days",
            new Color(220, 145, 35)
        ));

        cards.add(createCard(
            "EXPIRED FOODS",
            expiredValue,
            "Past expiry date",
            new Color(205, 65, 65)
        ));

        return cards;
    }

    private JPanel createCard(
            String title, JLabel value, String description, Color accent) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(MUTED);

        value.setFont(new Font("Arial", Font.BOLD, 29));
        value.setForeground(accent);

        JLabel desc = new JLabel(description);
        desc.setFont(new Font("Arial", Font.PLAIN, 11));
        desc.setForeground(MUTED);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(7));
        card.add(value);
        card.add(Box.createVerticalStrut(4));
        card.add(desc);

        return card;
    }

    // ================= FOOD FORM =================

    private JPanel createFormPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel title = new JLabel("Food Information");
        title.setFont(new Font("Arial", Font.BOLD, 17));
        title.setForeground(TEXT);

        panel.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 8));
        fields.setOpaque(false);

        nameField = new JTextField();
        quantityField = new JTextField();
        dateField = new JTextField();

        addField(fields, "Food Name", nameField);
        addField(fields, "Quantity", quantityField);
        addField(fields, "Expiry Date (YYYY-MM-DD)", dateField);

        panel.add(fields, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        JButton addButton = createActionButton("Add Food", GREEN);
        JButton updateButton = createActionButton(
            "Update", new Color(65, 125, 190)
        );
        JButton deleteButton = createActionButton(
            "Delete", new Color(200, 70, 70)
        );
        JButton clearButton = createActionButton(
            "Clear", new Color(110, 120, 114)
        );

        addButton.addActionListener(e -> addFood());
        updateButton.addActionListener(e -> updateFood());
        deleteButton.addActionListener(e -> deleteFood());
        clearButton.addActionListener(e -> clearForm());

        actions.add(addButton);
        actions.add(updateButton);
        actions.add(deleteButton);
        actions.add(clearButton);

        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void addField(
            JPanel panel, String label, JTextField field) {

        JPanel wrapper = new JPanel(new BorderLayout(0, 5));
        wrapper.setOpaque(false);

        JLabel title = new JLabel(label);
        title.setFont(new Font("Arial", Font.BOLD, 11));
        title.setForeground(MUTED);

        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(100, 34));

        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));

        wrapper.add(title, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);

        panel.add(wrapper);
    }

    private JButton createActionButton(String text, Color color) {

        JButton button = new JButton(text);

        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 16, 9, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // ================= FOOD TABLE =================

    private JPanel createTablePanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(18, 20, 18, 20)
        ));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);

        JLabel title = new JLabel("Food Inventory");
        title.setFont(new Font("Arial", Font.BOLD, 17));
        title.setForeground(TEXT);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(230, 34));
        searchField.setFont(new Font("Arial", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(5, 8, 5, 8)
        ));
        searchField.setToolTipText("Search food name...");

        top.add(title, BorderLayout.WEST);
        top.add(searchField, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(
            new Object[]{
                "ID", "Food Name", "Quantity",
                "Expiry Date", "Status"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(34);
        table.setSelectionBackground(LIGHT_GREEN);
        table.setSelectionForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(LIGHT_GREEN);
        header.setForeground(DARK_GREEN);
        header.setPreferredSize(new Dimension(0, 38));

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(4)
            .setCellRenderer(new StatusRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setPreferredSize(new Dimension(500, 250));

        panel.add(scroll, BorderLayout.CENTER);

        messageLabel = new JLabel("Ready");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        messageLabel.setForeground(MUTED);

        panel.add(messageLabel, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(
            new DocumentListener() {

                private void filter() {

                    String keyword = searchField.getText().trim();

                    if (keyword.isEmpty()) {
                        sorter.setRowFilter(null);
                    } else {
                        sorter.setRowFilter(
                            RowFilter.regexFilter(
                                "(?i)" +
                                java.util.regex.Pattern.quote(keyword),
                                1
                            )
                        );
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    filter();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    filter();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    filter();
                }
            }
        );

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedFood();
            }
        });

        return panel;
    }

    // ================= STATUS COLORS =================

    private class StatusRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {

            super.getTableCellRendererComponent(
                table, value, selected, focused, row, column
            );

            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Arial", Font.BOLD, 11));

            if (!selected) {

                String status = value == null ? "" : value.toString();

                switch (status) {

                    case "Normal":
                        setForeground(GREEN);
                        break;

                    case "Expiring Soon":
                    case "Expires Today":
                        setForeground(new Color(210, 135, 25));
                        break;

                    case "Expired":
                        setForeground(new Color(200, 55, 55));
                        break;

                    default:
                        setForeground(TEXT);
                }
            }

            return this;
        }
    }

    // ================= ADD FOOD =================

    private void addFood() {

        try {

            Food food = readForm();

            dao.addFood(food);

            refreshTable();
            clearForm();

            JOptionPane.showMessageDialog(
                this,
                "Food added successfully!"
            );

        } catch (Exception ex) {

            showError("Cannot add food", ex);
        }
    }

    // ================= UPDATE FOOD =================

    private void updateFood() {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a food item first."
            );

            return;
        }

        try {

            int row = table.convertRowIndexToModel(viewRow);

            int id = Integer.parseInt(
                model.getValueAt(row, 0).toString()
            );

            Food input = readForm();

            Food food = new Food(
                id,
                input.getName(),
                input.getQuantity(),
                input.getExpiryDate()
            );

            dao.updateFood(food);

            refreshTable();
            clearForm();

            JOptionPane.showMessageDialog(
                this,
                "Food updated successfully!"
            );

        } catch (Exception ex) {

            showError("Cannot update food", ex);
        }
    }

    // ================= DELETE FOOD =================

    private void deleteFood() {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Please select a food item first."
            );

            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this food?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            int row = table.convertRowIndexToModel(viewRow);

            int id = Integer.parseInt(
                model.getValueAt(row, 0).toString()
            );

            dao.deleteFood(id);

            refreshTable();
            clearForm();

            JOptionPane.showMessageDialog(
                this,
                "Food deleted successfully!"
            );

        } catch (Exception ex) {

            showError("Cannot delete food", ex);
        }
    }

    // ================= READ FORM =================

    private Food readForm() {

        String name = nameField.getText().trim();
        String quantityText = quantityField.getText().trim();
        String dateText = dateField.getText().trim();

        if (name.isEmpty()
                || quantityText.isEmpty()
                || dateText.isEmpty()) {

            throw new IllegalArgumentException(
                "Please fill in all fields."
            );
        }

        int quantity = Integer.parseInt(quantityText);

        if (quantity <= 0) {

            throw new IllegalArgumentException(
                "Quantity must be greater than zero."
            );
        }

        LocalDate date = LocalDate.parse(dateText);

        return new Food(name, quantity, date);
    }

    // ================= REFRESH TABLE =================

    private void refreshTable() {

        try {

            List<Food> foods = dao.getAllFoods();

            model.setRowCount(0);

            int total = 0;
            int soon = 0;
            int expired = 0;

            for (Food food : foods) {

                total++;

                long days = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    food.getExpiryDate()
                );

                String status;

                if (days < 0) {

                    status = "Expired";
                    expired++;

                } else if (days == 0) {

                    status = "Expires Today";
                    soon++;

                } else if (days <= 3) {

                    status = "Expiring Soon";
                    soon++;

                } else {

                    status = "Normal";
                }

                model.addRow(new Object[]{
                    food.getId(),
                    food.getName(),
                    food.getQuantity(),
                    food.getExpiryDate(),
                    status
                });
            }

            totalValue.setText(String.valueOf(total));
            soonValue.setText(String.valueOf(soon));
            expiredValue.setText(String.valueOf(expired));

            messageLabel.setText(
                "Showing " + total + " food items"
            );

        } catch (SQLException ex) {

            messageLabel.setText("Database connection failed");

            showError("Cannot load food data", ex);
        }
    }

    // ================= SELECT FOOD =================

    private void loadSelectedFood() {

        int viewRow = table.getSelectedRow();

        if (viewRow == -1) {
            return;
        }

        int row = table.convertRowIndexToModel(viewRow);

        nameField.setText(
            model.getValueAt(row, 1).toString()
        );

        quantityField.setText(
            model.getValueAt(row, 2).toString()
        );

        dateField.setText(
            model.getValueAt(row, 3).toString()
        );
    }

    // ================= CLEAR FORM =================

    private void clearForm() {

        nameField.setText("");
        quantityField.setText("");
        dateField.setText("");

        table.clearSelection();
        nameField.requestFocusInWindow();
    }

    // ================= ERROR MESSAGE =================

    private void showError(String title, Exception ex) {

        JOptionPane.showMessageDialog(
            this,
            ex.getMessage(),
            title,
            JOptionPane.ERROR_MESSAGE
        );
    }
}

