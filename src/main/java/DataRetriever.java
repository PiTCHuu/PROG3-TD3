import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataRetriever {

    Order findOrderByReference(String reference) {
        DBConnection dbConnection = new DBConnection();
        // Correction : Ajout de guillemets pour "type", "status" et "order"
        try (Connection connection = dbConnection.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT id, reference, creation_datetime, "type", "status" 
                    FROM "order" 
                    WHERE reference LIKE ?""");
            preparedStatement.setString(1, reference);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Order order = new Order();
                Integer idOrder = resultSet.getInt("id");
                order.setId(idOrder);
                order.setReference(resultSet.getString("reference"));
                order.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());

                String typeStr = resultSet.getString("type");
                if (typeStr != null) {
                    order.setType(OrderType.valueOf(typeStr));
                }

                String statusStr = resultSet.getString("status");
                if (statusStr != null) {
                    order.setStatus(OrderStatus.valueOf(statusStr));
                }

                order.setDishOrderList(findDishOrderByIdOrder(idOrder));
                return order;
            }
            throw new RuntimeException("Order not found with reference " + reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order saveOrder(Order toSave) {
        // Correction : Utilisation systématique des guillemets pour les mots réservés
        String upsertOrderSql = """
            INSERT INTO "order" (id, reference, creation_datetime, "type", "status")
            VALUES (?, ?, ?, ?::order_type, ?::order_status)
            ON CONFLICT (id) DO UPDATE
            SET reference = EXCLUDED.reference,
                creation_datetime = EXCLUDED.creation_datetime,
                "type" = EXCLUDED."type",
                "status" = EXCLUDED."status"
            RETURNING id
        """;

        try (Connection conn = new DBConnection().getConnection()) {
            // --- VERIFICATION DU STATUT DELIVERED ---
            if (toSave.getId() != null) {
                String checkStatusSql = "SELECT \"status\" FROM \"order\" WHERE id = ?";
                try (PreparedStatement checkPs = conn.prepareStatement(checkStatusSql)) {
                    checkPs.setInt(1, toSave.getId());
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next()) {
                            String currentStatusStr = rs.getString("status");
                            if (currentStatusStr != null && OrderStatus.DELIVERED.name().equals(currentStatusStr)) {
                                throw new RuntimeException("Une commande livrée ne peut plus être modifiée.");
                            }
                        }
                    }
                }
            }

            conn.setAutoCommit(false);
            Integer orderId;

            try (PreparedStatement ps = conn.prepareStatement(upsertOrderSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "order", "id"));
                }
                ps.setString(2, toSave.getReference());
                ps.setTimestamp(3, Timestamp.from(toSave.getCreationDatetime()));

                // Correction de l'ordre des paramètres (Position 4 = Type, Position 5 = Status)
                if (toSave.getType() != null) {
                    ps.setString(4, toSave.getType().name());
                } else {
                    ps.setNull(4, Types.OTHER);
                }

                if (toSave.getStatus() != null) {
                    ps.setString(5, toSave.getStatus().name());
                } else {
                    ps.setNull(5, Types.OTHER);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    orderId = rs.getInt(1);
                }
            }

            saveDishOrders(conn, orderId, toSave.getDishOrderList());
            conn.commit();
            return findOrderByReference(toSave.getReference());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveDishOrders(Connection conn, Integer orderId, List<DishOrder> dishOrders) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM dish_order WHERE id_order = ?")) {
            del.setInt(1, orderId);
            del.executeUpdate();
        }

        if (dishOrders != null && !dishOrders.isEmpty()) {
            String insertDishOrderSql = "INSERT INTO dish_order (id, id_order, id_dish, quantity) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertDishOrderSql)) {
                for (DishOrder item : dishOrders) {
                    ps.setInt(1, getNextSerialValue(conn, "dish_order", "id"));
                    ps.setInt(2, orderId);
                    ps.setInt(3, item.getDish().getId());
                    ps.setInt(4, item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException("Sequence non trouvée pour " + tableName + "." + columnName);
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        try (PreparedStatement ps = conn.prepareStatement("SELECT nextval(?)")) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
        // Correction : Utilisation de guillemets pour le nom de table et COALESCE à 1 pour éviter l'erreur sur table vide
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 1) FROM \"%s\"))",
                sequenceName, columnName, tableName
        );
        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.executeQuery();
        }
    }

    private String getSerialSequenceName(Connection conn, String tableName, String columnName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT pg_get_serial_sequence(?, ?)")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    // --- AUTRES MÉTHODES (RESTE DU CODE INITIAL SANS MODIFICATION NÉCESSAIRE) ---

    private List<DishOrder> findDishOrderByIdOrder(Integer idOrder) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishOrder> dishOrders = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select id, id_dish, quantity from dish_order where dish_order.id_order = ?");
            preparedStatement.setInt(1, idOrder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Dish dish = findDishById(resultSet.getInt("id_dish"));
                DishOrder dishOrder = new DishOrder();
                dishOrder.setId(resultSet.getInt("id"));
                dishOrder.setQuantity(resultSet.getInt("quantity"));
                dishOrder.setDish(dish);
                dishOrders.add(dishOrder);
            }
            dbConnection.closeConnection(connection);
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select dish.id as dish_id, dish.name as dish_name, dish_type, dish.selling_price as dish_price from dish where dish.id = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setPrice(resultSet.getObject("dish_price") == null ? null : resultSet.getDouble("dish_price"));
                dish.setDishIngredients(findIngredientByDishId(id));
                return dish;
            }
            dbConnection.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Ingredient saveIngredient(Ingredient toSave) {
        String upsertIngredientSql = "INSERT INTO ingredient (id, name, price, category) VALUES (?, ?, ?, ?::dish_type) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, category = EXCLUDED.category, price = EXCLUDED.price RETURNING id";
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer ingredientId;
            try (PreparedStatement ps = conn.prepareStatement(upsertIngredientSql)) {
                ps.setInt(1, toSave.getId() != null ? toSave.getId() : getNextSerialValue(conn, "ingredient", "id"));
                if (toSave.getPrice() != null) ps.setDouble(2, toSave.getPrice()); else ps.setNull(2, Types.DOUBLE);
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getCategory().name());
                try (ResultSet rs = ps.executeQuery()) { rs.next(); ingredientId = rs.getInt(1); }
            }
            insertIngredientStockMovements(conn, toSave);
            conn.commit();
            return findIngredientById(ingredientId);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void insertIngredientStockMovements(Connection conn, Ingredient ingredient) {
        List<StockMovement> stockMovementList = ingredient.getStockMovementList();
        String sql = "insert into stock_movement(id, id_ingredient, quantity, type, unit, creation_datetime) values (?, ?, ?, ?::movement_type, ?::unit, ?) on conflict (id) do nothing";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (StockMovement stockMovement : stockMovementList) {
                preparedStatement.setInt(1, getNextSerialValue(conn, "stock_movement", "id"));
                preparedStatement.setInt(2, ingredient.getId());
                preparedStatement.setDouble(3, stockMovement.getValue().getQuantity());
                preparedStatement.setObject(4, stockMovement.getType());
                preparedStatement.setObject(5, stockMovement.getValue().getUnit());
                preparedStatement.setTimestamp(6, Timestamp.from(stockMovement.getCreationDatetime()));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    Ingredient findIngredientById(Integer id) {
        try (Connection connection = new DBConnection().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select id, name, price, category from ingredient where id = ?;");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int idIngredient = resultSet.getInt("id");
                return new Ingredient(idIngredient, resultSet.getString("name"), CategoryEnum.valueOf(resultSet.getString("category")), resultSet.getDouble("price"), findStockMovementsByIngredientId(idIngredient));
            }
            throw new RuntimeException("Ingredient not found " + id);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    List<StockMovement> findStockMovementsByIngredientId(Integer id) {
        List<StockMovement> stockMovementList = new ArrayList<>();
        try (Connection connection = new DBConnection().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("select id, quantity, unit, type, creation_datetime from stock_movement where id_ingredient = ?;");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                StockMovement sm = new StockMovement();
                sm.setId(resultSet.getInt("id"));
                sm.setType(MovementTypeEnum.valueOf(resultSet.getString("type")));
                sm.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());
                StockValue sv = new StockValue();
                sv.setQuantity(resultSet.getDouble("quantity"));
                sv.setUnit(Unit.valueOf(resultSet.getString("unit")));
                sm.setValue(sv);
                stockMovementList.add(sm);
            }
            return stockMovementList;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    Dish saveDish(Dish toSave) {
        String upsertDishSql = "INSERT INTO dish (id, selling_price, name, dish_type) VALUES (?, ?, ?, ?::dish_type) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, dish_type = EXCLUDED.dish_type, selling_price = EXCLUDED.selling_price RETURNING id";
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                ps.setInt(1, toSave.getId() != null ? toSave.getId() : getNextSerialValue(conn, "dish", "id"));
                if (toSave.getPrice() != null) ps.setDouble(2, toSave.getPrice()); else ps.setNull(2, Types.DOUBLE);
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());
                try (ResultSet rs = ps.executeQuery()) { rs.next(); dishId = rs.getInt(1); }
            }
            detachIngredients(conn, toSave.getDishIngredients());
            attachIngredients(conn, toSave.getDishIngredients());
            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) return List.of();
        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            String insertSql = "INSERT INTO ingredient (id, name, category, price) VALUES (?, ?, ?::ingredient_category, ?) RETURNING id";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    ps.setInt(1, ingredient.getId() != null ? ingredient.getId() : getNextSerialValue(conn, "ingredient", "id"));
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getPrice());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        ingredient.setId(rs.getInt(1));
                        savedIngredients.add(ingredient);
                    }
                }
                conn.commit();
                return savedIngredients;
            } catch (SQLException e) { conn.rollback(); throw new RuntimeException(e); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void detachIngredients(Connection conn, List<DishIngredient> dishIngredients) {
        if (dishIngredients == null || dishIngredients.isEmpty()) return;
        Integer dishId = dishIngredients.get(0).getDish().getId();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_ingredient where id_dish = ?")) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void attachIngredients(Connection conn, List<DishIngredient> ingredients) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) return;
        String attachSql = "insert into dish_ingredient (id, id_ingredient, id_dish, required_quantity, unit) values (?, ?, ?, ?, ?::unit)";
        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (DishIngredient di : ingredients) {
                ps.setInt(1, getNextSerialValue(conn, "dish_ingredient", "id"));
                ps.setInt(2, di.getIngredient().getId());
                ps.setInt(3, di.getDish().getId());
                ps.setDouble(4, di.getQuantity());
                ps.setObject(5, di.getUnit());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<DishIngredient> findIngredientByDishId(Integer idDish) {
        List<DishIngredient> dishIngredients = new ArrayList<>();
        try (Connection connection = new DBConnection().getConnection()) {
            PreparedStatement ps = connection.prepareStatement("select ingredient.id, ingredient.name, ingredient.price, ingredient.category, di.required_quantity, di.unit from ingredient join dish_ingredient di on di.id_ingredient = ingredient.id where id_dish = ?;");
            ps.setInt(1, idDish);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ingredient ing = new Ingredient();
                ing.setId(rs.getInt("id"));
                ing.setName(rs.getString("name"));
                ing.setPrice(rs.getDouble("price"));
                ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));
                DishIngredient di = new DishIngredient();
                di.setIngredient(ing);
                di.setQuantity(rs.getObject("required_quantity") == null ? null : rs.getDouble("required_quantity"));
                di.setUnit(Unit.valueOf(rs.getString("unit")));
                dishIngredients.add(di);
            }
            return dishIngredients;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}