# NyxonStore | Advanced Rotating Shop

**Version:** 1.1.2 · **Language:** [English](#-english) · [Italiano](#-italiano)

Take your server economy to the next level with a dynamic shop system based on
**weighted random rotations**. Limited-time items, hidden items to discover,
VIP shops, multi-pool support, per-item currency, command-based items and a full in-game editor.

---

## 🇬🇧 English

### ✨ Key Features

- **Rotating GUI Shop** — random item selection based on a weight system on every rotation.
- **Multi-Pool** — create multiple shops, each in its own file, opened with `/ns open <poolname>`.
- **In-game Admin Menu** — create, edit and delete pools and items live, with no file editing. The store updates instantly.
- **Per-item currency** — set a different CoinsEngine currency for each item; falls back to the global default if not specified.
- **Command items** (`type: command`) — items that run console commands on purchase instead of giving a physical item. Supports `{player}` placeholder and a custom display material.
- **Purchase confirmation menu** — a configurable confirm/cancel screen after clicking an item, showing it with its exact store lore.
- **Hidden items** — items masked until a player clicks to reveal them (with a reveal sound).
- **VIP items / per-item permissions** — gate items behind a permission to build exclusive VIP shops.
- **Max purchases per item** — set a limit per rotation, with an anti-race check: two players can't exceed the limit even with the GUI open at the same time.
- **MMOItems & ItemsAdder integration** — sell advanced custom items alongside vanilla ones.
- **Built-in PlaceholderAPI** — `%nyxonstore_...%` placeholders (rotation timer, item name/price/purchases/stock) to use anywhere.
- **Fully configurable UI** — GUI size (27/36/45/54), per-slot layout, decorative blocks, sounds and messages.
- **Configurable broadcasts** — toggle rotation and warning messages on/off from the config.

### 🎮 Commands & Permissions

**Player commands**

    /ns   or   /ns open           Open the default store
    /ns open [pool]               Open a specific store

**Admin commands**

    /ns open <pool> <player>      Open a store for a specific player
    /ns admin edit [pool]         Open the in-game editor (pool optional)
    /ns admin reload              Reload all configuration
    /ns admin forcerotate [pool]  Force an immediate pool rotation

**Permissions**

    nyxonstore.open     (default: everyone)   Allows opening the store
    nyxonstore.vip      (default: op)         Allows seeing and buying VIP items
    nyxonstore.admin    (default: op)         Editor, reload, forcerotate and bypass
                                      hidden/VIP masking (preview mode)

### ⚙️ Configuration

All files are generated in `plugins/NyxonStore/` on first run.
Run `/ns admin reload` to apply changes without restarting.

#### config.yml

Global settings: rotation timer, currency, broadcasts, sounds, and the shared
hidden / VIP / sold-out icons.

```yaml
prefix: "<gray>[<gold>NyxonStore</gold>]</gray>"
rotation: "1d 0h 0m"          # supports d / h / m   e.g.  "12h 30m"
currency: "gold"              # CoinsEngine currency id (used as global default)
default-pool: "default"

broadcast:
  enabled: true               # master switch
  rotation: true              # announce when a pool rotates
  warnings: true              # announce the timed warnings

# Icon shown for hidden items a player has not revealed yet.
# For a head: material "PLAYER_HEAD" + texture (base64) OR owner.
hidden-item:
  material: "BARRIER"
  texture: ""
  owner: ""
  name: "<gray>Secret item</gray>"
  lore:
    - "<dark_gray>???"

# Icon shown for VIP items to players without nyxonstore.vip.
vip-item:
  material: "BARRIER"
  name: "<gold>VIP item</gold>"
  lore:
    - "<gray>Unlock this with <gold>VIP</gold>"

# Icon shown when an item reaches its max purchases for the rotation.
sold-out-item:
  material: "BARRIER"
  name: "<red>Sold out</red>"
  lore:
    - "<gray>This item is sold out"

sounds:
  gui-open:      { sound: "BLOCK_CHEST_OPEN",             volume: 1.0, pitch: 1.0 }
  purchase:      { sound: "ENTITY_PLAYER_LEVELUP",        volume: 1.0, pitch: 1.0 }
  force-rotate:  { sound: "BLOCK_NOTE_BLOCK_PLING",       volume: 1.0, pitch: 1.0 }
  hidden-reveal: { sound: "ENTITY_EXPERIENCE_ORB_PICKUP", volume: 1.0, pitch: 1.2 }
```

#### gui.yml

Controls the store window. `pool`-type slots are where rotated items appear
(the number of `pool` slots = how many items each rotation shows). Any other
slot is a decorative `filler`.

```yaml
title: "<b><color:#2B2B2B>nyxon store</color></b>"
size: 54                      # 27 / 36 / 45 / 54
lore:
  price: "<gold>Price: </gold><yellow>{price} {currency}</yellow>"
  purchase-hint: "<gray>Click to <green>purchase</green>!</gray>"
slots:
  21: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  22: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  23: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  0:  { type: "filler", material: "BLACK_STAINED_GLASS_PANE", name: " " }
```

#### confirmation.yml

The purchase confirmation menu. Every entry supports heads
(`material: "PLAYER_HEAD"` + `texture` or `owner`).

```yaml
enabled: true
title: "<gold>Confirm Purchase</gold>"
size: 27

confirm: { slot: 11, material: "LIME_WOOL", name: "<green>Confirm</green>" }
cancel:  { slot: 15, material: "RED_WOOL",  name: "<red>Cancel</red>"  }
info:    { slot: 13 }                          # real item, exact store lore
filler:  { material: "GRAY_STAINED_GLASS_PANE", name: " " }
slots: {}                                      # optional extra decorative slots
```

### 📦 Pool Configuration

Each file in `pools/` is an independent store; the file name is the pool name.
Open it with `/ns open <name>`.

**MMOItems Example (per-item `currency` override)**

```yaml
vip_sword:
  type: "mmoitems"
  mmoitems_type: "SWORD"
  mmoitems_id: "EPIC_SWORD"
  currency: "gems"                # ← pays with "gems" instead of "gold"
  price: 500
  weight: 50
  hidden: true
  max-purchases: 1
```

**Command Item Example (`type: command`)**

```yaml
daily_kits:
  type: "command"
  display-material: "NETHER_STAR" # item shown in the GUI
  display-name: "<gold>Daily Kit"
  commands:
    - "kit daily {player}"        # runs as CONSOLE, {player} = buyer name
    - "eco give {player} 100"
  currency: "tokens"              # ← pays with a custom currency
  price: 25
  weight: 20
  max-purchases: 1
```

**ItemsAdder Example**

```yaml
item2:
  type: "itemsadder"
  itemsadder-id: "namespace:my_custom_item"
  price: 300
  weight: 30
```

**Vanilla Item Example**

```yaml
diamond_pack:
  type: "vanilla"
  material: "DIAMOND"
  display-name: "<aqua>Diamond Pack</aqua>"
  amount: 16
  price: 100
  weight: 80
  vip: true
```

**Field Reference**

    type             Item type: "vanilla" / "mmoitems" / "itemsadder" / "command"
    material         Vanilla material                           (vanilla only)
    mmoitems_type    MMOItem type                               (mmoitems only)
    mmoitems_id      MMOItem id                                 (mmoitems only)
    itemsadder-id    ItemsAdder id (namespace:item)             (itemsadder only)
    display-material Material displayed in GUI                  (command only; default COMMAND_BLOCK)
    commands         List of console commands to run            (command only)
                     Use {player} as the buyer placeholder
    display-name     Custom MiniMessage name                    (optional)
    amount           Item amount, default 1                     (vanilla / command display)
    currency         CoinsEngine id to charge for this item     (optional)
                     Falls back to config.yml "currency" if omitted
    price            Item price
    weight           Selection weight (higher = more common)
    vip              If true, requires nyxonstore.vip to see/buy it
    hidden           If true, stays masked until clicked to reveal
    max-purchases    Buy limit per rotation; -1 = infinite
    slot             Fixed GUI slot, or -1 for auto-placement

### 🖼️ Player Heads

Any configurable icon (`hidden-item`, `vip-item`, `sold-out-item`, and every
`confirmation.yml` entry) can be a head:

```yaml
material: "PLAYER_HEAD"
texture: "<base64>"     # the "Value" field from e.g. minecraft-heads.com
# or
owner: "PlayerName"     # uses a real player's current skin
```

`texture` and `owner` are ignored if `material` is not `PLAYER_HEAD`.

### 🔗 PlaceholderAPI

Expansion identifier: `nyxonstore` (requires PlaceholderAPI).

    %nyxonstore_default_pool%                        Default pool name
    %nyxonstore_<pool>_rotation_time%                Time until rotation (Xd Xh Xm)
    %nyxonstore_<pool>_rotation_time_seconds%        Seconds until rotation
    %nyxonstore_<pool>_rotation_time_formatted%      Readable countdown
    %nyxonstore_<pool>_item_<i>_name%                Item name/material at index i
    %nyxonstore_<pool>_item_<i>_price%               Item price
    %nyxonstore_<pool>_item_<i>_purchases%           Purchases this rotation
    %nyxonstore_<pool>_item_<i>_max%                 Max purchases
    %nyxonstore_<pool>_item_<i>_soldout%             yes / no
    %nyxonstore_<pool>_item_<i>_type%                Item type

### 📥 Installation

1. Make sure **CoinsEngine** and **PlaceholderAPI** are installed on the server.
2. *(Optional)* Install **MMOItems** and/or **ItemsAdder** to sell custom items.
3. Drop `NyxonStore.jar` into your `/plugins` folder.
4. Restart the server (requires Paper 1.19+ and Java 17+).
5. Configure your pools in the files generated in `plugins/NyxonStore/`.
6. Run `/ns admin reload` after edits, or `/ns admin forcerotate` to test a rotation right away!

> Economy note: purchases charge the player via `<currency> take <player> <price>`
> and the balance is read through PlaceholderAPI. The system is built around
> **CoinsEngine** — set `currency` in config.yml to your global CoinsEngine currency id,
> or override it per-item with `currency:` in the pool YAML.

---

## 🇮🇹 Italiano

### ✨ Features Principali

- **Rotating GUI Shop** — selezione casuale degli oggetti basata su un sistema di pesi (weight) a ogni rotazione.
- **Multi-Pool** — crea più shop, ognuno nel suo file, e aprili con `/ns open <poolname>`.
- **Admin Menu in-game** — crea, modifica ed elimina pool e item live, senza toccare i file. Lo shop si aggiorna all'istante.
- **Valuta per ogni item** — imposta una valuta CoinsEngine diversa per ogni oggetto; se non specificata usa il default globale.
- **Oggetti a comando** (`type: command`) — oggetti che eseguono comandi in console all'acquisto invece di dare un item fisico. Supporta il placeholder `{player}` e un materiale di visualizzazione personalizzato.
- **Menu di conferma acquisto** — schermata confirm/cancel configurabile dopo il click, che mostra l'oggetto con la sua lore identica allo shop.
- **Item nascosti** — oggetti mascherati finché il giocatore non li clicca per rivelarli (con suono di reveal).
- **Item VIP / permessi per item** — blocca gli oggetti dietro un permesso per creare shop esclusivi VIP.
- **Limite di acquisti per item** — imposta un massimo per rotazione, con un controllo anti-race: due giocatori non possono superare il limite anche con la GUI aperta contemporaneamente.
- **Integrazione MMOItems & ItemsAdder** — vendi oggetti custom avanzati insieme a quelli vanilla.
- **PlaceholderAPI integrato** — placeholder `%nyxonstore_...%` (timer rotazione, nome/prezzo/acquisti/stock degli item) da usare ovunque.
- **UI totalmente configurabile** — dimensione GUI (27/36/45/54), layout per singolo slot, blocchi decorativi, suoni e messaggi.
- **Broadcast configurabili** — attiva/disattiva i messaggi di rotazione e gli avvisi dal config.

### 🎮 Comandi & Permessi

**Comandi Giocatori**

    /ns   oppure   /ns open        Apre lo shop di default
    /ns open [pool]                Apre uno shop specifico

**Comandi Admin**

    /ns open <pool> <player>       Apre uno shop per un giocatore specifico
    /ns admin edit [pool]          Apre l'editor in-game (pool opzionale)
    /ns admin reload               Ricarica tutta la configurazione
    /ns admin forcerotate [pool]   Forza subito la rotazione del pool

**Permessi**

    nyxonstore.open     (default: tutti)   Permette di aprire lo shop
    nyxonstore.vip      (default: op)      Permette di vedere e comprare gli item VIP
    nyxonstore.admin    (default: op)      Editor, reload, forcerotate e bypass del
                                   mascheramento hidden/VIP (modalità preview)

### ⚙️ Configurazione

Tutti i file vengono generati in `plugins/NyxonStore/` al primo avvio.
Usa `/ns admin reload` per applicare le modifiche senza riavviare.

#### config.yml

Impostazioni globali: timer di rotazione, valuta, broadcast, suoni e le icone
condivise per item nascosti / VIP / esauriti.

```yaml
prefix: "<gray>[<gold>NyxonStore</gold>]</gray>"
rotation: "1d 0h 0m"          # supporta d / h / m   es.  "12h 30m"
currency: "gold"              # id valuta CoinsEngine (default globale)
default-pool: "default"

broadcast:
  enabled: true               # interruttore generale
  rotation: true              # annuncia quando un pool ruota
  warnings: true              # annuncia gli avvisi a tempo

# Icona per gli item nascosti non ancora rivelati.
# Per una testa: material "PLAYER_HEAD" + texture (base64) OPPURE owner.
hidden-item:
  material: "BARRIER"
  texture: ""
  owner: ""
  name: "<gray>Secret item</gray>"
  lore:
    - "<dark_gray>???"

# Icona per gli item VIP mostrata ai giocatori senza nyxonstore.vip.
vip-item:
  material: "BARRIER"
  name: "<gold>VIP item</gold>"
  lore:
    - "<gray>Unlock this with <gold>VIP</gold>"

# Icona mostrata quando un item raggiunge il max acquisti della rotazione.
sold-out-item:
  material: "BARRIER"
  name: "<red>Sold out</red>"
  lore:
    - "<gray>This item is sold out"

sounds:
  gui-open:      { sound: "BLOCK_CHEST_OPEN",             volume: 1.0, pitch: 1.0 }
  purchase:      { sound: "ENTITY_PLAYER_LEVELUP",        volume: 1.0, pitch: 1.0 }
  force-rotate:  { sound: "BLOCK_NOTE_BLOCK_PLING",       volume: 1.0, pitch: 1.0 }
  hidden-reveal: { sound: "ENTITY_EXPERIENCE_ORB_PICKUP", volume: 1.0, pitch: 1.2 }
```

#### gui.yml

Controlla la finestra dello shop. Gli slot di tipo `pool` sono dove appaiono
gli item ruotati (il numero di slot `pool` = quanti item mostra ogni rotazione).
Ogni altro slot è un blocco decorativo (`filler`).

```yaml
title: "<b><color:#2B2B2B>nyxon store</color></b>"
size: 54                      # 27 / 36 / 45 / 54
lore:
  price: "<gold>Price: </gold><yellow>{price} {currency}</yellow>"
  purchase-hint: "<gray>Click to <green>purchase</green>!</gray>"
slots:
  21: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  22: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  23: { type: "pool",   material: "BLACK_STAINED_GLASS_PANE", name: " " }
  0:  { type: "filler", material: "BLACK_STAINED_GLASS_PANE", name: " " }
```

#### confirmation.yml

Il menu di conferma acquisto. Ogni voce supporta le teste
(`material: "PLAYER_HEAD"` + `texture` o `owner`).

```yaml
enabled: true
title: "<gold>Confirm Purchase</gold>"
size: 27

confirm: { slot: 11, material: "LIME_WOOL", name: "<green>Confirm</green>" }
cancel:  { slot: 15, material: "RED_WOOL",  name: "<red>Cancel</red>"  }
info:    { slot: 13 }                          # item reale con la sua lore
filler:  { material: "GRAY_STAINED_GLASS_PANE", name: " " }
slots: {}                                      # slot decorativi extra opzionali
```

### 📦 Pool Configuration

Ogni file dentro `pools/` è uno shop indipendente; il nome del file è il nome
del pool. Aprilo con `/ns open <nome>`.

**Esempio MMOItems (con valuta custom `currency:`)**

```yaml
vip_sword:
  type: "mmoitems"
  mmoitems_type: "SWORD"
  mmoitems_id: "EPIC_SWORD"
  currency: "gems"                # ← paga con "gems" invece di "gold"
  price: 500
  weight: 50
  hidden: true
  max-purchases: 1
```

**Esempio Oggetto a Comando (`type: command`)**

```yaml
daily_kits:
  type: "command"
  display-material: "NETHER_STAR" # item mostrato nella GUI
  display-name: "<gold>Daily Kit"
  commands:
    - "kit daily {player}"        # eseguito come CONSOLE, {player} = nome acquirente
    - "eco give {player} 100"
  currency: "tokens"              # ← valuta custom per l'acquisto
  price: 25
  weight: 20
  max-purchases: 1
```

**Esempio ItemsAdder**

```yaml
item2:
  type: "itemsadder"
  itemsadder-id: "namespace:my_custom_item"
  price: 300
  weight: 30
```

**Esempio Item Vanilla**

```yaml
diamond_pack:
  type: "vanilla"
  material: "DIAMOND"
  display-name: "<aqua>Diamond Pack</aqua>"
  amount: 16
  price: 100
  weight: 80
  vip: true
```

**Field Reference (Spiegazione Campi)**

    type             Tipo oggetto: "vanilla" / "mmoitems" / "itemsadder" / "command"
    material         Materiale Vanilla                          (solo vanilla)
    mmoitems_type    Tipo di MMOItem                            (solo mmoitems)
    mmoitems_id      ID dell'MMOItem                            (solo mmoitems)
    itemsadder-id    ID ItemsAdder (namespace:item)             (solo itemsadder)
    display-material Materiale mostrato in GUI                  (solo command; default COMMAND_BLOCK)
    commands         Lista di comandi da eseguire in console    (solo command)
                     Usa {player} come nome del giocatore
    display-name     Nome personalizzato MiniMessage            (opzionale)
    amount           Quantità, default 1                        (vanilla / display command)
    currency         Id CoinsEngine da addebitare               (opzionale)
                     Se omesso usa il default in config.yml
    price            Prezzo dell'oggetto
    weight           Peso per la selezione (più alto = più comune)
    vip              Se true richiede nyxonstore.vip per vederlo/comprarlo
    hidden           Se true resta nascosto finché non viene cliccato
    max-purchases    Limite acquisti per rotazione; -1 = infinito
    slot             Slot GUI fisso, oppure -1 per posizionamento automatico

### 🖼️ Teste (Player Heads)

Qualsiasi icona configurabile (`hidden-item`, `vip-item`, `sold-out-item` e ogni
voce di `confirmation.yml`) può essere una testa:

```yaml
material: "PLAYER_HEAD"
texture: "<base64>"     # il valore "Value" da es. minecraft-heads.com
# oppure
owner: "PlayerName"     # usa la skin attuale di un giocatore reale
```

`texture` e `owner` vengono ignorati se `material` non è `PLAYER_HEAD`.

### 🔗 PlaceholderAPI

Identifier dell'espansione: `nyxonstore` (richiede PlaceholderAPI installato).

    %nyxonstore_default_pool%                        Nome del pool di default
    %nyxonstore_<pool>_rotation_time%                Tempo alla rotazione (Xd Xh Xm)
    %nyxonstore_<pool>_rotation_time_seconds%        Secondi alla rotazione
    %nyxonstore_<pool>_rotation_time_formatted%      Countdown leggibile
    %nyxonstore_<pool>_item_<i>_name%                Nome/materiale item all'indice i
    %nyxonstore_<pool>_item_<i>_price%               Prezzo dell'item
    %nyxonstore_<pool>_item_<i>_purchases%           Acquisti in questa rotazione
    %nyxonstore_<pool>_item_<i>_max%                 Max acquisti
    %nyxonstore_<pool>_item_<i>_soldout%             yes / no
    %nyxonstore_<pool>_item_<i>_type%                Tipo dell'item

### 📥 Installazione

1. Assicurati che **CoinsEngine** e **PlaceholderAPI** siano installati sul server.
2. *(Opzionale)* Installa **MMOItems** e/o **ItemsAdder** se vuoi vendere oggetti custom.
3. Trascina il file `NyxonStore.jar` nella cartella `/plugins`.
4. Riavvia il server (richiede Paper 1.19+ e Java 17+).
5. Configura i tuoi pool nei file generati in `plugins/NyxonStore/`.
6. Esegui `/ns admin reload` dopo le modifiche, oppure `/ns admin forcerotate` per testare subito la rotazione!

> Nota economia: gli acquisti addebitano il giocatore tramite `<currency> take <player> <price>`
> e il saldo viene letto da PlaceholderAPI. Il sistema è pensato per **CoinsEngine** —
> imposta `currency` nel config.yml con la valuta di default, oppure usane una diversa per ogni
> item con il campo `currency:` nel YAML del pool.

---

### 📝 Changelog 1.1.2

**Novità**
- **Per-item currency** — campo `currency:` per ogni item, con fallback automatico al default globale se non specificato.
- **`type: command`** — nuovi item che eseguono comandi console all'acquisto invece di dare item fisici.
  - `display-material` (default `COMMAND_BLOCK`) per l'icona nella GUI
  - `commands:` lista di comandi con placeholder `{player}`
- **Pool Editor aggiornato** — nuovo tipo `command` nel ciclo, campo currency, display-material e comandi editabili in chat.
- **Rifiniture** — tutti i commenti del sorgente rimossi, header `//made with ❤ by nyxon` in ogni classe Java, package rinominato in `net.nyxon.store`, scrittura dati asincrona.

---

<div align="center">made with ❤ by nyxon</div>
