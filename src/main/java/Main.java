import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever dataRetriever = new DataRetriever();

        try {
            System.out.println("--- ÉTAPE 1 : Création d'une nouvelle commande ---");
            Order newOrder = new Order();
            newOrder.setReference("CMD-" + System.currentTimeMillis());
            newOrder.setCreationDatetime(Instant.now());
            newOrder.setType(OrderType.TAKE_AWAY); // 1) a) Gestion du type
            newOrder.setStatus(OrderStatus.CREATED); // 1) b) Gestion du statut
            newOrder.setDishOrderList(new ArrayList<>()); // Liste vide pour le test

            Order savedOrder = dataRetriever.saveOrder(newOrder);
            System.out.println("Commande enregistrée avec ID : " + savedOrder.getId());
            System.out.println("Statut actuel : " + savedOrder.getStatus());

            System.out.println("\n--- ÉTAPE 2 : Passage au statut READY ---");
            savedOrder.setStatus(OrderStatus.READY);
            dataRetriever.saveOrder(savedOrder);
            System.out.println("Mise à jour réussie : Statut = READY");

            System.out.println("\n--- ÉTAPE 3 : Passage au statut DELIVERED ---");
            savedOrder.setStatus(OrderStatus.DELIVERED);
            dataRetriever.saveOrder(savedOrder);
            System.out.println("Commande marquée comme LIVRÉE.");

            System.out.println("\n--- ÉTAPE 4 : Tentative de modification interdite ---");
            // On tente de changer la référence d'une commande déjà livrée
            savedOrder.setReference("MODIF-INTERDITE");

            try {
                dataRetriever.saveOrder(savedOrder);
                System.out.println("ERREUR : La modification aurait dû être bloquée !");
            } catch (RuntimeException e) {
                System.err.println("SUCCÈS : L'exception a bien été levée : " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}