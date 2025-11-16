import java.awt.Color;
import java.awt.Graphics;

/**
 * Représente une particule de feu avec une animation de couleur fluide.
 */
public class FireParticle {

    // Vitesse de l'animation (de 0.0 à 1.0).
    // 0.05 = prend 20 ticks (environ 1/3 sec) pour passer d'orange à rouge.
    private static final double ANIMATION_SPEED = 0.025;

    // Couleurs de début et de fin
    private static final Color START_COLOR = Color.ORANGE; // (255, 200, 0)
    private static final Color END_COLOR = Color.RED;      // (255, 0, 0)

    // Position sur la grille
    private final int x;
    private final int y;

    // Intensité de l'animation (0.0 = Orange, 1.0 = Rouge)
    private double intensity = 0.0;

    // Marqueur pour la réconciliation (voir GridPanel)
    private boolean seenInLastUpdate = true;

    /**
     * Crée une nouvelle particule de feu à une position donnée.
     */
    public FireParticle(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Met à jour l'intensité de l'animation, la faisant progresser vers 1.0.
     */
    public void update() {
        if (this.intensity < 1.0) {
            this.intensity += ANIMATION_SPEED;
            if (this.intensity > 1.0) {
                this.intensity = 1.0;
            }
        }
    }

    /**
     * Dessine la particule de feu avec la couleur interpolée.
     */
    public void draw(Graphics g, int pixelSize) {
        g.setColor(getInterpolatedColor());
        g.fillRect(
                this.x * pixelSize,
                this.y * pixelSize,
                pixelSize,
                pixelSize
        );
    }

    /**
     * Calcule la couleur actuelle entre Orange et Rouge en fonction de l'intensité.
     */
    private Color getInterpolatedColor() {
        if (intensity >= 1.0) {
            return END_COLOR;
        }
        if (intensity <= 0.0) {
            return START_COLOR;
        }

        // Interpolation linéaire entre les composantes Vertes
        // (Rouge et Bleu sont constants dans ce cas)
        int r = START_COLOR.getRed(); // Reste 255
        int g = (int) (START_COLOR.getGreen() + (END_COLOR.getGreen() - START_COLOR.getGreen()) * intensity);
        int b = START_COLOR.getBlue(); // Reste 0

        // Gère les cas limites
        g = Math.max(0, Math.min(255, g));

        return new Color(r, g, b);
    }

    // Getters et setters pour la logique de réconciliation
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean wasSeen() { return seenInLastUpdate; }
    public void setSeen(boolean seen) { this.seenInLastUpdate = seen; }
}
