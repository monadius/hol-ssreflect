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

let hol_service ic oc =
  (*  Location.formatter_for_warnings := Format.err_formatter; *)
  try while true do
      let raw_input = input_line ic in
      let s = 
        try Scanf.unescaped raw_input 
        with _ -> Printf.eprintf "[ERROR] Bad input\n"; flush stderr; raw_input in
      Printf.printf "Input: %s\n" s; flush stdout;
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
            Printf.eprintf "[ERROR] %s\n" exn_str; 
            Printf.sprintf "Error: %s" exn_str
        end in
      Printf.printf "Output (%d): %s\n" (String.length r) r;
      output_string oc (String.escaped r ^ "\n"); flush oc;
      flush stdout; flush stderr
    done
  with _ -> Printf.printf "[INFO] End\n"; flush stdout; exit 0;;


let get_my_addr () =
  let host = Unix.gethostbyname (Unix.gethostname ()) in
  host.Unix.h_addr_list.(0);;

let main_server (serv_fun, port) =
  let my_address = get_my_addr() in
  (*      let my_address = Unix.inet_addr_of_string "127.0.0.1" in *)
  Printf.printf "Port number: %d\n" port;
  flush_all();
  Unix.establish_server serv_fun (Unix.ADDR_INET (my_address, port));;

let port = ref 1499;;

let main () = 
  incr port;
  Unix.handle_unix_error main_server (hol_service, !port);;