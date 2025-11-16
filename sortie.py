#Stéphanie Sbircea


class Sortie:
    def __init__(self, x, y):
        self.x = x
        self.y = y

# Les sorties permettent aux individus de s'évacuer du bâtiment.
    def permettre_evacuation(self): pass

    #Les sorties agissent comme des "portes coupe-feu" et ne permettent pas au feu de les traverser
    def bloquer_feu(self): pass
