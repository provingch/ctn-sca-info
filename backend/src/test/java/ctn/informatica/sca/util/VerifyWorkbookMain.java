package ctn.informatica.sca.util;

import java.io.FileInputStream;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class VerifyWorkbookMain {
    public static void main(String[] args) throws Exception {
        String path = "/tmp/test_planilla_dinamico.xlsx";
        
        try (FileInputStream fis = new FileInputStream(path);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            System.out.println("\n=== VERIFICACIÓN DEL WORKBOOK ===\n");
            System.out.println("Sheet: " + sheet.getSheetName());
            System.out.println("Max Row: " + sheet.getLastRowNum());
            System.out.println("Max Col: " + sheet.getCTWorksheet().getDimension().getRef());
            
            // Check hidden columns
            System.out.println("\n--- COLUMNAS OCULTAS ---");
            int hiddenCount = 0;
            int visibleCount = 0;
            for (int colIdx = 0; colIdx < 30; colIdx++) {
                if (sheet.isColumnHidden(colIdx)) {
                    System.out.println("  Col " + colIdx + " (letra: " + (char)('A' + colIdx % 26) + "): OCULTA");
                    hiddenCount++;
                } else {
                    visibleCount++;
                }
            }
            
            System.out.println("\n--- RESUMEN ---");
            System.out.println("Columnas visibles: " + visibleCount);
            System.out.println("Columnas ocultas: " + hiddenCount);
            
            // Check for formulas
            System.out.println("\n--- CONTENIDO DE DATOS (rows 1-10) ---");
            for (int rowIdx = 0; rowIdx < Math.min(10, sheet.getLastRowNum() + 1); rowIdx++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIdx);
                if (row != null) {
                    System.out.print("Row " + rowIdx + ": ");
                    for (int colIdx = 0; colIdx < 20; colIdx++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIdx);
                        String value = "";
                        if (cell != null) {
                            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                                value = cell.getStringCellValue();
                            } else if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                                value = String.valueOf(cell.getNumericCellValue());
                            } else if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.FORMULA) {
                                value = "[FORMULA: " + cell.getCellFormula() + "]";
                            }
                        }
                        if (!value.isEmpty()) {
                            System.out.print(" [" + colIdx + "]=" + value.substring(0, Math.min(10, value.length())) + " ");
                        }
                    }
                    System.out.println();
                }
            }
            
            System.out.println("\n✓ Verificación completada");
        }
    }
}
