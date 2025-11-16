#Stéphanie Sbircea

#Stéphanie

class Objet:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.en_feu = False

    #Les objets sont inflammables et peuvent prendre feu, permettant au feu de se propager.
    def prendre_feu(self):
        self.en_feu = True


