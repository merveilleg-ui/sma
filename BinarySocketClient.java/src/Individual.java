import java.awt.Color;
import java.awt.Graphics;

/**
 * Représente un individu avec une position animée.
 */
public class Individual {

    // Facteur d'interpolation. Plus il est petit, plus le mouvement est "lissé" (lent).
    // Une valeur de 1.0 désactive l'animation (téléportation).
    private static final double LERP_FACTOR = 0.2;

    // Position actuelle (animée), en coordonnées de grille (ex: 5.5)
    private double currentX;
    private double currentY;

    // Position cible (où l'individu doit aller), en coordonnées de grille (ex: 6)
    private double targetX;
    private double targetY;

    // Indique si l'individu vient d'être créé (pour une apparition instantanée)
    private boolean isNew = true;

    /**
     * Crée un nouvel individu à une position de grille donnée.
     * @param gridX La coordonnée X initiale.
     * @param gridY La coordonnée Y initiale.
     */
    public Individual(int gridX, int gridY) {
        // Au début, la position actuelle et la cible sont les mêmes.
        this.currentX = gridX;
        this.currentY = gridY;
        this.targetX = gridX;
        this.targetY = gridY;
        this.isNew = true; // Apparaît immédiatement
    }

    /**
     * Définit la nouvelle destination de l'individu.
     * @param targetGridX La coordonnée X de la grille cible.
     * @param targetGridY La coordonnée Y de la grille cible.
     */
    public void setTarget(int targetGridX, int targetGridY) {
        this.targetX = targetGridX;
        this.targetY = targetGridY;
        this.isNew = false; // N'est plus "nouveau", doit s'animer
    }

    /**
     * Met à jour la position actuelle, en la rapprochant de la cible.
     * C'est le cœur de l'animation (Interpolation Linéaire ou "Lerp").
     */
    public void update() {
        if (isNew) {
            // Si c'est un nouvel individu, il se "téléporte" à sa cible
            this.currentX = this.targetX;
            this.currentY = this.targetY;
        } else {
            // Sinon, on se déplace d'un pourcentage (LERP_FACTOR) de la distance restante
            this.currentX += (this.targetX - this.currentX) * LERP_FACTOR;
            this.currentY += (this.targetY - this.currentY) * LERP_FACTOR;
        }
    }

    /**
     * Dessine l'individu à sa position actuelle (animée).
     * @param g Le contexte graphique.
     * @param pixelSize La taille d'une case de la grille en pixels.
     */
    public void draw(Graphics g, int pixelSize) {
        g.setColor(Color.BLUE);
        g.fillRect(
                (int) (this.currentX * pixelSize), // Position X en pixels
                (int) (this.currentY * pixelSize), // Position Y en pixels
                pixelSize,
                pixelSize
        );
    }

    /**
     * Retourne la position cible (X) de cet individu.
     */
    public int getTargetX() {
        return (int) targetX;
    }

    /**
     * Retourne la position cible (Y) de cet individu.
     */
    public int getTargetY() {
        return (int) targetY;
    }
}