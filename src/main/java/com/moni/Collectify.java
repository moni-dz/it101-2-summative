/*
 * Created by JFormDesigner on Thu Jun 20 02:01:30 PST 2024
 */

package com.moni;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemInfo;
import com.jgoodies.forms.factories.*;
import com.moni.themes.CollectifyLaf;

import static java.awt.Font.TRUETYPE_FONT;
import static javax.swing.JOptionPane.*;

/**
 * @author moni
 */
public class Collectify extends JFrame implements ActionListener, ChangeListener {
    private final static String APP_TITLE = "Collectify";

    public static void main(String[] args) {
        // macOS setup
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", APP_TITLE);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_TITLE);
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.textantialiasing", "true");
        }

        SwingUtilities.invokeLater(Collectify::new);
    }

    public Collectify() {
        // Load custom fonts
        try {
            var happyChickenFont = Objects.requireNonNull(getClass().getResourceAsStream("/Happy Chicken.ttf"));
            var playgroundFont = Objects.requireNonNull(getClass().getResourceAsStream("/Playground.ttf"));

            var happyChicken = Font.createFont(TRUETYPE_FONT, happyChickenFont);
            var playground = Font.createFont(TRUETYPE_FONT, playgroundFont);

            happyChicken = happyChicken.deriveFont(12f);
            playground = playground.deriveFont(14f);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(happyChicken);
            ge.registerFont(playground);
        } catch (Exception e) {
            showMessageDialog(this, "Failed to load fonts.", "Error", ERROR_MESSAGE);
        }

        // Load custom theme
        FlatLaf.registerCustomDefaultsSource(getClass().getResource("com.moni.themes"));

        if (!CollectifyLaf.setup()) {
            showMessageDialog(this, "Failed to load theme.", "Error", ERROR_MESSAGE);
        }

        // Setup app icon and window decorations
        var root = getRootPane();
        var iconImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));

        if (SystemInfo.isWindows) {
            setIconImage(iconImage);
            setTitle(APP_TITLE);
        } else if (SystemInfo.isMacOS) {
            var taskbar = Taskbar.getTaskbar();
            taskbar.setIconImage(iconImage);

            if (SystemInfo.isMacFullWindowContentSupported) {
                root.putClientProperty("apple.awt.fullWindowContent", true);
                root.putClientProperty("apple.awt.transparentTitleBar", true);

                if (SystemInfo.isJava_17_orLater) {
                    root.putClientProperty("apple.awt.windowTitleVisible", false);
                } else {
                    setTitle(null);
                }
            }
        }

        initComponents();
        initListeners();
        updateFields();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            addButtonHandler();
        } else if (e.getSource() == sellButton) {
            sellButtonHandler();
        } else if (e.getSource() == saveItem) {
            saveHandler();
        } else if (e.getSource() == loadItem) {
            loadHandler();
        } else if (e.getSource() == clearTableItem) {
            var model = (DefaultTableModel) itemTable.getModel();
            model.setRowCount(0);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == costSpinner || e.getSource() == quantitySpinner) {
            try {
                costSpinner.commitEdit();
            } catch (ParseException ex) {
                costSpinner.setValue(1);
            }

            try {
                quantitySpinner.commitEdit();
            } catch (ParseException ex) {
                quantitySpinner.setValue(1);
            }

            updateTotalCost();
        } else if (e.getSource() == priceSpinner || e.getSource() == sellQuantitySpinner) {
            try {
                sellQuantitySpinner.commitEdit();
            } catch (ParseException ex) {
                sellQuantitySpinner.setValue(1);
            }

            try {
                priceSpinner.commitEdit();
            } catch (ParseException ex) {
                priceSpinner.setValue(1);
            }

            updateTotalPrice();
        }
    }

    private void addButtonHandler() {
        var boxName = boxField.getText();
        var itemName = itemField.getText();
        var quantity = (Integer) quantitySpinner.getValue();
        var costPerItem = (Integer) costSpinner.getValue();
        var model = (DefaultTableModel) itemTable.getModel();
        var totalPrice = costPerItem * quantity;

        if (boxName.isEmpty()) {
            showMessageDialog(this, "Please enter a box name.", APP_TITLE, ERROR_MESSAGE);
            return;
        }

        if (itemName.isEmpty()) {
            showMessageDialog(this, "Please enter an item name.", APP_TITLE, ERROR_MESSAGE);
            return;
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{boxName, itemName, quantity, totalPrice});
        } else {
            var row = searchItem(itemName);

            if (row != -1) {
                int currentQuantity = (int) model.getValueAt(row, 2);
                int currentPrice = (int) model.getValueAt(row, 3);
                int newQuantity = currentQuantity + quantity;

                model.setValueAt(newQuantity, row, 2);
                model.setValueAt(currentPrice + totalPrice, row, 3);
            } else {
                model.addRow(new Object[]{boxName, itemName, quantity, totalPrice});
            }
        }

        updateItemSelection();
        updateExpenses();
    }

    private void sellButtonHandler() {
        var selectedItem = (String) itemSelection.getSelectedItem();

        if (selectedItem == null) {
            showMessageDialog(this, "Please select an item.", APP_TITLE, ERROR_MESSAGE);
            return;
        }

        var quantity = (int) sellQuantitySpinner.getValue();
        var sellingPrice = (int) priceSpinner.getValue();
        var model = (DefaultTableModel) itemTable.getModel();
        var row = searchItem(selectedItem);

        int currentQuantity = (int) model.getValueAt(row, 2);
        int currentPrice = (int) model.getValueAt(row, 3);
        int newQuantity = currentQuantity - quantity;
        int totalPrice = sellingPrice * quantity;

        if (newQuantity < 0) {
            showMessageDialog(this, "Cannot sell more items than you possess.", APP_TITLE, ERROR_MESSAGE);
        } else {
            model.setValueAt(newQuantity, row, 2);
            model.setValueAt(currentPrice - totalPrice, row, 3);
        }

        updateItemSelection();
        updateExpenses();
    }

    private void saveHandler() {
        selectFile(FileDialog.SAVE).ifPresent(path -> {
            var model = (DefaultTableModel) itemTable.getModel();
            var data = new StringBuilder();

            data.append("Box\tItem\tQuantity\tPrice\n");

            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    data.append(model.getValueAt(row, col));

                    if (col != model.getColumnCount() - 1) {
                        data.append('\t');
                    }
                }

                data.append('\n');
            }

            try {
                Files.writeString(path, data.toString());
                showMessageDialog(this, "Saved file successfully.", APP_TITLE, INFORMATION_MESSAGE);
            } catch (IOException e) {
                showMessageDialog(this, "Failed to save file!", APP_TITLE, ERROR_MESSAGE);
            }
        });
    }

    private void loadHandler() {
        selectFile(FileDialog.LOAD).ifPresent(path -> {
            if (Files.exists(path)) {
                try {
                    var model = (DefaultTableModel) itemTable.getModel();
                    var data = Files.readAllLines(path);

                    model.setRowCount(0); // Clear the table
                    data.removeFirst(); // Remove the header of the file

                    for (String row : data) {
                        var entry = row.split("\t");
                        model.addRow(new Object[]{entry[0], entry[1], Integer.parseInt(entry[2]), Integer.parseInt(entry[3])});
                    }

                    showMessageDialog(this, "Loaded file successfully.", APP_TITLE, INFORMATION_MESSAGE);
                } catch (IOException e) {
                    showMessageDialog(this, "Failed to load file!", APP_TITLE, ERROR_MESSAGE);
                }
            }

            updateFields();
        });
    }

    private void updateFields() {
        updateTotalCost();
        updateTotalPrice();
        updateExpenses();
        updateItemSelection();
    }

    private void updateTotalCost() {
        var quantity = (Integer) quantitySpinner.getValue();
        var cost = (Integer) costSpinner.getValue();
        totalCostField.setText(String.valueOf(quantity * cost));
    }

    private void updateTotalPrice() {
        var sellQuantity = (Integer) sellQuantitySpinner.getValue();
        var price = (Integer) priceSpinner.getValue();
        totalPriceField.setText(String.valueOf(sellQuantity * price));
    }

    private void updateExpenses() {
        var model = (DefaultTableModel) itemTable.getModel();
        var total = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            total += Integer.parseInt(model.getValueAt(i, 3).toString());
        }

        totalExpensesField.setText(String.valueOf(total));
    }

    private void updateItemSelection() {
        var model = (DefaultTableModel) itemTable.getModel();
        var items = new Vector<String>();

        for (int i = 0; i < model.getRowCount(); i++) {
            if ((int) model.getValueAt(i, 2) != 0) {
                items.add((String) model.getValueAt(i, 1));
            }
        }

        itemSelection.setModel(new DefaultComboBoxModel<>(items));
    }

    // Show file dialog and return a path if it exists
    private Optional<Path> selectFile(int mode) {
        var title = (mode == FileDialog.LOAD) ? "Load file" : "Save to file";

        if (SystemInfo.isMacOS) {
            var fileDialog = new FileDialog(this, title, mode);
            fileDialog.setVisible(true);

            var fileName = fileDialog.getFile();
            var fileDir = fileDialog.getDirectory();

            if (fileName != null && fileDir != null) {
                return Optional.of(Path.of(fileDir, fileName));
            }
        } else {
            var fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setDialogTitle(title);

            var result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                return Optional.of(fileChooser.getSelectedFile().toPath());
            }
        }

        return Optional.empty();
    }

    // Search and return the index of the item in the table if it exists, otherwise return -1
    private int searchItem(String item) {
        var model = (DefaultTableModel) itemTable.getModel();

        for (int row = 0; row < model.getRowCount(); row++) {
            if (item.equalsIgnoreCase(model.getValueAt(row, 1).toString())) {
                return row;
            }
        }

        return -1;
    }

    private void initListeners() {
        addButton.addActionListener(this);
        itemSelection.addActionListener(this);
        costSpinner.addChangeListener(this);
        quantitySpinner.addChangeListener(this);
        sellQuantitySpinner.addChangeListener(this);
        priceSpinner.addChangeListener(this);
        totalCostField.addActionListener(this);
        sellButton.addActionListener(this);
        saveItem.addActionListener(this);
        loadItem.addActionListener(this);
        clearTableItem.addActionListener(this);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner Educational license - Lythe Marvin Lacre
        DefaultComponentFactory compFactory = DefaultComponentFactory.getInstance();
        var menuBar = new JMenuBar();
        var fileMenu = new JMenu();
        saveItem = new JMenuItem();
        loadItem = new JMenuItem();
        var editMenu = new JMenu();
        clearTableItem = new JMenuItem();
        var vSpacer1 = new JPanel(null);
        var vSpacer3 = new JPanel(null);
        var buySep = compFactory.createSeparator("Buy");
        var hSpacer2 = new JPanel(null);
        var tableSep = compFactory.createSeparator("Purchasing Data");
        var tablePane = new JScrollPane();
        itemTable = new JTable();
        var hSpacer1 = new JPanel(null);
        var boxLabel = new JLabel();
        boxField = new JTextField();
        var hSpacer3 = new JPanel(null);
        var bannerPanel = new JPanel();
        var bannerLabel = new JLabel();
        var hSpacer4 = new JPanel(null);
        var hSpacer10 = new JPanel(null);
        var vSpacer7 = new JPanel(null);
        var itemLabel1 = new JLabel();
        itemField = new JTextField();
        var vSpacer8 = new JPanel(null);
        var quantityLabel = new JLabel();
        quantitySpinner = new JSpinner();
        var hSpacer7 = new JPanel(null);
        var costLabel = new JLabel();
        costSpinner = new JSpinner();
        var vSpacer9 = new JPanel(null);
        var label10 = new JLabel();
        totalCostField = new JTextField();
        addButton = new JButton();
        var vSpacer4 = new JPanel(null);
        var sellSep = compFactory.createSeparator("Sell");
        var hSpacer5 = new JPanel(null);
        var itemLabel2 = new JLabel();
        itemSelection = new JComboBox<>();
        var vSpacer10 = new JPanel(null);
        var sellQuantityLabel = new JLabel();
        sellQuantitySpinner = new JSpinner();
        var priceLabel = new JLabel();
        priceSpinner = new JSpinner();
        var vSpacer11 = new JPanel(null);
        var totalPriceLabel = new JLabel();
        totalPriceField = new JTextField();
        var hSpacer6 = new JPanel(null);
        var hSpacer11 = new JPanel(null);
        var hSpacer8 = new JPanel(null);
        sellButton = new JButton();
        var vSpacer5 = new JPanel(null);
        var vSpacer6 = new JPanel(null);
        var totalExpensesLabel = new JLabel();
        var hSpacer9 = new JPanel(null);
        totalExpensesField = new JTextField();
        var vSpacer2 = new JPanel(null);

        //======== this ========
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);
        setAutoRequestFocus(false);
        setFont(new Font("Happy Chicken", Font.PLAIN, 16));
        setResizable(false);
        setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());
        setBackground(new Color(0xffb4d6));
        setForeground(new Color(0xffb4d6));
        var contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout)contentPane.getLayout()).columnWidths = new int[] {36, 19, 100, 15, 100, 25, 100, 20, 100, 30, 99, 25, 334, 20, 0};
        ((GridBagLayout)contentPane.getLayout()).rowHeights = new int[] {20, 96, 45, 19, 30, 5, 30, 5, 30, 6, 30, 60, 20, 30, 5, 30, 5, 30, 0, 19, 10, 20, 0};
        ((GridBagLayout)contentPane.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout)contentPane.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

        //======== menuBar ========
        {

            //======== fileMenu ========
            {
                fileMenu.setText("File");

                //---- saveItem ----
                saveItem.setText("Save");
                fileMenu.add(saveItem);

                //---- loadItem ----
                loadItem.setText("Load");
                fileMenu.add(loadItem);
            }
            menuBar.add(fileMenu);

            //======== editMenu ========
            {
                editMenu.setText("Edit");

                //---- clearTableItem ----
                clearTableItem.setText("Clear Table");
                editMenu.add(clearTableItem);
            }
            menuBar.add(editMenu);
        }
        setJMenuBar(menuBar);
        contentPane.add(vSpacer1, new GridBagConstraints(0, 0, 14, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer3, new GridBagConstraints(0, 2, 14, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- buySep ----
        buySep.setForeground(new Color(0xffd2e0));
        buySep.setBackground(new Color(0xffd2e0));
        buySep.setOpaque(true);
        buySep.setFont(new Font(".AppleSystemUIFont", Font.BOLD, 13));
        contentPane.add(buySep, new GridBagConstraints(1, 3, 8, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer2, new GridBagConstraints(0, 0, 1, 22, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- tableSep ----
        tableSep.setOpaque(true);
        tableSep.setBackground(new Color(0xffd2e0));
        contentPane.add(tableSep, new GridBagConstraints(10, 3, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //======== tablePane ========
        {

            //---- itemTable ----
            itemTable.setModel(new DefaultTableModel(
                new Object[][] {
                    {"A", "x", 2, 1200},
                    {"B", "y", 1, 800},
                    {"C", "z", 5, 3500},
                },
                new String[] {
                    "Box", "Item", "Quantity", "Price"
                }
            ) {
                Class<?>[] columnTypes = new Class<?>[] {
                    String.class, String.class, Integer.class, Integer.class
                };
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnTypes[columnIndex];
                }
            });
            itemTable.setAutoCreateRowSorter(true);
            tablePane.setViewportView(itemTable);
        }
        contentPane.add(tablePane, new GridBagConstraints(10, 4, 3, 15, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer1, new GridBagConstraints(9, 2, 1, 20, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- boxLabel ----
        boxLabel.setText("Box");
        boxLabel.setBackground(new Color(0xffd2e0));
        boxLabel.setOpaque(true);
        contentPane.add(boxLabel, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(boxField, new GridBagConstraints(4, 4, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer3, new GridBagConstraints(13, 0, 1, 22, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //======== bannerPanel ========
        {
            bannerPanel.setBackground(new Color(0xffd2e0));
            bannerPanel.setForeground(new Color(0xffd2e0));
            bannerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

            //---- bannerLabel ----
            bannerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            bannerLabel.setForeground(new Color(0xffb4d6));
            bannerLabel.setBackground(new Color(0xffb4d6));
            bannerLabel.setIcon(new ImageIcon(getClass().getResource("/banner.png")));
            bannerPanel.add(bannerLabel);
        }
        contentPane.add(bannerPanel, new GridBagConstraints(0, 1, 14, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer4, new GridBagConstraints(1, 4, 1, 7, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer10, new GridBagConstraints(3, 4, 1, 7, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer7, new GridBagConstraints(2, 5, 7, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- itemLabel1 ----
        itemLabel1.setText("Item");
        itemLabel1.setOpaque(true);
        itemLabel1.setBackground(new Color(0xffd2e0));
        contentPane.add(itemLabel1, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(itemField, new GridBagConstraints(4, 6, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer8, new GridBagConstraints(2, 7, 7, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- quantityLabel ----
        quantityLabel.setText("Quantity");
        quantityLabel.setOpaque(true);
        quantityLabel.setBackground(new Color(0xffd2e0));
        contentPane.add(quantityLabel, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- quantitySpinner ----
        quantitySpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        contentPane.add(quantitySpinner, new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer7, new GridBagConstraints(5, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- costLabel ----
        costLabel.setText("Cost per Item");
        costLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        costLabel.setOpaque(true);
        costLabel.setBackground(new Color(0xffd2e0));
        contentPane.add(costLabel, new GridBagConstraints(6, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- costSpinner ----
        costSpinner.setModel(new SpinnerNumberModel(500, 0, null, 100));
        contentPane.add(costSpinner, new GridBagConstraints(8, 8, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer9, new GridBagConstraints(2, 9, 7, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- label10 ----
        label10.setText("Total Cost");
        label10.setOpaque(true);
        label10.setBackground(new Color(0xffd2e0));
        contentPane.add(label10, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- totalCostField ----
        totalCostField.setEditable(false);
        totalCostField.setText("100");
        totalCostField.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(totalCostField, new GridBagConstraints(4, 10, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- addButton ----
        addButton.setText("Add");
        contentPane.add(addButton, new GridBagConstraints(8, 10, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer4, new GridBagConstraints(0, 11, 10, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- sellSep ----
        sellSep.setBackground(new Color(0xffd2e0));
        sellSep.setOpaque(true);
        contentPane.add(sellSep, new GridBagConstraints(1, 12, 8, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer5, new GridBagConstraints(1, 13, 1, 5, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- itemLabel2 ----
        itemLabel2.setText("Item");
        itemLabel2.setOpaque(true);
        itemLabel2.setBackground(new Color(0xffd2e0));
        contentPane.add(itemLabel2, new GridBagConstraints(2, 13, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- itemSelection ----
        itemSelection.setModel(new DefaultComboBoxModel<>(new String[] {
            "z",
            "y",
            "x"
        }));
        contentPane.add(itemSelection, new GridBagConstraints(4, 13, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer10, new GridBagConstraints(2, 14, 7, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- sellQuantityLabel ----
        sellQuantityLabel.setText("Quantity");
        sellQuantityLabel.setOpaque(true);
        sellQuantityLabel.setBackground(new Color(0xffd2e0));
        contentPane.add(sellQuantityLabel, new GridBagConstraints(2, 15, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- sellQuantitySpinner ----
        sellQuantitySpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        contentPane.add(sellQuantitySpinner, new GridBagConstraints(4, 15, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- priceLabel ----
        priceLabel.setText("Price per Item");
        priceLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        priceLabel.setOpaque(true);
        priceLabel.setBackground(new Color(0xffd2e0));
        contentPane.add(priceLabel, new GridBagConstraints(6, 15, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- priceSpinner ----
        priceSpinner.setModel(new SpinnerNumberModel(600, 0, null, 100));
        contentPane.add(priceSpinner, new GridBagConstraints(8, 15, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer11, new GridBagConstraints(2, 16, 7, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- totalPriceLabel ----
        totalPriceLabel.setText("Total Price");
        totalPriceLabel.setOpaque(true);
        totalPriceLabel.setBackground(new Color(0xffd2e0));
        contentPane.add(totalPriceLabel, new GridBagConstraints(2, 17, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- totalPriceField ----
        totalPriceField.setEditable(false);
        totalPriceField.setText("1200");
        totalPriceField.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(totalPriceField, new GridBagConstraints(4, 17, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer6, new GridBagConstraints(7, 8, 1, 10, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer11, new GridBagConstraints(3, 13, 1, 5, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer8, new GridBagConstraints(5, 15, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- sellButton ----
        sellButton.setText("Sell");
        contentPane.add(sellButton, new GridBagConstraints(8, 17, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer5, new GridBagConstraints(0, 18, 10, 4, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer6, new GridBagConstraints(10, 19, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- totalExpensesLabel ----
        totalExpensesLabel.setText("Total Expenses");
        totalExpensesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        totalExpensesLabel.setOpaque(true);
        totalExpensesLabel.setBackground(new Color(0xffd2e0));
        contentPane.add(totalExpensesLabel, new GridBagConstraints(10, 20, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(hSpacer9, new GridBagConstraints(11, 20, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- totalExpensesField ----
        totalExpensesField.setEditable(false);
        totalExpensesField.setText("0");
        totalExpensesField.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(totalExpensesField, new GridBagConstraints(12, 20, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        contentPane.add(vSpacer2, new GridBagConstraints(0, 21, 14, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner Educational license - Lythe Marvin Lacre
    private JMenuItem saveItem;
    private JMenuItem loadItem;
    private JMenuItem clearTableItem;
    private JTable itemTable;
    private JTextField boxField;
    private JTextField itemField;
    private JSpinner quantitySpinner;
    private JSpinner costSpinner;
    private JTextField totalCostField;
    private JButton addButton;
    private JComboBox<String> itemSelection;
    private JSpinner sellQuantitySpinner;
    private JSpinner priceSpinner;
    private JTextField totalPriceField;
    private JButton sellButton;
    private JTextField totalExpensesField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
