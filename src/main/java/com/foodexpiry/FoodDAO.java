/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.foodexpiry; 

/**
 *
 * @author PC
 */
import java.io.*;                              // BufferedReader, BufferedWriter, IOException
import java.nio.charset.StandardCharsets;      // ใช้ UTF-8 เพื่อให้เก็บภาษาไทยได้
import java.nio.file.*;                        // Path, Paths, Files สำหรับจัดการไฟล์/โฟลเดอร์
import java.sql.SQLException;                  // ใช้เป็นชนิด Exception กลางที่หน้าจอรู้จักอยู่แล้ว
import java.time.LocalDate;                    // ชนิดข้อมูลวันที่ (วัน-เดือน-ปี)
import java.util.*;                            // List, ArrayList

public class FoodDAO {

    // โฟลเดอร์เก็บข้อมูล = <โฟลเดอร์ผู้ใช้>/FoodExpiryTracker
    private final Path folder = Paths.get(
        System.getProperty("user.home"),
        "FoodExpiryTracker"               
    );

    private final Path file = folder.resolve("foods.csv");

    public FoodDAO() {
        try {
            // สร้างโฟลเดอร์
            Files.createDirectories(folder);
            
            if (!Files.exists(file)) {
                // สร้างไฟล์ใหม่
                Files.writeString(
                    file,
                    "id,name,quantity,expiry_date\n",
                    StandardCharsets.UTF_8
                );
            }
        } catch (IOException e) {
            // สร้างโฟลเดอร์/ไฟล์ไม่ได้
            throw new RuntimeException("Cannot create CSV file", e);
        }
    }
    
    // เพิ่มอาหารใหม่
    public void addFood(Food food) throws SQLException {
        try {
            List<Food> foods = getAllFoods();

            // หาidใหม่
            int nextId = 1;

            // วนดูทุกรายการ แล้วเลือก id ที่ "มากที่สุด + 1"
            // เพื่อให้ id ใหม่ไม่ซ้ำกับของเดิม
            for (Food item : foods) {
                nextId = Math.max(nextId, item.getId() + 1);
            }

            food.setId(nextId);
            foods.add(food);   

            saveAll(foods);  

        } catch (Exception e) {
            throw new SQLException("Cannot add food", e);
        }
    }

    // แก้ไขอาหารที่มีอยู่
    public void updateFood(Food food) throws SQLException {
        try {
            List<Food> foods = getAllFoods();   // อ่านรายการ

            // วนหาอาหารที่ id ตรงกับตัวที่ส่งมา
            for (int i = 0; i < foods.size(); i++) {
                if (foods.get(i).getId() == food.getId()) {
                    foods.set(i, food); 
                    break;               
                }
            }

            saveAll(foods);

        } catch (Exception e) {
            throw new SQLException("Cannot update food", e);
        }
    }

    // ลบอาหารตามid
    public void deleteFood(int id) throws SQLException {
        try {
            List<Food> foods = getAllFoods();   // อ่านรายการ

            // ลบรายการที่ id ตรงกัน
            foods.removeIf(food -> food.getId() == id);

            saveAll(foods);   // เขียนกลับลงไฟล์

        } catch (Exception e) {
            throw new SQLException("Cannot delete food", e);
        }
    }
    
    // อ่านอาหารทั้งหมดจากไฟล์ CSV
    public List<Food> getAllFoods() throws SQLException {
        List<Food> foods = new ArrayList<>();   // เก็บผลลัพธ์

        // เปิดไฟล์สำหรับอ่านเป็น UTF-8
        try (BufferedReader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {

            String line;

            reader.readLine(); 

            // อ่านทีละบรรทัดจนหมดไฟล์readLine คืน null เมื่อหมด
            while ((line = reader.readLine()) != null) {

                // ข้ามบรรทัดว่าง
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                List<String> data = parseCSV(line);

                if (data.size() < 4) {
                    continue;
                }

                int id = Integer.parseInt(data.get(0));          // ช่อง 1: รหัส
                String name = data.get(1);                       // ช่อง 2: ชื่ออาหาร
                int quantity = Integer.parseInt(data.get(2));    // ช่อง 3: จำนวน
                LocalDate expiryDate =
                    LocalDate.parse(data.get(3));                // ช่อง 4: วันหมดอายุ

                // สร้าง Food แล้วใส่ลงรายการ
                foods.add(new Food(
                    id, name, quantity, expiryDate
                ));
            }

        } catch (IOException | RuntimeException e) {
            // อ่านไฟล์ไม่ได้ข้อมูลในไฟล์ผิดรูปแบบ
            throw new SQLException("Cannot read CSV file", e);
        }

        return foods; 
    }

    // เขียนรายการทั้งหมดลงไฟล์ CSV
    private void saveAll(List<Food> foods) throws IOException {
        // เปิดไฟล์สำหรับเขียน
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {

            // เขียนบรรทัดหัวตาราง
            writer.write("id,name,quantity,expiry_date");
            writer.newLine(); 

            // เขียนอาหารทีละรายการ
            for (Food food : foods) {
                writer.write(
                    food.getId() + ","                 
                    + escapeCSV(food.getName()) + ","  // ชื่อ (ใส่เครื่องหมาย " ครอบ)
                    + food.getQuantity() + ","         // จำนวน
                    + food.getExpiryDate()             // วันหมดอายุ รูปแบบ YYYY-MM-DD
                );

                writer.newLine();
            }
        }
    }

    // เตรียมข้อความก่อนเขียนลง CSV
    private String escapeCSV(String value) {
        // ถ้าไม่มีค่าให้เขียนเป็นช่องว่าง
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // แยกหนึ่งบรรทัดของ CSV ออกเป็นรายการช่อง
    private List<String> parseCSV(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean insideQuotes = false;             

        // อ่านทีละตัวอักษรของบรรทัด
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (insideQuotes && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {
                    value.append('"'); 
                    i++;                 
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (c == ',' && !insideQuotes) {
          
                result.add(value.toString());   // เก็บช่องลงรายการ
                value.setLength(0);             // ล้างตัวสะสมเตรียมอ่านช่องถัดไป
            } else {
               
                value.append(c);
            }
        }

        result.add(value.toString());

        return result;
    }
}