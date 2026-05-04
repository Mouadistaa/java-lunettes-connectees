# Projet Java — Lunettes connectées

Ce projet a été réalisé dans le cadre du module de Java Programmation Avancée du Master 1 MIAGE.  
Il consiste à développer un système simulant la commande et la fabrication de lunettes connectées.  
L'application repose sur plusieurs composants Java qui communiquent entre eux via un broker MQTT Mosquitto.  
Le projet est organisé autour d'un frontend JavaFX, d'un serveur Java, d'une usine Java et d'un environnement Docker pour faciliter les tests locaux.

## Architecture

Le système repose sur une architecture distribuée basée sur MQTT.

```text
+------------------+        +---------------------+        +------------------+        +------------------+
| Frontend JavaFX  | -----> | Broker MQTT         | -----> | Serveur Java     | -----> | Usine Java       |
| Client           |        | Mosquitto           |        | Gestion commandes|        | Fabrication      |
+------------------+        +---------------------+        +------------------+        +------------------+
                                                                                                 |
                                                                                                 v
                                                                                         +------------------+
                                                                                         | Fabricateur      |
                                                                                         | JAR professeur   |
                                                                                         +------------------+
```

Les applications JavaFX permettent aux clients de passer des commandes de lunettes connectées.  
Le broker MQTT Mosquitto sert d'intermédiaire de communication entre les clients et le backend.  
Le serveur Java reçoit les commandes depuis MQTT, les valide et les transmet à l'usine.  
L'usine Java pilote la fabrication des lunettes en s'appuyant sur la librairie `fabricateur` fournie par le professeur.

## Pré-requis

Avant de lancer le projet, les outils suivants doivent être installés sur la machine :

- [Java 21](https://www.oracle.com/fr/java/technologies/downloads/) ou une distribution OpenJDK compatible Java 21 ;
- [Apache Maven 3.8+](https://maven.apache.org/download.cgi) ;
- [Docker Desktop](https://docs.docker.com/desktop/) ;
- [Git](https://git-scm.com/install/).

Pour vérifier l'installation des outils principaux, utiliser les commandes suivantes :

```bash
java --version
mvn --version
docker --version
git --version
```

Docker doit également être démarré avant de lancer le broker MQTT Mosquitto.

## Configuration Maven pour accéder au fabricateur

Le projet utilise la librairie `bernard-flou:fabricateur:0.0.1`, fournie par le professeur via GitHub Packages.

Pour que Maven puisse télécharger cette dépendance, il faut configurer le fichier `settings.xml` dans le dossier local Maven :

```text
~/.m2/settings.xml
```

Sous Windows, ce fichier se trouve généralement ici :

```text
C:\Users\<votre-utilisateur>\.m2\settings.xml
```

Si le fichier n'existe pas, il faut le créer.

Exemple de contenu :

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>VOTRE_IDENTIFIANT_GITHUB</username>
      <password>VOTRE_PAT_ICI</password>
    </server>
  </servers>
</settings>
```

Le champ `<password>` doit contenir un Personal Access Token GitHub, et non le mot de passe du compte GitHub.

Ne mettez jamais le PAT directement dans ce README ou dans le dépôt Git.

Récupérez le PAT depuis le sujet du projet, section **Le backend**.

## Construction du projet

Depuis la racine du dépôt, le projet se construit avec Maven :

```bash
mvn clean install
```

Cette commande exécute plusieurs étapes :

- `clean` supprime les anciens fichiers générés dans les dossiers `target/` ;
- Maven compile les différents modules du projet ;
- Maven exécute les tests disponibles ;
- Maven installe les artefacts générés dans le dépôt Maven local de la machine.

Cette commande doit être lancée depuis la racine du projet, au même niveau que le fichier `pom.xml` principal.

## Lancement du système

Le système doit être lancé dans plusieurs terminaux séparés.

### Terminal 1 — Broker MQTT Mosquitto

Depuis la racine du projet :

```bash
docker compose up
```

ou en arrière-plan :

```bash
docker compose up -d
```

Pour arrêter le broker :

```bash
docker compose down
```

### Terminal 2 — Serveur Java

Après construction du projet avec `mvn clean install`, lancer le serveur avec :

```bash
java -jar lunettes-serveur/target/lunettes-serveur-1.0-SNAPSHOT.jar
```

Le nom exact du fichier `.jar` pourra être ajusté lorsque le module `lunettes-serveur` sera finalisé.

### Terminal 3 — Frontend JavaFX

Après construction du projet avec `mvn clean install`, lancer l'application graphique avec :

```bash
java -jar lunettes-frontend/target/lunettes-frontend-1.0-SNAPSHOT.jar
```

Le nom exact du fichier `.jar` pourra être ajusté lorsque le module `lunettes-frontend` sera finalisé.

## Structure du dépôt

Le projet est organisé en plusieurs modules Maven :

```text
java-lunettes-connectees/
├── lunettes-events/      DTOs partagés, topics MQTT et sérialisation des messages
├── lunettes-usine/       Wrapper autour du fabricateur fourni par le professeur
├── lunettes-serveur/     Serveur Java qui reçoit les commandes MQTT et pilote l'usine
├── lunettes-frontend/    Application JavaFX utilisée par les clients
├── mosquitto/            Configuration du broker MQTT Mosquitto
├── docker-compose.yml    Lancement local du broker MQTT
├── pom.xml               POM Maven parent du projet multi-module
└── README.md             Documentation d'installation et de lancement
```

## Tests

Pour lancer les tests de tous les modules, utiliser la commande suivante depuis la racine du projet :

```bash
mvn test
```

Cette commande compile le projet si nécessaire, puis exécute les tests unitaires présents dans les modules Maven.

## Liens utiles

- Sujet du projet : 
- Guide d'équipe : 
- Documentation PDF : 

## Auteurs

Projet réalisé par :

FUMERON–LECOMTE Baptiste
KACHLER Théo
SAHRAOUI DOUKKALI Mouad
EL AOUDI Rim
