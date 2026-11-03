/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.foodexpiry;

/**
 * Food = ข้อมูลอาหาร 1 รายการ ใช้ส่งข้อมูลระหว่าง MainFrame กับ FoodDAO
 *
 * @author PC
 */
import java.time.LocalDate;

public class Food {

    private int id;               // รหัสรายการ (ใช้แยกตอนแก้ไข/ลบ)
    private String name;          // ชื่ออาหาร
    private int quantity;         // จำนวน
    private LocalDate expiryDate; // วันหมดอายุ

    // อ่านจากไฟล์
    public Food(int id, String name, int quantity,
                LocalDate expiryDate) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    // เพิ่มของใหม่
    public Food(String name, int quantity,
                LocalDate expiryDate) {
        this(0, name, quantity, expiryDate);
    }

    // อ่านค่า 
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    // เปลี่ยนค่า
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
