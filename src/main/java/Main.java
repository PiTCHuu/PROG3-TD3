import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever dataRetriever = new DataRetriever();

        // Log before changes
        Dish dish = dataRetriever.findDishById(4);
        System.out.println("Before changes: " + dish);

        // Log after changes
        Ingredient ing1 = new Ingredient(1);
        ing1.setQuantity(1.0);
        Ingredient ing2 = new Ingredient(2);
        ing2.setQuantity(2.0);
        dish.setIngredients(List.of(ing1, ing2));
        Dish newDish = dataRetriever.saveDish(dish);
        System.out.println("After changes: " + newDish);

        // Ingredient creations
        List<Ingredient> createdIngredients = dataRetriever.createIngredients(List.of(new Ingredient(null, "Fromage", CategoryEnum.DAIRY, 1200.0)));
        System.out.println("Created ingredients: " + createdIngredients);
    }
}