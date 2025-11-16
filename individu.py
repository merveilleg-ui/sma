#Stéphanie

import math
import heapq
import threading
import time


class Individu:
    def __init__(self, x, y, environnement, sortie=None, id_individu=None):
        self.x = x
        self.y = y
        self.environnement = environnement
        self.etat = "vivant"
        self.vitesse = 1
        self.temps_en_feu = 0
        self.en_attente = False
        self.id_individu = id_individu

        self.cases_occupees = [(x, y)]

        self.a_fait_action = False

    def gerer_interaction_agents(self):
        sortie_proche = self.environnement.get_sortie_la_plus_proche(self.x, self.y)
        if not sortie_proche:
            self.en_attente = False
            return
        ma_distance_sortie = math.sqrt((self.x - sortie_proche.x) ** 2 + (self.y - sortie_proche.y) ** 2)
        self.en_attente = False

        for agent in self.environnement.personnes:
            if agent is self:
                continue

            distance_agent_sortie = math.sqrt((agent.x - sortie_proche.x) ** 2 + (agent.y - sortie_proche.y) ** 2)
            if distance_agent_sortie < ma_distance_sortie and abs(self.x - agent.x) <= 3 and abs(self.y - agent.y) <= 3:
                self.en_attente = True
                break

    def interagir_avec_feu(self):
        pass

    def mourir(self):
        pass

    def est_case_libre_pour_individu(self, x, y):
        type_case = self.environnement.get_type_case(x, y)
        if type_case in ["mur", "objet", "feu"]:
            return False

        for p in self.environnement.personnes:
            if p is not self and (x, y) == (p.x, p.y):
                return False

        return True

    def trouver_chemin_astar(self, debut, fin):
        def heuristique(pos1, pos2):
            # distance de Chebyshev adaptée au mouvement diagonal
            return max(abs(pos1[0] - pos2[0]), abs(pos1[1] - pos2[1]))

        open_set = []
        heapq.heappush(open_set, (0, debut))
        came_from = {}
        g_score = {debut: 0}
        f_score = {debut: heuristique(debut, fin)}

        directions = [
            (-1, 0), (1, 0), (0, -1), (0, 1),  # cardinales
            (-1, -1), (-1, 1), (1, -1), (1, 1)  # diagonales
        ]

        while open_set:
            _, current = heapq.heappop(open_set)

            if current == fin:
                path = []
                while current in came_from:
                    path.append(current)
                    current = came_from[current]
                path.reverse()
                return path

            for dx, dy in directions:
                voisin = (current[0] + dx, current[1] + dy)
                if not self.est_case_libre_pour_individu(voisin[0], voisin[1]):
                    continue

                tentative_g_score = g_score[current] + (1.4 if dx != 0 and dy != 0 else 1)

                if voisin not in g_score or tentative_g_score < g_score[voisin]:
                    came_from[voisin] = current
                    g_score[voisin] = tentative_g_score
                    f_score[voisin] = tentative_g_score + heuristique(voisin, fin)
                    heapq.heappush(open_set, (f_score[voisin], voisin))

        return []

    def deplacer_vers(self):
        if self.en_attente or self.etat != "vivant":
            return

        sortie_proche = self.environnement.get_sortie_la_plus_proche(self.x, self.y)
        if not sortie_proche:
            return

        chemin = self.trouver_chemin_astar((self.x, self.y), (sortie_proche.x, sortie_proche.y))
        if chemin:
            nouvelle_x, nouvelle_y = chemin[0]

            # Mise à jour de la position
            self.environnement.mettre_a_jour_position(self, nouvelle_x, nouvelle_y)
            self.x, self.y = nouvelle_x, nouvelle_y
            self.cases_occupees = [(nouvelle_x, nouvelle_y)]

            # Vérifie si l'individu est sur une sortie
            for s in self.environnement.sorties:
                if (self.x, self.y) == (s.x, s.y):
                    self.environnement.supprimer_individu(self)
                    return

    def percevoir_etat(self):
        return self.environnement.personnes, self.environnement.get_sortie_la_plus_proche(self.x, self.y)


class AgentThread(threading.Thread):
    def __init__(self, individu):
        super().__init__()
        self.individu = individu
        self.daemon = True

    def run(self):
        try:
            while self.individu.etat == "vivant":
                try:
                    self.individu.gerer_interaction_agents()
                    self.individu.deplacer_vers()

                    # NEW
                    self.individu.a_fait_action = True

                except Exception as e:
                    print(f"[ERREUR Agent {self.individu.id_individu}] {e}")
                time.sleep(0.5)
        except Exception as e:
            print(f"[THREAD CRASH] {e}")
