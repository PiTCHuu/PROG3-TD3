import java.util.Objects;

public class DishIngredient {
    private Integer id;
    private Integer dishId;
    private Integer ingredientId;
    private Double quantityRequired;
    private String unit;
    private Ingredient ingredient; // Pour stocker les détails de l'ingrédient

    public DishIngredient() {}

    public DishIngredient(Integer id, Integer dishId, Integer ingredientId,
                          Double quantityRequired, String unit) {
        this.id = id;
        this.dishId = dishId;
        this.ingredientId = ingredientId;
        this.quantityRequired = quantityRequired;
        this.unit = unit;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getDishId() { return dishId; }
    public void setDishId(Integer dishId) { this.dishId = dishId; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    public Double getQuantityRequired() { return quantityRequired; }
    public void setQuantityRequired(Double quantityRequired) { this.quantityRequired = quantityRequired; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DishIngredient that = (DishIngredient) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(dishId, that.dishId) &&
                Objects.equals(ingredientId, that.ingredientId) &&
                Objects.equals(quantityRequired, that.quantityRequired) &&
                Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dishId, ingredientId, quantityRequired, unit);
    }

    @Override
    public String toString() {
        return "DishIngredient{" +
                "id=" + id +
                ", dishId=" + dishId +
                ", ingredientId=" + ingredientId +
                ", quantityRequired=" + quantityRequired +
                ", unit='" + unit + '\'' +
                '}';
    }
}