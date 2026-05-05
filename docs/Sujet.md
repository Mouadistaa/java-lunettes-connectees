# La fabrique de lunettes

Vous allez devoir développer un système distribué permettant de commander des lunettes connectées, les faire fabriquer et les réceptionner.

## Les composants

Concretement, vous allez devoir développer deux composants :
* une application JavaFX qui doit permettre de passer une commande, de suivre son évolution et de réceptionner la marchandise
* une application « backend » qui doit réceptionner les commandes, organiser la production en fonction des contraintes, et livrer la marchandise produite

La communication entre l'application qui permet de passer commande et l'usine se fera
[par évènement](https://aws.amazon.com/fr/what-is/eda/) donc de manière asychrone.

### L'application JavaFX

L'application sera composée d'au moins 4 écrans :
* un écran d'accueil qui présente l'application
* un écran qui présente la liste des lunettes disponibles, et permet de passer commande d'une ou plusieurs paires
* un écran qui fait patienter pendant la fabrication, et affiche ensuite les numéros de série des lunettes reçus
* un écran qui permet de vérifier un numéro de série

Elle utilisera le bus mqtt pour dialoguer avec le _backend_. On se limitera à une seule commande
en simultané par client, mais plusieurs clients peuvent commander en parallèle.

### Le backend

Le backend doit réceptionner les commandes postées sur le bus mqtt, lancer la fabrication
et livrer les lunettes.

Pour la fabrication, vous avez à votre disposition une librairie java `fabricateur`.
Elle est hébergée sur un dépôt maven privé nécessitant une authentification par login/mdp.

```xml
<!--
url du dépôt : https://maven.pkg.github.com/le-prof-de-raizo/repo-maven
nom d'utilisateur : le-prof-de-raizo
mot de passe : <PAT_REDACTED — voir sujet original sur GitLab univ-lorraine>
-->
<dependency>
  <groupId>bernard-flou</groupId>
  <artifactId>fabricateur</artifactId>
  <version>0.0.1</version>
</dependency>
```

Le processus de fabrication des lunettes est assez contraignant,
lisez bien la documentation de la librairie avant de vous lancer ;-).

Le backend devra être découpé en deux composants : un `serveur` qui gère
l'interaction avec le bus mqtt, et une `usine` qui pilote le `fabricateur`.
Sa configuration doit être externalisée dans un fichier `properties`.
On y trouvera notamment l'url de connexion au _broker_


```
┌─────────┐   ┌─────────┐   ┌─────────────┐
│ serveur ├──►│  usine  ├──►│ fabricateur |
└─────────┘   └─────────┘   └─────────────┘

 ──► dépend de
```

Le point d'entrée de l'usine devra respecter le prototype suivant:

```java
/**
 * Lance la production de lunettes. Chaque entrée de la `Map`
 * associe au type de lunette la quantité qu'il faut en produire.
 */
List<Lunette> produire(final Map<TypeLunette, Integer> typesLunettes);
```

L'`usine` devra être packagées sous la forme d'un `.jar`, et publiée
sur un dépôt maven hébergé sur GitHub. La publication devra se faire
via une [action](https://github.com/features/actions) qui réagit
à la création d'une release.

L'usine **doit pouvoir traiter plusieurs commandes en parallèle**, ce qui veut dire
que la méthode `produire()` doit pouvoir être appelée par plusieurs threads simultanément.

Dans un premier temps, limitez vous à une gestion séquentielle :
* Exemple 1 : je reçois une commande pour 5 lunettes alors que la machine
ne peut en fabriquer que 4 à la fois, je dois lancer deux fabrications
à la suite (une fois pour 4 et une fois pour 1).
* Exemple 2 : je reçois trois commandes de deux lunettes, la machine peut
en fabriquer 4 à la fois, je passe par 3 étapes de fabrication de deux lunettes.

Dans un second temps, afin d’améliorer la productivité, implémentez la mutualisation des commandes :
* Exemple : si je reçois 3 commandes de 2 lunettes alors que la machine
peut en fabriquer 8 en //, je peux fabriquer les 3 commandes en même temps.

### Communication

Vous utiliserez le _broker_ [Mosquitto](https://mosquitto.org) qui implémente
le protocole [MQTT](https://mqtt.org). Au niveau du paramétrage, on restera sur
le port tcp par défaut (1883) sans authentification ni sécurisation tls.

```
      ┌───────────┐                                                   
      │ JavaFX #1 ├─┐                                                 
      └───────────┘ │     ┌────────────────────────┐     ┌─────────┐ 
                    └──► (│)    topic/subtopic    (│)◄───┤  Usine  │ 
 ┌───────────┐ ┌──────►  (│)      + payload       (│)    └─────────┘ 
 │ JavaFX #2 ├─┘          └────────────────────────┘                  
 └───────────┘                    Bus MQTT   
```

Le protocole MQTT implémente le paradigme _[publish/subscribe](https://fr.wikipedia.org/wiki/Publish-subscribe)_.
Un noeud du réseau publie une information, et les noeuds intéressés s'abonnent pour la recevoir.

#### Topics

Cette section décrit les différents évènements nécessaires au projet.
Le sens des évènements est signalé par des flèches, le client étant à gauche, l'usine à droite.
Les éventuelles données associées aux évènements devront être sérialisé dans un format spécifique,
que vous allez devoir concevoir et implémenter (interdiction d'utiliser un format « tout fait » !).
Il faudra expliquer votre format dans la documentation du projet.

Pour passer commande, le client doit envoyer la liste des lunettes à fabriquer avec pour chacune la quantité désirée.
Pour identifier les commandes, vous devrez générer un identifiant qui doit ête unique dans l'univers entier.

| topic        | sens | données                                                 | détails            |
|--------------|:----:|---------------------------------------------------------|--------------------|
| `orders/xxx` |  →   | La commande (liste des lunettes et quantité de chacune) | Passe une commande |

Le 'xxx' dans le nom de topic doit être remplacé par l'identifiant unique. Exemple : « order/42 ».
Il est possible de commander 4 types de lunettes gérées par le fabricateur.

L'usine doit valider la commande avant de répondre :
* le type de lunette doit être connu (voir enum `TypeLunette`)
* la quantité totale à produire doit être strictement supérieure à zéro
* la quantité de chaque type doit être comprise entre 0 (inclu) et 10 (exclu)

En cas de commande invalide, un évènement est envoyé au client. Ce dernier doit mettre fin à la transaction,
aucune autre action ne devra être faite pour cette commande, et aucune autre évènement ne sera envoyé.

| topic                  | sens |      données       | détails                                                    |
|------------------------|:----:|:------------------:|------------------------------------------------------------|
| `orders/xxx/validated` |  ←   |        n/a         | La commande est valide, et sera fabriquée dès que possible |
| `orders/xxx/cancelled` |  ←   | Détail de l'erreur | La commande est incorrecte, et donc annulée                |

La livraison se fait par un dernier message qui mettre fin à la transaction.

| topic                 | sens | données                                                 | détails            |
|-----------------------|:----:|---------------------------------------------------------|--------------------|
| `orders/xxx/delivery` |  ←   | La liste des lunettes produites (type + numéro de série | Fin de la commande |

A tout moment, si une erreur survient pendant le traitment de la commande,
il faut en avertir le client et mettre fin à la transaction.

| topic              | sens | données                    | détails |
|--------------------|:----:|----------------------------|---------|
| `orders/xxx/error` |  ←   | La description de l'erreur |         |

La validité d'un numéro de série peut être établie en utilisant un topic spécifique :

| topic               | sens |             données             | détails |
|---------------------|:----:|:-------------------------------:|---------|
| `serials/xxx/check` |  →   |               n/a               |         |
| `serials/xxx`       |  ←   | Le type de lunette ou "invalid" |         |

**Bonus** : il peut être agréable pour les clients de pouvoir suivre la progression
de la commande. Vous pouvez implémenter cette focntionnalité grâce au sous-topic `/status`.

| topic               | sens |             données              | détails                     |
|---------------------|:----:|:--------------------------------:|-----------------------------|
| `orders/xxx/status` |  ←   | Le nouveau statut de la commande | Liste des statut ci-dessous |

Statut possibles de la commande :
* `processing` : la fabrication a démarré
* `processed` : la fabrication est terminée

## Ce qu'il faut produire

A la fin du projet, vous devrez me fournir :

* un `.jar` _prêt à l'emploi_ contenant l'application graphique qui permet de passer commande
* un `.jar` _prêt à l'emploi_ qui gère la production de lunettes (backend)
* un `README.md` expliquant comment utiliser ces livrables
* un PDF d'une quarantaine de pages expliquant l'architecture de votre projet,
ses spécificités, ce qui a été implémenté et comment, ce qui manque,
la répartition du travail, l'organisation des sources, ...
* tout autre ressource que vous jugerez pertinente (TU, TI, CI/CD, ...)

Vous devrez également me donner accès aux sources des projets (@le-prof-de-raizo).
Je dois pouvoir créer une _release_, ce qui doit provoquer la publication de votre `usine`
via un workflow automatisé.

⚠️ Votre projet ne devra pas utiliser de _frameworks_ « magiques » type [Spring](https://spring.io).
Si vous n'êtes pas sûr, demandez moi !

⚠️ Votre projet doit être **_production ready_**, c'est-à-dire pouvoir être lancé facilement,
être opérationnel, gérer les erreurs et produire des logs pertinents (ni trop, ni trop peu).
Les paramètres de configuration (adresse du _broker_, ...) doivent être lus depuis un fichier
(qui peut être embarqué dans le jar pour simplifier son utilisation).

Concernant les erreurs, il faudra notamment gérer correctement la connexion au bus,
la validation ou non de la commande ainsi que les erreurs qui peuvent survenir
lors de la fabrication des lunettes.

**Votre dossier devra également répondre de manière détaillée aux questions suivantes** :
* comment gérer l'absence d'usine connectée au bus pour produire les lunettes ?
* que faut-il modifier pour gérer plusieurs commandes en parallèle pour un même client ?
* que faut-il modifier pour pouvoir gérer plusieurs usines ?

## Par où commencer ?

Si vous ne savez pas comment démarrer, voici quelques pistes :
* se documenter sur le paradigme _publisher_/_subscriber_
* se documenter sur mqtt, l'installer, tester l'envoi et la réception d'un message
* réfléchir à la « bonne » manière d'implémenter le protocole décrit
  dans cette documentation ([ping-pong](https://en.m.wikipedia.org/wiki/Ping-pong_scheme))
* implémenter l'usine, la tester avec un `main`, puis le serveur, le tester avec un client "en dur"
* développer le client javafx (peut se faire en parallèle du point précédent)
* travaillez de manière incrémentielle
