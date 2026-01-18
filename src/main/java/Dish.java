import java.util.List;
import java.util.Objects;

public class Dish {
    private Integer id;
    private Double sellingPrice;
    // private Double price;
    private String name;
    private DishTypeEnum dishType;
    private List<DishIngredient> dishIngredients;
    // private List<Ingredient> ingredients;

    public Double getDishCost() {
        if (dishIngredients == null || dishIngredients.isEmpty()) {
            return 0.0;
        }

        double totalCost = 0.0;
        for (DishIngredient di : dishIngredients) {
            if (di.getIngredient() != null && di.getQuantityRequired() != null) {
                totalCost += di.getIngredient().getPrice() * di.getQuantityRequired();
            }
        }
        return totalCost;
    }

    public Dish() {
    }

    public Dish(Integer id, String name, DishTypeEnum dishType, List<DishIngredient> dishIngredients) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.dishIngredients = dishIngredients;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getSellingPrice() { return sellingPrice; }

    public void setSellingPrice(Double sellingPrice) { this.sellingPrice = sellingPrice; }

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
        if (dishIngredients != null) {
            for (DishIngredient di : dishIngredients) {
                di.setDishId(this.id);
            }
        }
        this.dishIngredients = dishIngredients;
    }

    public List<Ingredient> getIngredients() {
        if (dishIngredients == null) return null;

        return dishIngredients.stream()
                .filter(di -> di.getIngredient() != null)
                .map(DishIngredient::getIngredient)
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id) &&
                Objects.equals(sellingPrice, dish.sellingPrice) &&
                Objects.equals(name, dish.name) &&
                dishType == dish.dishType &&
                Objects.equals(dishIngredients, dish.dishIngredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, dishType, dishIngredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", sellingPrice=" + sellingPrice +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", dishIngredients=" + dishIngredients +
                '}';
    }

    public Double getGrossMargin() {
        if (sellingPrice == null) {
            throw new RuntimeException("Le prix de vente du plat est null");
        }
        return sellingPrice - getDishCost();
    }
}