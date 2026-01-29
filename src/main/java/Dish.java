import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private Integer id;
    private Double price;
    private String name;
    private DishTypeEnum dishType;
    private List<DishIngredient> dishIngredients;

    public Double getDishCost() {
        double totalPrice = 0;
        for (DishIngredient dishIngredient : dishIngredients) {
            Double quantity = dishIngredient.getQuantity();
            if (quantity == null) {
                throw new RuntimeException("Some ingredients have undefined quantity");
            }
            totalPrice = totalPrice + dishIngredient.getIngredient().getPrice() * quantity;
        }
        return totalPrice;
    }

    public Dish() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getPrice() { return price; }

    public void setPrice(Double sellingPrice) { this.price = price; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public void setDishType(DishTypeEnum dishType) {
        this.dishType = dishType;
    }

    public List<DishIngredient> getDishIngredients() { return dishIngredients; }

    public void setDishIngredients(List<DishIngredient> dishIngredients) {
        if (dishIngredients == null) {
            this.dishIngredients = new ArrayList<>();
            return;
        }
        for (DishIngredient ingredient : dishIngredients) {
            ingredient.setDish(this);
        }
        this.dishIngredients = dishIngredients;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id) && Objects.equals(name, dish.name) && dishType == dish.dishType && Objects.equals(dishIngredients, dish.dishIngredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dishType, dishIngredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", sellingPrice=" + price +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", cost=" + getDishCost() +
                ", grossMargin=" + getGrossMargin() +
                ", dishIngredients=" + dishIngredients +
                '}';
    }

    public Double getGrossMargin() {
        if (price == null) {
            throw new RuntimeException("Price is null");
        }
        return price - getDishCost();
    }
}