package com.smartstock.util;

import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVExporter {

    public static void exportBranchReport(List<Branch> branches, String filePath) throws IOException {
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write("ID,Name,Location,Phone,Email,Active,ProductCount,TotalQuantity,LowStockCount\n");
            for (Branch b : branches) {
                fw.write(String.format("%d,%s,%s,%s,%s,%b,%d,%d,%d\n",
                        b.getId(), escapeCsv(b.getName()), escapeCsv(b.getLocation()),
                        escapeCsv(b.getPhone()), escapeCsv(b.getEmail()),
                        b.isActive(), b.getProductCount(), b.getTotalQuantity(), b.getLowStockCount()));
            }
        }
    }

    public static void exportInventoryReport(List<Product> products, String filePath) throws IOException {
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write("ID,Name,Barcode,Category,UnitCost,SellingPrice,Quantity,MinStock\n");
            for (Product p : products) {
                fw.write(String.format("%d,%s,%s,%s,%.2f,%.2f,%d,%d\n",
                        p.getId(), escapeCsv(p.getName()), escapeCsv(p.getBarcode()),
                        escapeCsv(p.getCategory()), p.getUnitCost(), p.getSellingPrice(),
                        p.getQuantity(), p.getMinStock()));
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
