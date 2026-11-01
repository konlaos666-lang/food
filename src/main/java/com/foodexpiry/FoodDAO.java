/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.foodexpiry;

/**
 *
 * @author PC
 */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class FoodDAO {

    private final Path folder = Paths.get(
        System.getProperty("user.home"),
        "FoodExpiryTracker"
    );

    private final Path file = folder.resolve("foods.csv");

    public FoodDAO() {
        try {
            Files.createDirectories(folder);

            if (!Files.exists(file)) {
                Files.writeString(
                    file,
                    "id,name,quantity,expiry_date\n",
                    StandardCharsets.UTF_8
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create CSV file", e);
        }
    }

    public void addFood(Food food) throws SQLException {
        try {
            List<Food> foods = getAllFoods();

            int nextId = 1;

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

    public void updateFood(Food food) throws SQLException {
        try {
            List<Food> foods = getAllFoods();

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

    public void deleteFood(int id) throws SQLException {
        try {
            List<Food> foods = getAllFoods();

            foods.removeIf(food -> food.getId() == id);

            saveAll(foods);

        } catch (Exception e) {
            throw new SQLException("Cannot delete food", e);
        }
    }

    public List<Food> getAllFoods() throws SQLException {
        List<Food> foods = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {

            String line;

            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> data = parseCSV(line);

                if (data.size() < 4) {
                    continue;
                }

                int id = Integer.parseInt(data.get(0));
                String name = data.get(1);
                int quantity = Integer.parseInt(data.get(2));
                LocalDate expiryDate =
                    LocalDate.parse(data.get(3));

                foods.add(new Food(
                    id, name, quantity, expiryDate
                ));
            }

        } catch (IOException | RuntimeException e) {
            throw new SQLException("Cannot read CSV file", e);
        }

        return foods;
    }

    private void saveAll(List<Food> foods) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {

            writer.write("id,name,quantity,expiry_date");
            writer.newLine();

            for (Food food : foods) {
                writer.write(
                    food.getId() + ","
                    + escapeCSV(food.getName()) + ","
                    + food.getQuantity() + ","
                    + food.getExpiryDate()
                );

                writer.newLine();
            }
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private List<String> parseCSV(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean insideQuotes = false;

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
                result.add(value.toString());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }

        result.add(value.toString());

        return result;
    }
}


