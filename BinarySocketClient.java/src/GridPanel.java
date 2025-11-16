import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GridPanel extends JPanel {

    // Taille de chaque "pixel" en pixels réels à l'écran
    private final int PIXEL_SIZE = 10;

    // Intervalle de l'animation (en ms). 16ms = ~60 FPS
    private static final int ANIMATION_DELAY = 16;

    // La grille statique (murs, feu, etc.) SANS les individus
    private char[][] staticGrille;

    // La liste des individus (dynamiques) que nous animons
    private final List<Individual> individuals;

    private final int largeurGrille;
    private final int hauteurGrille;

    // Le "moteur" d'animation qui se déclenche à intervalle régulier
    private final Timer animationTimer;

    public GridPanel(int largeur, int hauteur) {
        this.largeurGrille = largeur;
        this.hauteurGrille = hauteur;

        // Initialise une grille statique vide
        this.staticGrille = new char[hauteur][largeur];

        // Initialise la liste (thread-safe) des individus
        this.individuals = new ArrayList<>();

        setPreferredSize(new Dimension(largeur * PIXEL_SIZE, hauteur * PIXEL_SIZE));

        // --- NOUVEAU : Démarrage du Timer d'animation ---
        this.animationTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // À chaque "tick" du timer...
                updateIndividuals(); // 1. Mettre à jour la position animée
                repaint();         // 2. Redemander un dessin
            }
        });
        this.animationTimer.start();
    }

    /**
     * Met à jour la grille de données. C'est ici que la "magie" opère.
     * Cette méthode sépare la grille statique des individus.
     * * @param nouvelleGrille La grille 2D complète reçue du serveur.
     */
    public synchronized void setGrille(char[][] nouvelleGrille) {
        // 1. Listes temporaires pour le matching
        List<Point> newPositions = new ArrayList<>();
        char[][] newStaticGrille = new char[hauteurGrille][largeurGrille];

        // 2. Séparer les 'I' du reste
        for (int y = 0; y < hauteurGrille; y++) {
            for (int x = 0; x < largeurGrille; x++) {
                char c = nouvelleGrille[y][x];
                if (c == 'I') {
                    newPositions.add(new Point(x, y));
                    newStaticGrille[y][x] = '.'; // Met un sol sous l'individu
                } else {
                    newStaticGrille[y][x] = c;
                }
            }
        }

        // 3. Mettre à jour la grille statique
        this.staticGrille = newStaticGrille;

        // 4. "Réconcilier" la liste des individus avec les nouvelles positions
        reconcileIndividuals(newPositions);
    }

    /**
     * Compare la liste actuelle des individus avec les nouvelles positions
     * pour "matcher" les mouvements, créer les nouveaux et supprimer les anciens.
     */
    private synchronized void reconcileIndividuals(List<Point> newPositions) {

        List<Individual> unassigned = new ArrayList<>(this.individuals);
        List<Point> toCreate = new ArrayList<>();

        // 1. Tenter de "matcher" les nouvelles positions aux individus existants
        for (Point pos : newPositions) {
            Individual closest = findClosestUnassigned(pos, unassigned);

            if (closest != null) {
                // Trouvé ! C'est (probablement) un mouvement.
                closest.setTarget(pos.x, pos.y); // Donne la nouvelle cible
                unassigned.remove(closest);        // Marque comme "assigné"
            } else {
                // Pas de correspondance proche = un nouvel individu
                toCreate.add(pos);
            }
        }

        // 2. Les individus "unassigned" restants n'ont pas de nouvelle position
        // -> Ils ont disparu.
        this.individuals.removeAll(unassigned);

        // 3. Les positions "toCreate" restantes sont de nouveaux individus
        for (Point pos : toCreate) {
            this.individuals.add(new Individual(pos.x, pos.y));
        }
    }

    /**
     * Trouve l'individu (non assigné) le plus proche d'une position.
     * Limité à une petite distance pour éviter les "téléportations" illogiques.
     */
    private Individual findClosestUnassigned(Point pos, List<Individual> unassigned) {
        // Seuil de distance (en cases). Un individu ne peut pas sauter de plus de 2 cases.
        final double MAX_MOVE_DISTANCE = 2.0;

        Individual closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Individual ind : unassigned) {
            // Distance euclidienne simple
            double dist = Math.sqrt(Math.pow(ind.getTargetX() - pos.x, 2) + Math.pow(ind.getTargetY() - pos.y, 2));

            if (dist < minDistance && dist <= MAX_MOVE_DISTANCE) {
                minDistance = dist;
                closest = ind;
            }
        }
        return closest;
    }

    /**
     * Appelé par le Timer, met à jour la position animée de tous les individus.
     */
    private synchronized void updateIndividuals() {
        for (Individual ind : individuals) {
            ind.update();
        }
    }

    /**
     * Méthode Swing, appelée automatiquement
     * chaque fois que le panneau a besoin d'être redessiné (ex: .repaint()).
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        // --- 1. Dessiner la grille statique (Murs, Feu, Sol...) ---
        if (staticGrille != null) {
            for (int y = 0; y < hauteurGrille; y++) {
                for (int x = 0; x < largeurGrille; x++) {
                    g.setColor(getCouleurPourChar(staticGrille[y][x]));
                    g.fillRect(x * PIXEL_SIZE, y * PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);
                }
            }
        }

        // --- 2. Dessiner tous les individus par-dessus ---
        for (Individual ind : individuals) {
            ind.draw(g, PIXEL_SIZE);
        }
    }

    /**
     * Correspondance entre le caractère de la simulation et une couleur Java.
     */
    private Color getCouleurPourChar(char c) {
        switch (c) {
            case 'M': return Color.BLACK;
            case 'I': return Color.BLUE; // Ne devrait plus être appelé, mais au cas où
            case 'F': return Color.RED;
            case 'S': return Color.GREEN;
            case 'O': return Color.GRAY;
            case '.': return Color.WHITE;
            default: return Color.LIGHT_GRAY;
        }
    }
}