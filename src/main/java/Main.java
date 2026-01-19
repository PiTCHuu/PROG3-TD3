import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever dataRetriever = new DataRetriever();

        System.out.println("=== Test des nouvelles fonctionnalités ===");

        try {
            Dish dish1 = dataRetriever.findDishById(1);
            System.out.println("Plat 1 (Salade fraîche):");
            System.out.println("  - Nom: " + dish1.getName());
            System.out.println("  - Prix de vente: " + dish1.getSellingPrice());
            System.out.println("  - Coût: " + dish1.getDishCost());
            System.out.println("  - Marge brute: " + dish1.getGrossMargin());
            System.out.println("  - Ingrédients:");
            for (DishIngredient di : dish1.getDishIngredients()) {
                System.out.println("    * " + di.getIngredient().getName() +
                        " - " + di.getQuantityRequired() + " " + di.getUnit());
            }
        } catch (Exception e) {
            System.out.println("Erreur pour plat 1: " + e.getMessage());
        }

        System.out.println("\n---\n");

        try {
            Dish dish3 = dataRetriever.findDishById(3);
            System.out.println("Plat 3 (Riz aux légumes):");
            System.out.println("  - Nom: " + dish3.getName());
            System.out.println("  - Prix de vente: " + dish3.getSellingPrice());
            System.out.println("  - Coût: " + dish3.getDishCost());
            System.out.println("  - Marge brute: " + dish3.getGrossMargin());
        } catch (RuntimeException e) {
            System.out.println("  - Exception attendue: " + e.getMessage());
        }

        System.out.println("\n=== Test de création ===");

        Ingredient newIng = new Ingredient(null, "Oignon", CategoryEnum.VEGETABLE, 400.0);
        List<Ingredient> created = dataRetriever.createIngredients(List.of(newIng));
        System.out.println("Ingrédient créé: " + created.get(0));

        DishIngredient newDI = new DishIngredient();
        newDI.setDishId(1);
        newDI.setIngredientId(newIng.getId());
        newDI.setQuantityRequired(0.10);
        newDI.setUnit("KG");

        DishIngredient savedDI = dataRetriever.saveDishIngredient(newDI);
        System.out.println("Association créée: " + savedDI);
    }
}