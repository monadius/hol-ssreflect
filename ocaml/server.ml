(* #load "unix.cma";; *)
(* Already loaded by HOL Light *)
(*#directory "+compiler-libs";;*)

(*#load "ocamlcommon.cma"*)

exception Timeout

let set_timeout, get_timeout =
  let timeout = ref 20 in
  (fun t -> timeout := t),
  (fun () -> !timeout)

let () = Sys.set_signal Sys.sigalrm (Sys.Signal_handle (fun _ -> raise Timeout))

type redirected_descr = {
  new_descr : Unix.file_descr;
  mutable new_pos : int;
  mutable old_descr_dup : Unix.file_descr option;
  mutable old_descr : Unix.file_descr;
}

let create_redirected_descr fname = {
  new_descr = Unix.openfile fname [Unix.O_RDWR; Unix.O_TRUNC; Unix.O_CREAT] 0o666;
  new_pos = 0;
  old_descr_dup = None;
  old_descr = Unix.stdout;
}

let redirect old_descr redirect =
  match redirect.old_descr_dup with
  | Some _ -> failwith "The descriptor is already redirected"
  | None ->
    redirect.old_descr <- old_descr;
    redirect.old_descr_dup <- Some (Unix.dup old_descr);
    redirect.new_pos <- Unix.lseek redirect.new_descr 0 Unix.SEEK_CUR;
    Unix.dup2 redirect.new_descr old_descr

let restore redirect =
  match redirect.old_descr_dup with
  | None -> failwith "The descriptor is not redirected"
  | Some descr ->
    redirect.old_descr_dup <- None;
    Unix.dup2 descr redirect.old_descr;
    Unix.close descr

let rec really_read fd buffer start length =
  if length <= 0 then () else
    match Unix.read fd buffer start length with
    | 0 -> raise End_of_file
    | r -> really_read fd buffer (start + r) (length - r);;

let read_redirected redirect =
  try
    let pos = Unix.lseek redirect.new_descr 0 Unix.SEEK_END in
    let len = pos - redirect.new_pos in
    if len <= 0 then ""
    else
      let buffer = Bytes.create len in
      ignore (Unix.lseek redirect.new_descr redirect.new_pos Unix.SEEK_SET);
      really_read redirect.new_descr buffer 0 len;
      Bytes.to_string buffer
  with exn ->
    Printf.eprintf "Error reading the redirected file: %s"
      (Printexc.to_string exn);
    ""

let write_to_string writer =
  let sbuff = ref "" in
  let output s m n = sbuff := !sbuff ^ String.sub s m n in
  let flush () = () in
  let fmt = Format.make_formatter output flush in
  Format.pp_set_max_boxes fmt 100;
  fun arg ->
    sbuff := ""; 
    let result = writer fmt arg in
    Format.pp_print_flush fmt ();
    (result, !sbuff)

let starts_with str ~prefix =
  let n = String.length prefix in
  if n > String.length str then false
  else
    String.sub str 0 n = prefix    

let exec print_result fmt cmd = 
  let lb = Lexing.from_string cmd in
  let p1 = !Toploop.parse_toplevel_phrase lb in
  let p2 = Toploop.preprocess_phrase fmt p1 in
  Toploop.execute_phrase print_result fmt p2

(*
let exec2 print_result fmt s = ignore @@ Toploop.execute_phrase print_result fmt
  @@ !Toploop.parse_toplevel_phrase @@ Lexing.from_string s
*)

let __strBuffer = ref ""

let execute_string_cmd cmd =
  let full_cmd = "Server.__strBuffer := " ^ cmd in
  let result = exec false Format.std_formatter full_cmd in
  (result, !__strBuffer)

let new_stdout = create_redirected_descr "stdout.txt"
let new_stderr = create_redirected_descr "stderr.txt"

let try_finally (f, finally) arg =
  let result = try f arg with exn -> finally (); raise exn in
  finally (); 
  result

let toploop_service exit_flag ic oc =
  Format.printf "[START] Connection open@.";
  let process_input input =
    if starts_with input ~prefix:"raw_print_string" then
      snd (execute_string_cmd input)
    else
      snd (write_to_string (exec true) input) in
  try while true do
      let raw_input = input_line ic in
      let s = 
        try Scanf.unescaped raw_input 
        with _ -> Format.eprintf "[ERROR] Bad input@."; raw_input in
      Format.printf "Input: %s@." s;
      if String.trim s = "exit" then raise End_of_file;
      let r = begin
        try
          let finally () = 
            ignore (Unix.alarm 0);
            Format.pp_flush_formatter Format.std_formatter;
            Format.pp_flush_formatter Format.err_formatter;
            flush stdout; flush stderr;
            restore new_stdout; restore new_stderr in
          redirect Unix.stdout new_stdout;
          redirect Unix.stderr new_stderr;
          (* Set timeout for special commands only (we don't want to interrupt "needs", etc.) *)
          let set_alarm = starts_with s ~prefix:"raw_print_string" ||
                          starts_with s ~prefix:"refine" in
          let timeout = get_timeout () in
          if timeout > 0 && set_alarm then ignore (Unix.alarm timeout);
          try_finally (process_input, finally) s
        with exn ->
          let exn_str = 
            if exn = Timeout then "timeout" else Printexc.to_string exn in
          Format.eprintf "[ERROR] %s@." exn_str; 
          Format.sprintf "Error: %s" exn_str 
      end in
(*      Format.printf "Output (%d): %s@." (String.length r) r; *)
      let stdout_str = read_redirected new_stdout in
      let stderr_str = read_redirected new_stderr in
      output_string oc (String.escaped r ^ "\n");
      output_string oc ("stdout:" ^ String.escaped stdout_str ^ "\n");
      output_string oc ("stderr:" ^ String.escaped stderr_str ^ "\n");
      flush oc;
      flush stdout; 
      flush stderr
    done
  with _ -> 
    Format.printf "[STOP] Connection closed@."; 
    if exit_flag then exit 0

let string_of_sockaddr = function
  | Unix.ADDR_UNIX s -> s
  | Unix.ADDR_INET (inet_addr, _) -> Unix.string_of_inet_addr inet_addr

let establish_forkless_server server_fun sockaddr =
  let domain = Unix.domain_of_sockaddr sockaddr in
  let sock = Unix.socket domain Unix.SOCK_STREAM 0 in
  try
    Unix.bind sock sockaddr;
    Unix.listen sock 1;
    while true do
      let (s, caller) = Unix.accept sock in
      Format.printf "Connection from: %s@." (string_of_sockaddr caller);
      let inchan = Unix.in_channel_of_descr s in
      let outchan = Unix.out_channel_of_descr s in
      let finally () =
        (* close_out closes the file descriptor so Unix.close s should not be called *)
        close_out outchan;
        (* close_in is not necessary *)
        close_in_noerr inchan in
      try_finally (server_fun inchan, finally) outchan;
    done
  with Sys.Break ->
    Unix.close sock;
    Format.printf "[STOP] Server stopped@."

let get_my_addr () =
  let host = Unix.gethostbyname (Unix.gethostname ()) in
  host.Unix.h_addr_list.(0)

let main_server_with_forks serv_fun address port =
  Format.printf "Host name: %s; port number: %d@." 
    (Unix.string_of_inet_addr address) port;
  flush_all();
  Unix.establish_server serv_fun (Unix.ADDR_INET (address, port))

let main_server_without_forks serv_fun address port =
  Format.printf "Host name: %s; port number: %d (no forks)@." 
    (Unix.string_of_inet_addr address) port;
  flush_all();
  establish_forkless_server serv_fun (Unix.ADDR_INET (address, port))

let start ?(host_name = "127.0.0.1") ?(port = 2012) use_forks =
  Sys.catch_break true;
  let address = 
    if host_name = "" then get_my_addr()
    else Unix.inet_addr_of_string host_name in
  let start () = 
    if use_forks then
      main_server_with_forks (toploop_service true) address port
    else
      main_server_without_forks (toploop_service false) address port in
  Unix.handle_unix_error start ()


let port = ref 2011

let test1 () = 
  incr port;
  start ~port:!port true

let test2 () =
  incr port;
  start ~port:!port false
