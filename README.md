# ModernStore

Shop a rotazione per Paper. A ogni ciclo pesca gli item da un pool in base al peso. Versione 1.1.2.

Richiede Paper 1.19+ e Java 17. CoinsEngine e PlaceholderAPI servono per prezzi e saldo. MMOItems e ItemsAdder sono opzionali.

Cartella dati: `plugins/ModernStore/`.

## Installazione

1. Metti `ModernStore.jar` in `plugins/`.
2. Avvia il server. Vengono creati `config.yml`, `gui.yml`, `confirmation.yml` e `pools/`.
3. Dopo una modifica: `/ns admin reload`. Per provare subito una rotazione: `/ns admin forcerotate`.

Gli acquisti scalano il saldo con `<currency> take <player> <price>`. Il saldo si legge da PlaceholderAPI. `currency` in `config.yml` è il default CoinsEngine. Si può sovrascrivere sul singolo item.

## Comandi

| Comando | Cosa fa |
|---|---|
| `/ns`, `/ns open` | Apre il pool di default |
| `/ns open [pool]` | Apre un pool |
| `/ns open <pool> <player>` | Lo apre a un altro player |
| `/ns admin edit [pool]` | Editor in game |
| `/ns admin reload` | Ricarica i file |
| `/ns admin forcerotate [pool]` | Ruota subito |

Permessi:

| Nodo | Default | Cosa fa |
|---|---|---|
| `nyxonstore.open` | tutti | Aprire lo shop |
| `nyxonstore.vip` | op | Vedere e comprare gli item VIP |
| `nyxonstore.admin` | op | Editor, reload, forcerotate, vede anche hidden e VIP |

## config.yml

Timer, valuta, broadcast, suoni e icone condivise (nascosto, VIP, esaurito).

```yaml
prefix: "<gray>[<gold>ModernStore</gold>]</gray>"
rotation: "1d 0h 0m"          # d / h / m, es. "12h 30m"
currency: "gold"              # id CoinsEngine, default globale
default-pool: "default"

broadcast:
  enabled: true
  rotation: true
  warnings: true

hidden-item:
  material: "BARRIER"
  texture: ""
  owner: ""
  name: "<gray>Secret item</gray>"
  lore:
    - "<dark_gray>???"

vip-item:
  material: "BARRIER"
  name: "<gold>VIP item</gold>"
  lore:
    - "<gray>Unlock this with <gold>VIP</gold>"

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

Per una testa: `material: "PLAYER_HEAD"` e `texture` (base64, il campo Value) oppure `owner` (skin di un player). `texture` e `owner` valgono solo con `PLAYER_HEAD`. Lo stesso vale per le voci di `confirmation.yml`.

## gui.yml

Gli slot `pool` sono quelli degli item ruotati. Quanti sono, tanti item escono a rotazione. Gli altri slot sono `filler`.

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

## confirmation.yml

Menu di conferma. `info` mostra l'item vero, con la lore dello shop.

```yaml
enabled: true
title: "<gold>Confirm Purchase</gold>"
size: 27

confirm: { slot: 11, material: "LIME_WOOL", name: "<green>Confirm</green>" }
cancel:  { slot: 15, material: "RED_WOOL",  name: "<red>Cancel</red>" }
info:    { slot: 13 }
filler:  { material: "GRAY_STAINED_GLASS_PANE", name: " " }
slots: {}
```

## Pool

Ogni file in `pools/` è uno shop. Il nome del file è il nome del pool: `/ns open <nome>`.

MMOItems, con valuta diversa dal default:

```yaml
vip_sword:
  type: "mmoitems"
  mmoitems_type: "SWORD"
  mmoitems_id: "EPIC_SWORD"
  currency: "gems"
  price: 500
  weight: 50
  hidden: true
  max-purchases: 1
```

Item che esegue comandi in console invece di dare un oggetto. `{player}` è chi compra.

```yaml
daily_kits:
  type: "command"
  display-material: "NETHER_STAR"
  display-name: "<gold>Daily Kit"
  commands:
    - "kit daily {player}"
    - "eco give {player} 100"
  currency: "tokens"
  price: 25
  weight: 20
  max-purchases: 1
```

ItemsAdder:

```yaml
item2:
  type: "itemsadder"
  itemsadder-id: "namespace:my_custom_item"
  price: 300
  weight: 30
```

Vanilla:

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

Campi:

| Campo | Uso |
|---|---|
| `type` | `vanilla`, `mmoitems`, `itemsadder`, `command` |
| `material` | Materiale vanilla |
| `mmoitems_type`, `mmoitems_id` | Tipo e id MMOItems |
| `itemsadder-id` | Id `namespace:item` |
| `display-material` | Icona in GUI per i `command`. Default `COMMAND_BLOCK` |
| `commands` | Comandi console. `{player}` è l'acquirente |
| `display-name` | Nome MiniMessage, opzionale |
| `amount` | Quantità, default 1 |
| `currency` | Id CoinsEngine. Se manca, usa `config.yml` |
| `price` | Prezzo |
| `weight` | Peso in estrazione. Più alto, più spesso esce |
| `vip` | Richiede `nyxonstore.vip` |
| `hidden` | Resta mascherato finché non si clicca |
| `max-purchases` | Limite per rotazione. `-1` = nessuno |
| `slot` | Slot fisso, oppure `-1` per il piazzamento automatico |

Due player con la GUI aperta non superano `max-purchases`.

## PlaceholderAPI

Identificatore: `nyxonstore`.

| Placeholder | Valore |
|---|---|
| `%nyxonstore_default_pool%` | Nome del pool di default |
| `%nyxonstore_<pool>_rotation_time%` | Tempo alla rotazione (`Xd Xh Xm`) |
| `%nyxonstore_<pool>_rotation_time_seconds%` | Secondi alla rotazione |
| `%nyxonstore_<pool>_rotation_time_formatted%` | Countdown |
| `%nyxonstore_<pool>_item_<i>_name%` | Nome o materiale all'indice i |
| `%nyxonstore_<pool>_item_<i>_price%` | Prezzo |
| `%nyxonstore_<pool>_item_<i>_purchases%` | Acquisti in questa rotazione |
| `%nyxonstore_<pool>_item_<i>_max%` | Massimo |
| `%nyxonstore_<pool>_item_<i>_soldout%` | `yes` / `no` |
| `%nyxonstore_<pool>_item_<i>_type%` | Tipo dell'item |

## License

MIT. Nyxon / ModernStore.
