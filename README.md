# BuscaMinasInternet

Buscaminas distribuido sobre TCP con mensajes JSON (Gson) y un servidor con ThreadPool.
Laboratorio de Computación en Internet I — Universidad Icesi, 2026-2.

## Estructura

```
clase_tcp_udp/
├── server/   co.icesi.buscaminas...          Servidor (TCPController, ServicesImpl, BoardGame)
└── client/   co.icesi.buscaminas.client...   Cliente interactivo de consola
```

## Requisitos

- JDK 17 o superior (probado con JDK 21). `JAVA_HOME` debe apuntar a la carpeta del JDK.
- No hace falta instalar Gradle: se usa el wrapper (`gradlew`).

## Ejecución

Servidor (puerto por defecto 12345; escucha en `0.0.0.0`):

```bash
./gradlew :server:run
./gradlew :server:run --args="12345"
```

Cliente (en otra terminal). Si no se pasan argumentos, pide la IP y el puerto
(Enter = `localhost` / `12345`), verifica la conexión y luego pide el nombre del jugador:

```bash
./gradlew :client:run -q --console=plain
./gradlew :client:run -q --console=plain --args="192.168.1.20 12345"
```

Otra opción es generar los ejecutables con `./gradlew installDist` y lanzar
`server/build/install/server/bin/server` y `client/build/install/client/bin/client`
(`.bat` en Windows).

### Jugar desde otro computador

1. En el PC del servidor, use la IP del Wi-Fi/Ethernet que imprime el servidor al arrancar
   y permita Java en el Firewall de Windows (redes privadas).
2. Genere el cliente empaquetado con `./gradlew :client:distZip` y comparta
   `client/build/distributions/client.zip` (requiere Java 17+ en el otro PC).
3. En el otro PC: descomprimir y ejecutar `bin/client.bat` (o `bin/client`), escribir la IP del servidor.

Para la prueba multiusuario, abra dos o más terminales con el cliente contra el mismo servidor.
La bitácora del servidor indica qué hilo del pool (`pool-1-thread-N`) atendió cada petición.

## Protocolo

Cada mensaje es una línea JSON terminada en `\n`. Se abre una conexión por petición.

| Acción        | `data`              | Respuesta (`data`)          |
|---------------|---------------------|-----------------------------|
| `INIT_GAME`   | `n`, `m`, `minas`   | `board`                     |
| `SELECT_CELL` | `i`, `j`            | `board`, `win`, `gameEnd`   |
| `MARK_CELL`   | `i`, `j`            | `board`                     |
| `GET_BOARD`   | —                   | `board`                     |
| `SOW_ALL`     | —                   | `board`                     |

Si una petición es inválida (JSON mal formado, parámetros faltantes o fuera del tablero,
acción desconocida), el servidor responde `"status": "ERROR"` con un `message`.
