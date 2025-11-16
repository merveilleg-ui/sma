#Stéphanie
import math
import threading

from individu import Individu
from mur import Mur
from objet import Objet
from sortie import Sortie
from feu import Feu


class Environnement:
    def __init__(self, largeur, hauteur):
        self.largeur = largeur
        self.hauteur = hauteur
        self.personnes = []
        self.murs = []
        self.objets = []
        self.feux = []
        self.sorties = []
        self.grille = [[None for _ in range(largeur)] for _ in range(hauteur)]
        self.lock = threading.Lock()

        self.classes = {
            "Individu": Individu,
            "Objet": Objet,
            "Feu": Feu,
            "Mur": Mur,
            "Sortie": Sortie,
        }

    def est_dans_limites(self, x, y):
        return 0 <= x < self.largeur and 0 <= y < self.hauteur

    def get_element(self, x, y):
        if self.est_dans_limites(x, y):
            return self.grille[y][x]
        return None

    def mettre_a_jour_position(self, individu, new_x, new_y):
        with self.lock:
            for x, y in individu.cases_occupees:
                if self.est_dans_limites(x, y):
                    self.grille[y][x] = None

            individu.x = new_x
            individu.y = new_y
            individu.cases_occupees = [(new_x, new_y), (new_x + 1, new_y)]

            for x, y in individu.cases_occupees:
                if self.est_dans_limites(x, y):
                    self.grille[y][x] = individu

    def supprimer_individu(self, individu):
        with self.lock:
            if individu in self.personnes:
                self.personnes.remove(individu)

            for x, y in individu.cases_occupees:
                if self.est_dans_limites(x, y):
                    self.grille[y][x] = None

    def transformer_en_feu(self, objet):
        with self.lock:
            self.grille[objet.y][objet.x] = None
            if objet in self.objets:
                self.objets.remove(objet)

            new_feu = Feu(objet.x, objet.y, environnement=self)
            self.feux.append(new_feu)
            self.grille[new_feu.y][new_feu.x] = new_feu

    def ajouter_agent(self, agent):
        if hasattr(agent, 'cases_occupees'):
            for x, y in agent.cases_occupees:
                if self.est_dans_limites(x, y):
                    self.grille[y][x] = agent
        else:
            if self.est_dans_limites(agent.x, agent.y):
                self.grille[agent.y][agent.x] = agent

    def get_sortie_la_plus_proche(self, x_individu, y_individu):
        if not self.sorties:
            return None
        return min(self.sorties, key=lambda s: math.sqrt((s.x - x_individu) ** 2 + (s.y - y_individu) ** 2))

    def get_type_case(self, x, y):

        if x < 0 or y < 0 or x >= self.largeur or y >= self.hauteur:
            return "mur"  # bord = mur

        for m in self.murs:
            if m.x == x and m.y == y:
                return "mur"

        for o in self.objets:
            if o.x == x and o.y == y:
                return "objet"

        for f in self.feux:
            if f.x == x and f.y == y:
                return "feu"

        for s in self.sorties:
            if s.x == x and s.y == y:
                return "sortie"

        for p in self.personnes:
            if (x, y) in p.cases_occupees:
                return "individu"

        return "vide"
