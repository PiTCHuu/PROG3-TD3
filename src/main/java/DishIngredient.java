import java.util.Objects;

public class DishIngredient {
    private Dish dish;
    private Ingredient ingredient;
    private Double quantity;
    private Unit unit;

    public Integer getDish() { return dish; }
    public void setDish(Integer dish) { this.dish = dish; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredientId) { this.ingredient = ingredient; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantityRequired) { this.quantity = quantity; }

    public Unit getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }

    @Override
    public String toString() {
        return "DishIngredient{" +
                "ingredient=" + ingredient +
                ", quantity=" + quantity +
                ", unit=" + unit +
                '}';
    }
}