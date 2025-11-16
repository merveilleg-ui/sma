#Stéphanie Sbircea
from individu import Individu
import threading
import time

class Feu:
    def __init__(self, x, y, environnement):
        self.x = x
        self.y = y
        self.environnement = environnement

#Cassandre
    def propager(self):
        x_origine, y_origine = self.x, self.y
        for dx in [-1, 0, 1]:
            for dy in [-1, 0, 1]:
                if dx == 0 and dy == 0:
                    continue

                x_cible = x_origine + dx
                y_cible = y_origine + dy

                if not self.environnement.est_dans_limites(x_cible, y_cible): #faire est_dans_limites dans environnement
                    continue
                element_cible = self.environnement.get_element(x_cible, y_cible)

                if isinstance(element_cible, self.environnement.classes["Objet"]) and not element_cible.en_feu:
                    element_cible.prendre_feu()
                    self.environnement.transformer_en_feu(element_cible)
                elif isinstance(element_cible, self.environnement.classes["Individu"]):
                    element_cible.interagir_avec_feu()


class FireThread(threading.Thread):
    def __init__(self, env, interval=1.0):
        super().__init__()
        self.env = env
        self.interval = interval
        self.daemon = True
        self.running = True

    def run(self):
        while self.running:
            for feu in list(self.env.feux):  # ← important : copie pour éviter la modification en cours d'itération
                feu.propager()
            time.sleep(self.interval)

