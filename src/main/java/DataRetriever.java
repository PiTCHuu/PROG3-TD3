import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
    Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try {
            PreparedStatement dishStatement = connection.prepareStatement(
                    "SELECT id, name, dish_type, selling_price FROM dish WHERE id = ?");
            dishStatement.setInt(1, id);
            ResultSet dishResult = dishStatement.executeQuery();

            if (dishResult.next()) {
                Dish dish = new Dish();
                dish.setId(dishResult.getInt("id"));
                dish.setName(dishResult.getString("name"));
                dish.setDishType(DishTypeEnum.valueOf(dishResult.getString("dish_type")));
                dish.setSellingPrice(dishResult.getObject("selling_price") == null ?
                        null : dishResult.getDouble("selling_price"));

                dish.setDishIngredients(findDishIngredientsByDishId(id));

                return dish;
            }
            dbConnection.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DishIngredient> findDishIngredientsByDishId(Integer dishId) {
        List<DishIngredient> dishIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT di.id, di.id_dish, di.id_ingredient, di.quantity_required, di.unit, " +
                            "i.name, i.price, i.category " +
                            "FROM dish_ingredient di " +
                            "JOIN ingredient i ON di.id_ingredient = i.id " +
                            "WHERE di.id_dish = ?");
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DishIngredient dishIngredient = new DishIngredient();
                dishIngredient.setId(rs.getInt("id"));
                dishIngredient.setDishId(rs.getInt("id_dish"));
                dishIngredient.setIngredientId(rs.getInt("id_ingredient"));
                dishIngredient.setQuantityRequired(rs.getDouble("quantity_required"));
                dishIngredient.setUnit(rs.getString("unit"));

                Ingredient ingredient = new Ingredient();
                ingredient.setId(rs.getInt("id_ingredient"));
                ingredient.setName(rs.getString("name"));
                ingredient.setPrice(rs.getDouble("price"));
                ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));

                dishIngredient.setIngredient(ingredient);
                dishIngredients.add(dishIngredient);
            }

            return dishIngredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(connection);
        }
    }

    Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                INSERT INTO dish (id, name, dish_type, selling_price)
                VALUES (?, ?, ?::dish_type, ?)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    dish_type = EXCLUDED.dish_type,
                    selling_price = EXCLUDED.selling_price
                RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;

            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                }
                ps.setString(2, toSave.getName());
                ps.setString(3, toSave.getDishType().name());
                if (toSave.getSellingPrice() != null) {
                    ps.setDouble(4, toSave.getSellingPrice());
                } else {
                    ps.setNull(4, Types.DOUBLE);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            List<DishIngredient> dishIngredients = toSave.getDishIngredients();
            if (dishIngredients != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM dish_ingredient WHERE id_dish = ?")) {
                    ps.setInt(1, dishId);
                    ps.executeUpdate();
                }

                String insertDishIngredientSql = """
                        INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity_required, unit)
                        VALUES (?, ?, ?, ?)
                        """;

                try (PreparedStatement ps = conn.prepareStatement(insertDishIngredientSql)) {
                    for (DishIngredient di : dishIngredients) {
                        ps.setInt(1, dishId);
                        ps.setInt(2, di.getIngredientId());
                        ps.setDouble(3, di.getQuantityRequired());
                        ps.setString(4, di.getUnit());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public DishIngredient saveDishIngredient(DishIngredient dishIngredient) {
        String sql = """
                INSERT INTO dish_ingredient (id, id_dish, id_ingredient, quantity_required, unit)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET id_dish = EXCLUDED.id_dish,
                    id_ingredient = EXCLUDED.id_ingredient,
                    quantity_required = EXCLUDED.quantity_required,
                    unit = EXCLUDED.unit
                RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            PreparedStatement ps = conn.prepareStatement(sql);

            if (dishIngredient.getId() != null) {
                ps.setInt(1, dishIngredient.getId());
            } else {
                ps.setInt(1, getNextSerialValue(conn, "dish_ingredient", "id"));
            }
            ps.setInt(2, dishIngredient.getDishId());
            ps.setInt(3, dishIngredient.getIngredientId());
            ps.setDouble(4, dishIngredient.getQuantityRequired());
            ps.setString(5, dishIngredient.getUnit());

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int generatedId = rs.getInt(1);
                dishIngredient.setId(generatedId);
                return dishIngredient;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }
        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                    INSERT INTO ingredient (id, name, category, price)
                    VALUES (?, ?, ?::ingredient_category, ?)
                    RETURNING id
                    """;
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    if (ingredient.getId() != null) {
                        ps.setInt(1, ingredient.getId());
                    } else {
                        ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                    }
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getPrice());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        int generatedId = rs.getInt(1);
                        ingredient.setId(generatedId);
                        savedIngredients.add(ingredient);
                    }
                }
                conn.commit();
                return savedIngredients;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(conn);
        }
    }


    private void detachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
            throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ingredient SET id_dish = NULL, required_quantity = NULL WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }
            return;
        }

        String baseSql = """
                    UPDATE ingredient
                    SET id_dish = NULL, required_quantity = NULL
                    WHERE id_dish = ? AND id NOT IN (%s)
                """;

        String inClause = ingredients.stream()
                .map(i -> "?")
                .collect(Collectors.joining(","));

        String sql = String.format(baseSql, inClause);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            int index = 2;
            for (Ingredient ingredient : ingredients) {
                ps.setInt(index++, ingredient.getId());
            }
            ps.executeUpdate();
        }
    }

    //private void attachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
    //        throws SQLException {
    //    if (ingredients == null || ingredients.isEmpty()) {
    //        return;
    //    }
    //    String attachSql = """
    //                UPDATE ingredient
    //                SET id_dish = ?, required_quantity = ?
    //                WHERE id = ?
    //            """;
    //    try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
    //        for (Ingredient ingredient : ingredients) {
    //            ps.setInt(1, dishId);
    //            if (ingredient.getQuantity() != null) {
    //                ps.setDouble(2, ingredient.getQuantity());
    //            } else {
    //                ps.setNull(2, Types.DOUBLE);
    //            }
    //            ps.setInt(3, ingredient.getId());
    //            ps.addBatch();
    //        }
    //        ps.executeBatch();
    //    }
    //}

    //private List<Ingredient> findIngredientByDishId(Integer idDish) {
    //    DBConnection dbConnection = new DBConnection();
    //    Connection connection = dbConnection.getConnection();
    //    List<Ingredient> ingredients = new ArrayList<>();
    //    try {
    //        PreparedStatement preparedStatement = connection.prepareStatement(
    //               """
    //                        select ingredient.id, ingredient.name, ingredient.price, ingredient.category, ingredient.required_quantity
    //                        from ingredient where id_dish = ?;
    //                        """);
    //        preparedStatement.setInt(1, idDish);
    //        ResultSet resultSet = preparedStatement.executeQuery();
    //        while (resultSet.next()) {
    //            Ingredient ingredient = new Ingredient();
    //            ingredient.setId(resultSet.getInt("id"));
    //            ingredient.setName(resultSet.getString("name"));
    //            ingredient.setPrice(resultSet.getDouble("price"));
    //            ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));
    //            Object requiredQuantity = resultSet.getObject("required_quantity");
    //            ingredient.setQuantity(requiredQuantity == null ? null : resultSet.getDouble("required_quantity"));
    //            ingredients.add(ingredient);
    //        }
    //        dbConnection.closeConnection(connection);
    //        return ingredients;
    //    } catch (SQLException e) {
    //        throw new RuntimeException(e);
    //    }
    //}


    private String getSerialSequenceName(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sql = "SELECT pg_get_serial_sequence(?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "Any sequence found for " + tableName + "." + columnName
            );
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        String nextValSql = "SELECT nextval(?)";

        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                sequenceName, columnName, tableName
        );

        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }
}