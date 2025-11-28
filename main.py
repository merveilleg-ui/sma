# main.py
#Merveille
from individu import Individu, AgentThread
from feu import Feu, FireThread
from mur import Mur
from objet import Objet
from sortie import Sortie
from environnement import Environnement

import csv
import time
import socket
import struct
import os
#helloooooo
# ---------------------------
# Simulation / I/O utilities
# ---------------------------

def simulation(fichier_csv="environnement.csv"):
    print("simulation")
    grille = []
    with open(fichier_csv, newline='') as f:
        reader = csv.reader(f, delimiter=',')
        for ligne in reader:
            grille.append(list(ligne))

    compteur_individus = 1 # <-- AJOUTER CETTE LIGNE

    env = Environnement(largeur=len(grille[0]), hauteur=len(grille))

    individus_crees = {}

    for y, ligne in enumerate(grille):
        for x, case in enumerate(ligne):
            if case == "I":
                # Plus besoin du 'if case not in individus_crees:'
                id_unique = f"I{compteur_individus}" # Créer un ID unique (I1, I2, etc.)
                
                # Créer le nouvel individu avec son ID unique
                individu = Individu(x, y, environnement=env, id_individu=id_unique)
                env.personnes.append(individu)
                env.ajouter_agent(individu)
                
                compteur_individus += 1 # Incrémenter pour le prochain individu
            elif case == "M":
                mur = Mur(x, y)
                env.murs.append(mur)
                env.ajouter_agent(mur)
            elif case == "O":
                objet = Objet(x, y)
                env.objets.append(objet)
                env.ajouter_agent(objet)
            elif case == "F":
                feu = Feu(x, y, env)
                env.feux.append(feu)
                env.ajouter_agent(feu)
            elif case == "S":
                env.sorties.append(Sortie(x, y))

    # résumé
    print("\n--- Résumé des objets détectés ---")
    print(f"{len(env.personnes)} individus trouvés")
    print(f"{len(env.murs)} murs trouvés")
    print(f"{len(env.objets)} objets trouvés")
    print(f"{len(env.feux)} feux trouvés")
    print(f"{len(env.sorties)} sorties trouvées")

    return env


def generer_grille_etat(env):
    """
    Crée une matrice de caractères correspondant à l'état actuel de l'environnement.
    Utilise env.lock pour éviter les lectures pendant des écritures concurrentes.
    """
    with env.lock:
        grille_chars = [["." for _ in range(env.largeur)] for _ in range(env.hauteur)]

        for m in env.murs:
            grille_chars[m.y][m.x] = "M"
        for o in env.objets:
            grille_chars[o.y][o.x] = "O"
        for s in env.sorties:
            grille_chars[s.y][s.x] = "S"
        for f in env.feux:
            grille_chars[f.y][f.x] = "F"

        for p in env.personnes:
            id_court = "I"
            for (case_x, case_y) in p.cases_occupees:
                if 0 <= case_y < env.hauteur and 0 <= case_x < env.largeur:
                    grille_chars[case_y][case_x] = id_court

    return grille_chars


def preparer_donnees_socket(grille_chars):
    """
    Convertit la grille en bytes (suite de codes ASCII des caractères)
    Le client Java devra reconstruire la matrice avec largeur/hauteur reçues en header.
    """
    byte_data = bytearray()
    for ligne in grille_chars:
        for char in ligne:
            byte_data.append(ord(char))
    return bytes(byte_data)


def afficher_grille_depuis_etat(grille_chars):
    for ligne in grille_chars:
        print(" ".join(ligne))
    print("\n" * 2)


# ---------------------------
# Main: lancement et serveur
# ---------------------------

HOST = '127.0.0.1'
PORT = 30000

def main():
    print("Lancement de la simulation...")
    env = simulation()

    # Lancer thread de propagation du feu
    fire_thread = FireThread(env)
    fire_thread.start()

    # Lancer threads Agents (daemon)
    threads = [AgentThread(ind) for ind in env.personnes]
    for t in threads:
        t.start()

    conn = None
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind((HOST, PORT))
            s.listen()
            print(f"[SERVEUR] En attente d'une connexion Java sur {HOST}:{PORT}...")
            conn, addr = s.accept()
            with conn:
                print(f"[SERVEUR] Client Java connecté: {addr}")

                # Envoi header (largeur, hauteur) une seule fois
                header = struct.pack('!ii', env.largeur, env.hauteur)
                conn.sendall(header)
                print(f"[SERVEUR] Dimensions ({env.largeur}x{env.hauteur}) envoyées.")

                # Boucle de simulation - chaque étape attend que tous les agents aient fait >= 1 action
                for step in range(50):
                    # Si plus personne -> on arrête
                    if len(env.personnes) == 0:
                        print("[SIM] Plus aucun individu : fin de la simulation.")
                        break

                    # --- Attente : tous les agents doivent avoir fait au moins une action ---
                    # Remarque : all([]) -> True, donc la boucle sort si env.personnes est vide.
                    waited = 0.0
                    while True:
                        # Copie rapide de la liste pour éviter race sur longueur ; lecture seule donc ok
                        personnes_snapshot = list(env.personnes)
                        if all(p.a_fait_action for p in personnes_snapshot):
                            break
                        time.sleep(0.02)
                        waited += 0.02
                        # Optionnel : garder un timeout pour éviter blocage infini (ici 10s)
                        if waited > 10.0:
                            print("[WARN] Timeout d'attente des agents (on avance quand même).")
                            break

                    # --- Générer état thread-safe ---
                    grille_etat = generer_grille_etat(env)

                    # --- Affichage local (debug) ---
                    print(f"Étape {step + 1}")
                    afficher_grille_depuis_etat(grille_etat)

                    # --- Préparer/envoi via socket ---
                    data_bytes = preparer_donnees_socket(grille_etat)
                    try:
                        conn.sendall(data_bytes)
                    except (BrokenPipeError, ConnectionResetError):
                        print("[SERVEUR] Connexion rompue lors de l'envoi.")
                        raise

                    # --- Réinitialiser les flags pour le tour suivant ---
                    with env.lock:
                        for p in list(env.personnes):
                            # si l'individu a été supprimé pendant l'envoi, on l'ignore
                            if hasattr(p, "a_fait_action"):
                                p.a_fait_action = False

                    # Pause pour rythmer la simulation (et laisser les threads bosser)
                    time.sleep(0.05)

                    # Vérification de terminaison (au cas où tout part en vrac)
                    if len(env.personnes) == 0:
                        print("[SIM] Plus aucun individu : fin de la simulation.")
                        break

    except (BrokenPipeError, ConnectionResetError):
        print("[SERVEUR] Le client Java s'est déconnecté.")
    except KeyboardInterrupt:
        print("[SERVEUR] Arrêt manuel.")
    finally:
        # fermer la connexion si elle existe
        try:
            if conn:
                conn.close()
        except Exception:
            pass

        # Arrêt propre des threads de feu
        try:
            fire_thread.running = False
            fire_thread.join(timeout=2.0)
        except Exception:
            pass

        print("[SERVEUR] Serveur arrêté. Simulation terminée.")


if __name__ == "__main__":

    main()
