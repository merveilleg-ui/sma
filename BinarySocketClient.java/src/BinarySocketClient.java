import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class BinarySocketClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 30000;

        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connecté au serveur Python...");
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // --- 1. Lire l'en-tête (Header) AVANT de créer la GUI ---
            int largeur = in.readInt();
            int hauteur = in.readInt();
            System.out.println("Dimensions de la grille reçues: " + largeur + "x" + hauteur);

            // --- 2. Créer l'interface graphique ---
            // 'final' nécessaire pour accéder depuis le thread de la GUI
            final GridPanel gridPanel = new GridPanel(largeur, hauteur);

            // Crée la fenêtre principale
            JFrame frame = new JFrame("Simulation Incendie");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(gridPanel); // Ajoute notre panneau de dessin à la fenêtre
            frame.pack(); // Ajuste la taille de la fenêtre au contenu
            frame.setLocationRelativeTo(null); // Centre la fenêtre

            // Affiche la fenêtre. Doit être fait sur le "Event Dispatch Thread" (EDT)
            SwingUtilities.invokeLater(() -> frame.setVisible(true));

            // Prépare les buffers pour lire les données
            byte[] buffer = new byte[largeur * hauteur];
            char[][] grillePourPanel = new char[hauteur][largeur];

            // --- 3. Boucle de lecture des "frames" (sur le thread principal) ---
            while (true) {
                // Lit exactement le nombre d'octets attendu
                in.readFully(buffer);

                // Convertit le buffer 1D en grille 2D de caractères
                int index = 0;
                for (int y = 0; y < hauteur; y++) {
                    for (int x = 0; x < largeur; x++) {
                        grillePourPanel[y][x] = (char) buffer[index];
                        index++;
                    }
                }

                // --- 4. Mise à jour de la GUI ---
                // Planifie la mise à jour des données et le redessin
                // sur le thread de la GUI (EDT) pour éviter les conflits.
                // Crée une copie pour la sécurité des threads
                final char[][] grilleCopie = new char[hauteur][largeur];
                for (int y = 0; y < hauteur; y++) {
                    System.arraycopy(grillePourPanel[y], 0, grilleCopie[y], 0, largeur);
                }

                SwingUtilities.invokeLater(() -> {
                    gridPanel.setGrille(grilleCopie); // Donne la nouvelle grille au panneau
                    gridPanel.repaint(); // Demande au panneau de se redessiner
                });
            }

        } catch (EOFException e) {
            // C'est normal, le serveur Python a fermé la connexion.
            System.out.println("Simulation terminée (serveur déconnecté).");
            // Ferme la fenêtre proprement
            System.exit(0);
        } catch (IOException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // La méthode afficherGrilleConsole n'est plus nécessaire !

             // Fonction d'aide pour afficher la grille 2D dans la console.

            private static void afficherGrilleConsole(char[][] grille) {
                // "Efface" la console (fonctionne bien dans de nombreux terminaux)
                System.out.print("\033[H\033[2J");
                System.out.flush();

                for (int y = 0; y < grille.length; y++) {
                    for (int x = 0; x < grille[y].length; x++) {
                        System.out.print(grille[y][x] + " ");
                    }
                    System.out.println(); // Saut de ligne
                }
            }
}
