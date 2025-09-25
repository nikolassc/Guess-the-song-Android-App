package com.example.gsong;

import android.content.Context;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtils {

    public static List<String> readSongTitlesFromExcel(Context context, String assetFileName) {
        List<String> songTitles = new ArrayList<>();

        try {
            InputStream inputStream = context.getAssets().open(assetFileName);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getCell(0) != null) {
                    songTitles.add(row.getCell(0).getStringCellValue());
                }
            }

            workbook.close();
            inputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return songTitles;
    }
}
