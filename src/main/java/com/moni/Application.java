package com.moni;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.ParseException;
import java.util.Objects;
import java.util.Scanner;
import java.util.Vector;
import javax.swing.*;
import com.jgoodies.forms.factories.*;

import static javax.swing.JOptionPane.*;

/**
 * @author moni
 */
public class Application extends JFrame implements ActionListener, ChangeListener {
    private final static String APP_TITLE = "Expense Tracker";

    public static void main(String[] args) {
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", APP_TITLE);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_TITLE);
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.textantialiasing", "true");
        }

        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            FlatXcodeDarkIJTheme.setup();
            new Application();
        });
    }

    public Application() {
        var root = getRootPane();
        var taskbar = Taskbar.getTaskbar();

        taskbar.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon-crop.png")));

        if (SystemInfo.isMacOS && SystemInfo.isMacFullWindowContentSupported) {
            root.putClientProperty("apple.awt.fullWindowContent", true);
            root.putClientProperty("apple.awt.transparentTitleBar", true);

            if (SystemInfo.isJava_17_orLater) {
                root.putClientProperty("apple.awt.windowTitleVisible", false);
            } else {
                setTitle(null);
            }
        }

        initComponents();
        initListeners();
        updateTotalFields();
    }

    private void saveHandler() {
        File file = null;

        if (SystemInfo.isMacOS) {
            var fileDialog = new FileDialog(this, "Save to file", FileDialog.SAVE);
            fileDialog.setVisible(true);

            String fileName = fileDialog.getFile(), fileDir = fileDialog.getDirectory();

            if (fileName != null && fileDir != null) {
                file = new File(fileDir, fileName);
            }
        } else {
            var fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setDialogTitle("Save to file");

            var result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
            }
        }

        if (file != null) {
            try (var writer = new FileWriter(file)) {
                var model = (DefaultTableModel) itemTable.getModel();

                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.write(model.getColumnName(i) + "\t");
                }

                writer.write("\n");

                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        writer.write(model.getValueAt(i, j).toString() + "\t");
                    }

                    writer.write("\n");
                }

                showMessageDialog(this, "Saved file successfully.", APP_TITLE, INFORMATION_MESSAGE);
            } catch (IOException e) {
                showMessageDialog(this, "Failed to save file!", APP_TITLE, ERROR_MESSAGE);
            }
        }
    }

    private void loadHandler() {
        File file = null;

        if (SystemInfo.isMacOS) {
            var fileDialog = new FileDialog(this, "Load file", FileDialog.LOAD);
            fileDialog.setVisible(true);

            String fileName = fileDialog.getFile(), fileDir = fileDialog.getDirectory();

            if (fileName != null && fileDir != null) {
                file = new File(fileDir, fileName);
            }
        } else {
            var fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setDialogTitle("Load file");

            var result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
            }
        }

        if (file != null && !file.getAbsolutePath().trim().isEmpty()) {
            try (var sc = new Scanner(file)) {
                var model = (DefaultTableModel) itemTable.getModel();
                model.setRowCount(0);

                sc.nextLine(); // ignore header

                while (sc.hasNextLine()) {
                    var data = sc.nextLine().split("\t");
                    String box = data[0], item = data[1];
                    int quantity = Integer.parseInt(data[2]), price = Integer.parseInt(data[3]);
                    model.addRow(new Object[]{box, item, quantity, price});
                }

                updateExpenses();
            } catch (IOException e) {
                showMessageDialog(this, "Failed to load file!", APP_TITLE, ERROR_MESSAGE);
            }
        }
    }

    private void updateTotalFields() {
        updateTotalCost();
        updateTotalPrice();
        updateExpenses();
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
            if ((Integer) model.getValueAt(i, 2) != 0) {
                items.add((String) model.getValueAt(i, 1));
            }
        }

        itemSelection.setModel(new DefaultComboBoxModel<>(items));
    }

    private void addButtonHandler() {
        var boxName = boxField.getText();
        var itemName = itemField.getText();
        var quantity = (Integer) quantitySpinner.getValue();
        var costPerItem = (Integer) costSpinner.getValue();
        var model = (DefaultTableModel) itemTable.getModel();
        var totalPrice = costPerItem * quantity;

        for (int i = 0; i < model.getRowCount(); i++) {
            if (itemName.equalsIgnoreCase((String) model.getValueAt(i, 1))) {
                int currentQuantity = (int) model.getValueAt(i, 2);
                int currentPrice = (int) model.getValueAt(i, 3);
                int newQuantity = currentQuantity + quantity;
                System.out.println(totalPrice);

                model.setValueAt(newQuantity, i, 2);
                model.setValueAt(currentPrice + totalPrice, i, 3);
            } else if (i == model.getRowCount() - 1) {
                model.addRow(new Object[]{boxName, itemName, quantity, totalPrice});
            }
        }

        updateItemSelection();
        updateExpenses();
    }

    private void sellButtonHandler() {
        var selectedItem = (String) Objects.requireNonNull(itemSelection.getSelectedItem());
        var quantity = (Integer) sellQuantitySpinner.getValue();
        var sellingPrice = (Integer) priceSpinner.getValue();

        var model = (DefaultTableModel) itemTable.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            if (selectedItem.equalsIgnoreCase((String) model.getValueAt(i, 1))) {
                int currentQuantity = (int) model.getValueAt(i, 2);
                int currentPrice = (int) model.getValueAt(i, 3);
                int newQuantity = currentQuantity - quantity;
                int totalPrice = sellingPrice * quantity;

                if (newQuantity < 0) {
                    showMessageDialog(this, "Cannot sell more items than you possess.", APP_TITLE, ERROR_MESSAGE);
                } else {
                    model.setValueAt(newQuantity, i, 2);
                    model.setValueAt(currentPrice - totalPrice, i, 3);
                }

                break;
            }
        }

        updateItemSelection();
        updateExpenses();
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
        var bannerLabel = new JLabel();
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
        var itemLabel1 = new JLabel();
        itemField = new JTextField();
        var quantityLabel = new JLabel();
        quantitySpinner = new JSpinner();
        var costLabel = new JLabel();
        costSpinner = new JSpinner();
        var label10 = new JLabel();
        totalCostField = new JTextField();
        addButton = new JButton();
        var vSpacer4 = new JPanel(null);
        var sellSep = compFactory.createSeparator("Sell");
        var itemLabel2 = new JLabel();
        itemSelection = new JComboBox<>();
        var sellQuantityLabel = new JLabel();
        sellQuantitySpinner = new JSpinner();
        var priceLabel = new JLabel();
        priceSpinner = new JSpinner();
        var totalPriceLabel = new JLabel();
        totalPriceField = new JTextField();
        sellButton = new JButton();
        var vSpacer5 = new JPanel(null);
        var totalExpensesLabel = new JLabel();
        totalExpensesField = new JTextField();
        var vSpacer2 = new JPanel(null);

        //======== this ========
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);
        setAutoRequestFocus(false);
        setFont(new Font("SF Pro Display", Font.PLAIN, 16));
        setResizable(false);
        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/icon-crop.png"))).getImage());
        var contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout)contentPane.getLayout()).columnWidths = new int[] {41, 24, 105, 105, 105, 105, 35, 104, 339, 20, 0};
        ((GridBagLayout)contentPane.getLayout()).rowHeights = new int[] {25, 101, 50, 24, 35, 35, 35, 35, 65, 25, 35, 35, 35, 0, 15, 20, 0};
        ((GridBagLayout)contentPane.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout)contentPane.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

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
        contentPane.add(vSpacer1, new GridBagConstraints(0, 0, 10, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));
        contentPane.add(bannerLabel, new GridBagConstraints(1, 1, 8, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(vSpacer3, new GridBagConstraints(0, 2, 10, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 0), 0, 0));
        contentPane.add(buySep, new GridBagConstraints(1, 3, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(hSpacer2, new GridBagConstraints(0, 1, 1, 15, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 5), 0, 0));
        contentPane.add(tableSep, new GridBagConstraints(7, 3, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

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
        contentPane.add(tablePane, new GridBagConstraints(7, 4, 2, 10, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(hSpacer1, new GridBagConstraints(6, 4, 1, 11, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- boxLabel ----
        boxLabel.setText("Box");
        contentPane.add(boxLabel, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(boxField, new GridBagConstraints(3, 4, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(hSpacer3, new GridBagConstraints(9, 1, 1, 15, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- itemLabel1 ----
        itemLabel1.setText("Item");
        contentPane.add(itemLabel1, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(itemField, new GridBagConstraints(3, 5, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- quantityLabel ----
        quantityLabel.setText("Quantity");
        contentPane.add(quantityLabel, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- quantitySpinner ----
        quantitySpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        contentPane.add(quantitySpinner, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- costLabel ----
        costLabel.setText("Cost per Item");
        costLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(costLabel, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- costSpinner ----
        costSpinner.setModel(new SpinnerNumberModel(500, 0, null, 100));
        contentPane.add(costSpinner, new GridBagConstraints(5, 6, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- label10 ----
        label10.setText("Total Cost");
        contentPane.add(label10, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- totalCostField ----
        totalCostField.setEditable(false);
        totalCostField.setText("100");
        totalCostField.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(totalCostField, new GridBagConstraints(3, 7, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- addButton ----
        addButton.setText("Add");
        contentPane.add(addButton, new GridBagConstraints(5, 7, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(vSpacer4, new GridBagConstraints(1, 8, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(sellSep, new GridBagConstraints(1, 9, 5, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- itemLabel2 ----
        itemLabel2.setText("Item");
        contentPane.add(itemLabel2, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- itemSelection ----
        itemSelection.setModel(new DefaultComboBoxModel<>(new String[] {
            "z",
            "y",
            "x"
        }));
        contentPane.add(itemSelection, new GridBagConstraints(3, 10, 3, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- sellQuantityLabel ----
        sellQuantityLabel.setText("Quantity");
        contentPane.add(sellQuantityLabel, new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- sellQuantitySpinner ----
        sellQuantitySpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
        contentPane.add(sellQuantitySpinner, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- priceLabel ----
        priceLabel.setText("Price per Item");
        priceLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(priceLabel, new GridBagConstraints(4, 11, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- priceSpinner ----
        priceSpinner.setModel(new SpinnerNumberModel(600, 0, null, 100));
        contentPane.add(priceSpinner, new GridBagConstraints(5, 11, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- totalPriceLabel ----
        totalPriceLabel.setText("Total Price");
        contentPane.add(totalPriceLabel, new GridBagConstraints(2, 12, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- totalPriceField ----
        totalPriceField.setEditable(false);
        totalPriceField.setText("1200");
        totalPriceField.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(totalPriceField, new GridBagConstraints(3, 12, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- sellButton ----
        sellButton.setText("Sell");
        contentPane.add(sellButton, new GridBagConstraints(5, 12, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(vSpacer5, new GridBagConstraints(1, 13, 5, 2, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- totalExpensesLabel ----
        totalExpensesLabel.setText("Total Expenses");
        totalExpensesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPane.add(totalExpensesLabel, new GridBagConstraints(7, 14, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));

        //---- totalExpensesField ----
        totalExpensesField.setEditable(false);
        totalExpensesField.setText("0");
        totalExpensesField.setHorizontalAlignment(SwingConstants.TRAILING);
        contentPane.add(totalExpensesField, new GridBagConstraints(8, 14, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(vSpacer2, new GridBagConstraints(0, 15, 10, 1, 0.0, 0.0,
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
