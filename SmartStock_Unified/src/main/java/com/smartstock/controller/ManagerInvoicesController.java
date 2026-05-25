package com.smartstock.controller;

import com.smartstock.dao.TransactionDAO;
import com.smartstock.model.Transaction;
import com.smartstock.model.TransactionItem;
import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import com.smartstock.util.InvoicePDFExporter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ManagerInvoicesController extends VBox {
    private final AuthService authService;
    private final Stage stage;
    private final TransactionDAO transactionDAO;
    private final TableView<Transaction> invoiceTable;
    private final TextArea detailsArea;

    public ManagerInvoicesController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.transactionDAO = new TransactionDAO();
        this.invoiceTable = new TableView<>();
        this.detailsArea = new TextArea();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(12);
        setPadding(new Insets(16));

        Label title = new Label("BRANCH INVOICES");
        title.getStyleClass().add("section-title");

        buildTable();
        buildActions();

        getChildren().addAll(title, invoiceTable, detailsArea);
        VBox.setVgrow(invoiceTable, Priority.ALWAYS);
        detailsArea.setPrefHeight(140);
        detailsArea.setEditable(false);
        detailsArea.setPromptText("Select invoice to view details...");

        loadInvoices();
    }

    private void buildTable() {
        invoiceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        invoiceTable.getColumns().addAll(
                col("Invoice #", "transactionId"),
                col("Cashier", "cashierName"),
                col("Date", "transactionAtFormatted"),
                col("Final Amount", "finalAmount"),
                col("Status", "status")
        );
        invoiceTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            List<TransactionItem> items = transactionDAO.findItemsByTransaction(sel.getTransactionId());
            StringBuilder sb = new StringBuilder();
            sb.append("Invoice #").append(sel.getTransactionId()).append("\n");
            sb.append("Cashier: ").append(sel.getCashierName()).append("\n");
            sb.append("Date: ").append(sel.getTransactionAtFormatted()).append("\n\n");
            for (TransactionItem i : items) {
                sb.append("- ").append(i.getProductName())
                        .append(" | Qty: ").append(i.getQuantity())
                        .append(" | Unit: ").append(String.format("%.2f", i.getUnitPrice()))
                        .append(" | Subtotal: ").append(String.format("%.2f", i.getSubtotal()))
                        .append("\n");
            }
            sb.append("\nFinal: ").append(String.format("%.2f EGP", sel.getFinalAmount()));
            detailsArea.setText(sb.toString());
        });
    }

    private void buildActions() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Button refresh = new Button("Refresh");
        Button download = new Button("Download PDF");
        refresh.setOnAction(e -> loadInvoices());
        download.setOnAction(e -> downloadSelectedInvoice());
        row.getChildren().addAll(refresh, download);
        getChildren().add(row);
    }

    private void loadInvoices() {
        User user = authService.getCurrentUser();
        if (user == null || user.getBranchId() == null) {
            invoiceTable.setItems(FXCollections.observableArrayList());
            return;
        }
        invoiceTable.setItems(FXCollections.observableArrayList(transactionDAO.findByBranch(user.getBranchId())));
    }

    private void downloadSelectedInvoice() {
        Transaction tx = invoiceTable.getSelectionModel().getSelectedItem();
        if (tx == null) return;
        try {
            List<TransactionItem> items = transactionDAO.findItemsByTransaction(tx.getTransactionId());
            Path invoiceDir = Path.of(System.getProperty("user.home"), "SmartStock", "invoices");
            Files.createDirectories(invoiceDir);
            Path generated = invoiceDir.resolve("invoice_" + tx.getTransactionId() + ".pdf");
            InvoicePDFExporter.exportInvoice(tx, items, generated.toString());

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Invoice");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName("invoice_" + tx.getTransactionId() + ".pdf");
            File out = chooser.showSaveDialog(stage);
            if (out == null) return;
            Files.copy(generated, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to download invoice: " + ex.getMessage());
            err.setHeaderText(null);
            err.showAndWait();
        }
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String name, String prop) {
        TableColumn<S, T> c = new TableColumn<>(name);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }
}
