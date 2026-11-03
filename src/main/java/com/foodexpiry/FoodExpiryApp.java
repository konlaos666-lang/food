/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.foodexpiry;

/**
 *
 * @author PC
 */
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class FoodExpiryApp {

    public static void main(String[] args) {
        
          // สร้างหน้าจอบนเธรดของ Swing
        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            // สร้างและแสดงหน้าต่างหลัก
            new MainFrame().setVisible(true);
        });
    }
}

