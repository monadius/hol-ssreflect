(* #load "unix.cma";; *)
(* Already loaded by HOL Light *)
(*#directory "+compiler-libs";;*)

(*#load "ocamlcommon.cma"*)

let write_to_string writer =
  let sbuff = ref "" in
  let output s m n = sbuff := !sbuff ^ String.sub s m n and
    flush () = () in
  let fmt = Format.make_formatter output flush in
  ignore (Format.pp_set_max_boxes fmt 100);
  fun arg -> ignore (writer fmt arg);
    ignore (Format.pp_print_flush fmt ());
    let s = !sbuff in
    let () = sbuff := "" in
    s;;

let starts_with str ~prefix =
  let n = String.length prefix in
  if n > String.length str then false
  else
    String.sub str 0 n = prefix    

let exec print_result fmt s = ignore @@ Toploop.execute_phrase print_result fmt
  @@ Toploop.preprocess_phrase fmt @@ !Toploop.parse_toplevel_phrase @@ Lexing.from_string s;;

let exec2 print_result fmt s = ignore @@ Toploop.execute_phrase print_result fmt
  @@ !Toploop.parse_toplevel_phrase @@ Lexing.from_string s;;

let __strBuffer = ref "";;

let execute_string_cmd cmd =
  let full_cmd = "Server.__strBuffer := " ^ cmd in
  exec false Format.std_formatter full_cmd;
  !__strBuffer;;

let hol_service exit_flag ic oc =
  Format.printf "[START] Connection open@.";
  (*  Location.formatter_for_warnings := Format.err_formatter; *)
  try while true do
      let raw_input = input_line ic in
      let s = 
        try Scanf.unescaped raw_input 
        with _ -> Format.eprintf "[ERROR] Bad input@."; raw_input in
      Format.printf "Input: %s@." s;
      if String.trim s = "exit" then raise End_of_file;
      let r = 
        begin
          try
            if starts_with s ~prefix:"raw_print_string" then
              execute_string_cmd s
            else
              write_to_string (exec true) s
          with exn ->
            (*          let _ = Location.report_exception Format.err_formatter exn in *)
            let exn_str = Printexc.to_string exn in
            Format.eprintf "[ERROR] %s@." exn_str; 
            Format.sprintf "Error: %s" exn_str
        end in
      Format.printf "Output (%d): %s@." (String.length r) r;
      output_string oc (String.escaped r ^ "\n"); flush oc;
      flush stdout; flush stderr
    done
  with _ -> 
    Format.printf "[STOP] Connection closed@."; 
    if exit_flag then exit 0;;

let string_of_sockaddr = function
    | Unix.ADDR_UNIX s -> s
    | Unix.ADDR_INET (inet_addr, _) -> Unix.string_of_inet_addr inet_addr;;

let establish_single_server server_fun sockaddr =
  let domain = Unix.domain_of_sockaddr sockaddr in
  let sock = Unix.socket domain Unix.SOCK_STREAM 0 in
  Unix.bind sock sockaddr;
  Format.printf "Listening: %s@." (string_of_sockaddr sockaddr);
  Unix.listen sock 1;
  while true do
    let (s, caller) = Unix.accept sock in
    Format.printf "Connection from: %s@." (string_of_sockaddr caller);
    let inchan = Unix.in_channel_of_descr s in
    let outchan = Unix.out_channel_of_descr s in
    server_fun inchan outchan;
(*    close_in inchan;
    close_out outchan; *)
    Unix.close s
  done;;

let get_my_addr () =
  let host = Unix.gethostbyname (Unix.gethostname ()) in
  host.Unix.h_addr_list.(0);;

let main_server_with_forks (serv_fun, port) =
  let my_address = get_my_addr() in
  (*      let my_address = Unix.inet_addr_of_string "127.0.0.1" in *)
  Format.printf "Port number: %d@." port;
  flush_all();
  Unix.establish_server serv_fun (Unix.ADDR_INET (my_address, port));;

let main_server_without_forks (serv_fun, port) =
  let my_address = get_my_addr() in
  Format.printf "Port number: %d (no forks)@." port;
  flush_all();
  establish_single_server serv_fun (Unix.ADDR_INET (my_address, port));;

let port = ref 1499;;

let main1 () = 
  incr port;
  Unix.handle_unix_error main_server_with_forks ((hol_service true), !port);;

let main2() =
  incr port;
  Unix.handle_unix_error main_server_without_forks ((hol_service false), !port);;
