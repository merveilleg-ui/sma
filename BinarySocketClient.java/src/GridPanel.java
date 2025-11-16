import javax.swing.*;
import java.awt.*;

public class GridPanel extends JPanel {

    // Taille de chaque "pixel" en pixels réels à l'écran
    private final int PIXEL_SIZE = 10;

    private char[][] grille;
    private final int largeurGrille;
    private final int hauteurGrille;

    public GridPanel(int largeur, int hauteur) {
        this.largeurGrille = largeur;
        this.hauteurGrille = hauteur;
        // Initialise une grille vide pour éviter les erreurs au premier dessin
        this.grille = new char[hauteur][largeur];

        // Définit la taille préférée du panneau pour que la fenêtre s'adapte
        setPreferredSize(new Dimension(largeur * PIXEL_SIZE, hauteur * PIXEL_SIZE));
    }

    /**
     * Met à jour la grille de données avec les nouvelles informations reçues du serveur.
     * @param nouvelleGrille La grille 2D de caractères.
     */
    public void setGrille(char[][] nouvelleGrille) {
        this.grille = nouvelleGrille;
    }

    /**
     * Méthode Swing, appelée automatiquement
     * chaque fois que le panneau a besoin d'être redessiné (ex: .repaint()).
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (grille == null) {
            return;
        }

        // Boucle sur chaque cellule de la grille
        for (int y = 0; y < hauteurGrille; y++) {
            for (int x = 0; x < largeurGrille; x++) {

                // Définit la couleur en fonction du caractère
                g.setColor(getCouleurPourChar(grille[y][x]));

                // Dessine un rectangle plein à la position (x, y)
                g.fillRect(
                        x * PIXEL_SIZE, // coordonnée X à l'écran
                        y * PIXEL_SIZE, // coordonnée Y à l'écran
                        PIXEL_SIZE,     // largeur du pixel
                        PIXEL_SIZE      // hauteur du pixel
                );
            }
        }
    }

    // Correspondance entre le caractère de la simulation et une couleur Java.

    private Color getCouleurPourChar(char c) {
        switch (c) {
            case 'M': // Mur
                return Color.BLACK;
            case 'I': // Individu
                return Color.BLUE;
            case 'F': // Feu
                return Color.RED;
            case 'S': // Sortie
                return Color.GREEN;
            case 'O': // Objet (non en feu)
                return Color.GRAY;
            case '.': // Sol / Vide
                return Color.WHITE;
            default: // Inconnu
                return Color.LIGHT_GRAY;
        }
    }
}