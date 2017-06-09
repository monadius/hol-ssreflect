# Installation

## Requirements

 - Java SDK 1.8
 - OCaml 4.04
 - Camlp5 (required by HOL Light)
 - HOL Light

## Linux and macOS

1) Run `./gradlew make`.

1) Go to `ocaml` and run `make`.

2) Go to the HOL Light directory and create the file `server.hl` with the following content:
```
load_path := "{Absolute path to HOL-SSReflect}/ocaml" :: !load_path;;
#directory "{Absolute path to HOL-SSReflect}/ocaml";;
loadt "start_server.hl";;
```
Here, `{Absolute path to HOL-SSReflect}` should be replaced with your path to the
copy of HOL-SSReflect.

3) Load a HOL Light session.

 - Run
 ```
 ocaml -I `camlp5 -where`
 ```

- Type `#use "hol.ml";;` and press Enter. Wait.

- Load all HOL Light libraries which you need.

- Type `needs "server.hl";;` and press Enter.

- Type `Server.start false;;` and press Enter.
 
4) Run `bin/HOL-SSReflect` in a new terminal.

# Additional info

## `Server.start`

The function `Server.start` has the following parameter:
 
- `use_forks:bool`: The server can process connections in the same process 
as the current HOL Light session or it can create a new process for each connection.
If this parameter is `false` then all connections are processed in the current HOL Light
process. In this case, only one client can be connected to the server at any time. The advantage
of this mode is that no additional memory is allocated for each new connection. Also,
the state of the current session is preserved between different connections.
If this parameter is `true` then each new connection spawns a new process.

- `?(host_name = "127.0.0.1")`: This optional parameter defines the host name of the server.

- `?(port = 2012)`: This optinal parameter defines the port number of the server.

An example:
```
Server.start ~host_name:"my_host" ~port:1500 true;;
```

## `bin/HOL-SSReflect`

```
bin/HOL-SSReflect [host_name] [port_number]
```

The parameters `host_name` and `port_number` can be used to connect to a remote server. 
The default values of these parameters are: `127.0.0.1` and `2012`.

In particular, it is possible to run a HOL Light session inside a virtual machine and connect
to it from a host machine. In this case, you need to set up an appropriate network interface
between host and the virtual machine. It may be also necessary to start the server 
(`Server.start`) with a correct `host_name`.

Instead of the `bin/HOL-SSReflect` script, you can run the `jar` file:
```
java -jar lib/HOL-SSReflect.jar [host_name] [port_number]
```
