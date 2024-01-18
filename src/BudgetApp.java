import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class BudgetApp extends JFrame {

    // Fields to store financial balances and UI components
    private double walletBalance;
    private double incomeBalance;
    private double expenseBalance;

    private JTextField amountField;
    private JTextField descriptionField;
    private JComboBox<String> transactionTypeDropdown;
    private JTextArea transactionListArea;
    private JLabel walletBalanceLabel;
    private JLabel incomeBalanceLabel;
    private JLabel expenseBalanceLabel;

    private List<Transaction> transactions; // List to store transaction data

    private static final String DATABASE_URL = "jdbc:sqlite:budget.db"; // Database URL

    public BudgetApp() {
        // Initialize balances and transaction list
        walletBalance = 0.0;
        incomeBalance = 0.0;
        expenseBalance = 0.0;
        transactions = new ArrayList<>();

        // Set up the main frame properties
        setTitle("Budget App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize the UI components and create the database
        initUI();
        createDatabase();
    }

    // Method to create the SQLite database and the transactions table
    private void createDatabase() {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {

            // Create transactions table if not exists
            String createTableQuery = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "amount REAL NOT NULL," +
                    "description TEXT NOT NULL," +
                    "transaction_type TEXT NOT NULL)";
            statement.executeUpdate(createTableQuery);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to create the database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to set up the graphical user interface
    private void initUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Create input panel with text fields, combo box, and add button
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2));

        amountField = new JTextField();
        descriptionField = new JTextField();
        transactionTypeDropdown = new JComboBox<>(new String[]{"Income", "Expense"});
        JButton addButton = new JButton("Add Transaction");

        // Add action listener for the "Add Transaction" button
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTransaction();
            }
        });

        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descriptionField);
        inputPanel.add(new JLabel("Transaction Type:"));
        inputPanel.add(transactionTypeDropdown);
        inputPanel.add(new JLabel()); // Empty space
        inputPanel.add(addButton);

        // Create labels for displaying financial balances
        walletBalanceLabel = new JLabel("Wallet Balance: $" + walletBalance);
        incomeBalanceLabel = new JLabel("Income Balance: $" + incomeBalance);
        expenseBalanceLabel = new JLabel("Expense Balance: $" + expenseBalance);

        // Create a panel to display financial balances
        JPanel balancePanel = new JPanel();
        balancePanel.setLayout(new GridLayout(3, 1));
        balancePanel.add(walletBalanceLabel);
        balancePanel.add(incomeBalanceLabel);
        balancePanel.add(expenseBalanceLabel);

        // Create a text area to display the list of transactions
        transactionListArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(transactionListArea);

        // Add components to the main panel
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(balancePanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        add(panel);

        setLocationRelativeTo(null); // Center the frame
    }

    // Method to add a transaction
    private void addTransaction() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            String description = descriptionField.getText();
            String transactionType = (String) transactionTypeDropdown.getSelectedItem();

            // Create a Transaction object and add it to the list
            Transaction transaction = new Transaction(amount, description, transactionType);
            transactions.add(transaction);

            // Update financial balances and transaction list
            updateBalances();
            updateTransactionList();

            // Save the transaction to the database
            saveTransactionToDatabase(transaction);

            // Clear input fields
            amountField.setText("");
            descriptionField.setText("");
            transactionTypeDropdown.setSelectedIndex(0); // Set it back to "Income"
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to save a transaction to the database
    private void saveTransactionToDatabase(Transaction transaction) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO transactions (amount, description, transaction_type) VALUES (?, ?, ?)")) {

            preparedStatement.setDouble(1, transaction.getAmount());
            preparedStatement.setString(2, transaction.getDescription());
            preparedStatement.setString(3, transaction.getTransactionType());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save transaction to the database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to update financial balances
    private void updateBalances() {
        // Update wallet balance based on the latest transaction
        walletBalance += transactions.get(transactions.size() - 1).getAmount()
                * (transactions.get(transactions.size() - 1).getTransactionType().equals("Income") ? 1 : -1);

        // Update income and expense balances
        if (transactions.get(transactions.size() - 1).getTransactionType().equals("Income")) {
            incomeBalance += transactions.get(transactions.size() - 1).getAmount();
        } else {
            expenseBalance += transactions.get(transactions.size() - 1).getAmount();
        }

        // Update the displayed labels
        walletBalanceLabel.setText("Wallet Balance: $" + walletBalance);
        incomeBalanceLabel.setText("Income Balance: $" + incomeBalance);
        expenseBalanceLabel.setText("Expense Balance: $" + expenseBalance);
    }

    // Method to update the transaction list display
    private void updateTransactionList() {
        StringBuilder sb = new StringBuilder();
        for (Transaction transaction : transactions) {
            sb.append(transaction).append("\n");
        }
        transactionListArea.setText(sb.toString());
    }

    // Main method to launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                BudgetApp app = new BudgetApp();
                app.setVisible(true);
            }
        });
    }
}

// Class representing a financial transaction
class Transaction {
    private double amount;
    private String description;
    private String transactionType;

    // Constructor to initialize a transaction
    public Transaction(double amount, String description, String transactionType) {
        this.amount = amount;
        this.description = description;
        this.transactionType = transactionType;
    }

    // Getter method for the transaction amount
    public double getAmount() {
        return amount;
    }

    // Getter method for the transaction description
    public String getDescription() {
        return description;
    }

    // Getter method for the transaction type (Income or Expense)
    public String getTransactionType() {
        return transactionType;
    }

    // Override toString method to provide a string representation of the transaction
    @Override
    public String toString() {
        return String.format("Type: %s, Amount: $%.2f, Description: %s", transactionType, amount, description);
    }
}
