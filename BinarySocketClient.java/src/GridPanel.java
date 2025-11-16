import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GridPanel extends JPanel {

    private final int PIXEL_SIZE = 10;
    private static final int ANIMATION_DELAY = 16; // ~60 FPS

    // Grille statique (Murs, Sol, Sorties, Objets)
    private char[][] staticGrille;

    // Listes pour les objets animés
    private final List<Individual> individuals;
    private final List<FireParticle> fireParticles; // NOUVELLE LISTE

    private final int largeurGrille;
    private final int hauteurGrille;
    private final Timer animationTimer;

    public GridPanel(int largeur, int hauteur) {
        this.largeurGrille = largeur;
        this.hauteurGrille = hauteur;

        this.staticGrille = new char[hauteur][largeur];
        this.individuals = new ArrayList<>();
        this.fireParticles = new ArrayList<>(); // Initialisation

        setPreferredSize(new Dimension(largeur * PIXEL_SIZE, hauteur * PIXEL_SIZE));

        // Le Timer met à jour les Individus ET le Feu
        this.animationTimer = new Timer(ANIMATION_DELAY, e -> {
            updateIndividuals();
            updateFireParticles(); // NOUVEL APPEL
            repaint();
        });
        this.animationTimer.start();
    }

    /**
     * Met à jour l'état de la grille en fonction des données du serveur.
     * Sépare les éléments statiques, les individus et le feu.
     */
    public synchronized void setGrille(char[][] nouvelleGrille) {

        List<Point> newIndividualPositions = new ArrayList<>();
        // NOUVEAU : Grille temporaire pour marquer les 'F' vus
        boolean[][] fireSeenOnServer = new boolean[hauteurGrille][largeurGrille];

        // 1. Initialiser la grille statique et trouver les individus/feu
        for (int y = 0; y < hauteurGrille; y++) {
            for (int x = 0; x < largeurGrille; x++) {

                char c = nouvelleGrille[y][x];

                if (c == 'I') {
                    newIndividualPositions.add(new Point(x, y));
                    this.staticGrille[y][x] = '.'; // Sol sous l'individu

                } else if (c == 'F') {
                    fireSeenOnServer[y][x] = true; // Marque le feu
                    this.staticGrille[y][x] = '.'; // Sol sous le feu

                } else {
                    this.staticGrille[y][x] = c; // Mur, Sol, Objet, Sortie
                }
            }
        }

        // 2. Réconcilier les individus (inchangé)
        reconcileIndividuals(newIndividualPositions);

        // 3. NOUVEAU : Réconcilier les particules de feu
        reconcileFire(fireSeenOnServer);
    }

    /**
     * Met à jour la liste des particules de feu en fonction de ce que le serveur a envoyé.
     */
    private synchronized void reconcileFire(boolean[][] fireSeenOnServer) {
        // 1. Marquer les particules existantes comme "non vues"
        for (FireParticle p : fireParticles) {
            p.setSeen(false);
        }

        // 2. Parcourir la grille du serveur
        for (int y = 0; y < hauteurGrille; y++) {
            for (int x = 0; x < largeurGrille; x++) {
                if (fireSeenOnServer[y][x]) {
                    // Le serveur veut du feu ici
                    FireParticle existing = findFireAt(x, y);
                    if (existing != null) {
                        // Le feu existe déjà, on le marque comme "vu"
                        existing.setSeen(true);
                    } else {
                        // C'est un NOUVEAU feu, on crée la particule
                        fireParticles.add(new FireParticle(x, y));
                    }
                }
            }
        }

        // 3. Supprimer les particules de feu qui n'existent plus (non "vues")
        // Utilisation d'un Iterator pour suppression sécurisée
        Iterator<FireParticle> it = fireParticles.iterator();
        while (it.hasNext()) {
            if (!it.next().wasSeen()) {
                it.remove(); // Le feu s'est éteint
            }
        }
    }

    /**
     * Helper : Trouve une particule de feu à une coordonnée (ou null).
     */
    private FireParticle findFireAt(int x, int y) {
        for (FireParticle p : fireParticles) {
            if (p.getX() == x && p.getY() == y) {
                return p;
            }
        }
        return null;
    }

    /**
     * Appelé par le Timer : met à jour l'animation des individus.
     */
    private synchronized void updateIndividuals() {
        for (Individual ind : individuals) {
            ind.update();
        }
    }

    /**
     * Appelé par le Timer : met à jour l'animation du feu.
     */
    private synchronized void updateFireParticles() {
        for (FireParticle p : fireParticles) {
            p.update(); // Fait progresser l'intensité (Orange -> Rouge)
        }
    }

    /**
     * Méthode de dessin (appelée par repaint()).
     */
    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);

        // --- 1. Dessiner la grille statique (Murs, Sol, 'O', 'S') ---
        if (staticGrille != null) {
            for (int y = 0; y < hauteurGrille; y++) {
                for (int x = 0; x < largeurGrille; x++) {
                    g.setColor(getCouleurPourChar(staticGrille[y][x]));
                    g.fillRect(x * PIXEL_SIZE, y * PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);
                }
            }
        }

        // --- 2. Dessiner les particules de feu (par-dessus) ---
        for (FireParticle p : fireParticles) {
            p.draw(g, PIXEL_SIZE);
        }

        // --- 3. Dessiner les individus (par-dessus tout) ---
        for (Individual ind : individuals) {
            ind.draw(g, PIXEL_SIZE);
        }
    }

    /**
     * Couleurs pour la grille STATIQUE uniquement.
     */
    private Color getCouleurPourChar(char c) {
        switch (c) {
            case 'M': return Color.BLACK;
            case 'S': return Color.GREEN;
            case 'O': return Color.GRAY;
            case '.': return Color.WHITE;
            // 'F' et 'I' sont gérés par leurs listes respectives
            default: return Color.LIGHT_GRAY;
        }
    }

    // --- (Les méthodes reconcileIndividuals et findClosestUnassigned restent identiques) ---

    private synchronized void reconcileIndividuals(List<Point> newPositions) {
        List<Individual> unassigned = new ArrayList<>(this.individuals);
        List<Point> toCreate = new ArrayList<>();
        for (Point pos : newPositions) {
            Individual closest = findClosestUnassigned(pos, unassigned);
            if (closest != null) {
                closest.setTarget(pos.x, pos.y);
                unassigned.remove(closest);
            } else {
                toCreate.add(pos);
            }
        }
        this.individuals.removeAll(unassigned);
        for (Point pos : toCreate) {
            this.individuals.add(new Individual(pos.x, pos.y));
        }
    }

    private Individual findClosestUnassigned(Point pos, List<Individual> unassigned) {
        final double MAX_MOVE_DISTANCE = 2.0;
        Individual closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Individual ind : unassigned) {
            double dist = Math.sqrt(Math.pow(ind.getTargetX() - pos.x, 2) + Math.pow(ind.getTargetY() - pos.y, 2));
            if (dist < minDistance && dist <= MAX_MOVE_DISTANCE) {
                minDistance = dist;
                closest = ind;
            }
        }
        return closest;
    }
}