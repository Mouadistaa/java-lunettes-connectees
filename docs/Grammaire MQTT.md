# Grammaire du format MQTT

Spécification du format de sérialisation utilisé pour les payloads MQTT du projet **Lunettes connectées**.

Le sujet du projet interdit l'utilisation d'un format de sérialisation prêt à l'emploi (JSON, protobuf, MessagePack, etc.). Ce document décrit le format ad-hoc que nous avons conçu pour répondre à cette contrainte tout en restant lisible humainement et trivial à parser.

## Principes de conception

Quatre principes ont guidé la conception du format :

1. **Lisibilité humaine.** Un développeur qui debugue avec un client MQTT générique (`mosquitto_sub`, MQTT Explorer) doit pouvoir lire les payloads à l'œil nu sans outil de décodage.
2. **Parsing trivial.** Le format doit pouvoir être parsé avec uniquement les outils du JDK standard (`String.split`, `String.join`, `Integer.parseInt`), sans librairie tierce.
3. **Pas de cérémonie inutile.** Le contexte sémantique du message (commande, livraison, erreur) est porté par le **topic MQTT** sur lequel il est publié, jamais redondé dans le payload.
4. **Garanties du domaine exploitées.** Les valeurs métier que nous transportons (enums, entiers, UUID, numéros de série) ne contiennent par construction ni le séparateur de champ ni le séparateur de ligne. Cette garantie nous permet de renoncer au quoting et à l'échappement.

## Règles générales

| Règle                | Valeur                  |
|----------------------|-------------------------|
| Encodage             | UTF-8                   |
| Séparateur de champs | `;` (point-virgule)     |
| Séparateur de lignes | `\n` (LF, pas CRLF)     |
| Quoting              | Aucun                   |
| Échappement          | Aucun                   |
| Header               | Aucun                   |
| Schéma               | Variable selon le topic |

Le payload est transporté sur MQTT comme un `byte[]`. La conversion `String` <-> `byte[]` se fait toujours en UTF-8 explicitement, jamais via l'encodage par défaut de la JVM.

Un payload peut être **vide** (`byte[0]`) : c'est le cas pour les notifications qui ne portent pas de donnée propre, l'information étant entièrement contenue dans le topic.

## Catalogue des topics et payloads

Le tableau suivant liste tous les couples (topic, payload) du protocole.

| Topic                     | Sens            | Payload                                         |
|---------------------------|-----------------|-------------------------------------------------|
| `orders/{uuid}`           | client -> usine | une ligne par type : `<TYPE>;<QUANTITE>`        |
| `orders/{uuid}/validated` | usine -> client | (vide)                                          |
| `orders/{uuid}/cancelled` | usine -> client | une ligne : message d'erreur                    |
| `orders/{uuid}/status`    | usine -> client | une ligne : `processing` ou `processed`         |
| `orders/{uuid}/delivery`  | usine -> client | une ligne par lunette : `<TYPE>;<NUMERO_SERIE>` |
| `orders/{uuid}/error`     | usine -> client | une ligne : message d'erreur                    |
| `serials/{numero}/check`  | client -> usine | (vide)                                          |
| `serials/{numero}`        | usine -> client | une ligne : `<TYPE>` ou `INVALID`               |

Le segment `{uuid}` dans le topic est un identifiant de commande au format UUID v4 (généré côté client par `UUID.randomUUID()`). Le segment `{numero}` est un numéro de série tel que produit par le fabricateur.

## Grammaire formelle

### Payload `Commande`

Topic : `orders/{uuid}`

```
payload   = ligne ( "\n" ligne )*
ligne     = type ";" quantite
type      = "BANANA" | "CHATGPT" | "LE_CHAT" | "CLAUDE"
quantite  = entier dans [0, 10[
```

Règles supplémentaires :

- Au moins une ligne avec une quantité strictement positive est requise (un payload vide ou ne contenant que des quantités à zéro est rejeté à la désérialisation).
- Un type ne peut apparaître **qu'une seule fois** dans un payload (la duplication est rejetée).
- Les types absents du payload sont assimilés à une quantité de zéro (le serializer omet les lignes à zéro pour éviter le bruit, le deserializer accepte un payload qui ne mentionne pas tous les types).

### Payload `Livraison`

Topic : `orders/{uuid}/delivery`

```
payload      = ligne ( "\n" ligne )*
ligne        = type ";" numero_serie
type         = "BANANA" | "CHATGPT" | "LE_CHAT" | "CLAUDE"
numero_serie = chaîne non vide, format imposé par le fabricateur
```

Le format du numéro de série est de la forme `XX-NNNNN-CCCCCCCC` (deux premières lettres du type, identifiant pseudo-aléatoire en base 36, somme de contrôle CRC32 en hexadécimal). La validité formelle d'un numéro de série n'est vérifiable qu'en interrogeant le fabricateur lui-même via sa méthode `validateSerial`.

Le payload n'est jamais vide : une livraison contient au moins une lunette.

### Payloads texte mono-ligne

Topics concernés : `orders/{uuid}/cancelled`, `orders/{uuid}/error`, `orders/{uuid}/status`, `serials/{numero}`.

```
payload = chaîne UTF-8 sans saut de ligne
```

Pour `orders/{uuid}/status`, les seules valeurs autorisées sont `processing` et `processed`.

Pour `serials/{numero}`, le payload est soit le nom d'un type de lunette (`BANANA`, `CHATGPT`, `LE_CHAT`, `CLAUDE`), soit la chaîne littérale `INVALID` si le numéro de série n'est pas reconnu par le fabricateur.

Pour `orders/{uuid}/cancelled` et `orders/{uuid}/error`, le payload est un message d'erreur libre destiné à l'affichage utilisateur. Il ne doit pas contenir de saut de ligne.

### Payloads vides

Topics concernés : `orders/{uuid}/validated`, `serials/{numero}/check`.

```
payload = byte[0]
```

L'information est entièrement portée par le topic.

## Exemples

### Commande de 3 BANANA et 1 CLAUDE

Topic : `orders/a3c5e4f1-2b9d-4f6a-9c1e-7d2b8a5f3e4d`

```
BANANA;3
CLAUDE;1
```

### Livraison correspondante

Topic : `orders/a3c5e4f1-2b9d-4f6a-9c1e-7d2b8a5f3e4d/delivery`

```
BANANA;BA-5XK2J-A1B2C3D4
BANANA;BA-9PQ1L-77AB22FF
BANANA;BA-3MNXT-EE001122
CLAUDE;CL-Z8K2P-DEAD1234
```

### Vérification d'un numéro de série

Le client publie sur `serials/BA-5XK2J-A1B2C3D4/check` un payload vide.

L'usine répond sur `serials/BA-5XK2J-A1B2C3D4` :

```
BANANA
```

Si le numéro de série est inconnu :

```
INVALID
```

### Notification de statut

Sur `orders/a3c5e4f1-2b9d-4f6a-9c1e-7d2b8a5f3e4d/status` :

```
processing
```

Puis, une fois la fabrication terminée :

```
processed
```

### Erreur métier

Sur `orders/a3c5e4f1-2b9d-4f6a-9c1e-7d2b8a5f3e4d/cancelled` :

```
quantité totale doit être strictement positive
```

## Distinction avec CSV

Bien que notre format utilise un séparateur en `;` qu'on retrouve dans certaines variantes de CSV, il s'en distingue sur trois points structurels :

1. **Pas de quoting ni d'échappement.** CSV (RFC 4180) gère le quoting des champs avec guillemets doubles et l'échappement des guillemets internes par doublage. Notre format ne fait rien de tout ça, parce que nos valeurs métier ne peuvent pas contenir les délimiteurs.
2. **Pas de schéma fixe.** Un fichier CSV a une structure tabulaire homogène (mêmes colonnes du début à la fin). Notre format change de grammaire selon le topic (`<TYPE>;<QUANTITE>` pour une commande, `<TYPE>;<NUMERO_SERIE>` pour une livraison) et certains payloads sont mono-ligne quand d'autres sont multi-lignes.
3. **Pas de header.** CSV avec header est auto-descriptif. Notre format délègue la sémantique au topic MQTT, ce qui ne fait pas partie du format CSV.

En pratique, un parseur CSV générique ne fonctionnerait pas correctement sur nos payloads, et un parseur de notre format ne fonctionnerait pas sur du CSV qui contient des champs quotés.

## Implémentation

L'implémentation Java vit dans le module `lunettes-events` :

- `MqttFormat` : constantes (séparateurs, charset)
- `Serializer` : conversion DTOs -> `byte[]`
- `Deserializer` : conversion `byte[]` -> DTOs
- `MalformedPayloadException` : signalement des payloads invalides

Le code source est documenté en Javadoc. Les tests unitaires de round-trip et de gestion d'erreur se trouvent dans `lunettes-events/src/test/java`.
