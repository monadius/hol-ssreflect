(* #load "unix.cma";; *)
(* Already loaded by HOL Light *)
(*#directory "+compiler-libs";;*)

(*#load "ocamlcommon.cma"*)

let write_to_string writer =
  let sbuff = ref "" in
  let output s m n = sbuff := !sbuff ^ String.sub s m n and
    flush () = () in
  let fmt = Format.make_formatter output flush in
(*  ignore (Format.pp_set_max_boxes fmt 100); *)
  fun arg -> ignore (writer fmt arg);
    ignore (Format.pp_print_flush fmt ());
    let s = !sbuff in
    let () = sbuff := "" in
    s;;

let exec fmt s = ignore @@ Toploop.execute_phrase true fmt
  @@ Toploop.preprocess_phrase fmt @@ !Toploop.parse_toplevel_phrase @@ Lexing.from_string s;;

let exec2 fmt s = ignore @@ Toploop.execute_phrase true fmt
  @@ !Toploop.parse_toplevel_phrase @@ Lexing.from_string s;;


let hol_service ic oc =
  (*  Location.formatter_for_warnings := Format.err_formatter; *)
  try while true do
      let s = input_line ic in
      Printf.printf "Input: %s\n" s; flush stdout;
      if String.trim s = "exit" then raise End_of_file;
      let r = begin
        try
          write_to_string exec2 s
        with exn ->
          (*          let _ = Location.report_exception Format.err_formatter exn in *)
          let exn_str = Printexc.to_string exn in
          Printf.eprintf "Error: %s\n" exn_str; 
          Printf.sprintf "Error: %s" exn_str
      end in
      Printf.printf "Output: %s\n" r;
      output_string oc (String.escaped r ^ "\n"); flush oc;
      flush stdout; flush stderr
    done
  with _ -> Printf.printf "End of text\n"; flush stdout; exit 0;;


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