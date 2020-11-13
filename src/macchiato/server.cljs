(ns macchiato.server
  (:require
    [cljs.nodejs :as node]
    [macchiato.fs :as fs]
    [macchiato.http :as http]))

(def ^:no-doc ws (node/require "ws"))

(defn http-server
  ":host - hostname to bind
  :port - HTTP port the server will listen on
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening"
  [{:keys [handler host port on-success websockets?] :as opts}]
  (let [http-handler (http/handler handler (assoc opts :scheme :http))
        ^js module (node/require "http")
        ^js server (.createServer module http-handler)]
    (.listen server port host on-success)
    server))

(defn https-server
  ":host - hostname to bind
  :port - HTTP port the server will listen on
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening
  :private-key - path to the private key
  :certificate - path to the certificate for the key"
  [{:keys [handler host port on-success private-key certificate] :as opts}]
  (let [pk           (fs/slurp private-key)
        pc           (fs/slurp certificate)
        http-handler (http/handler handler (assoc opts :scheme :https))
        ^js module   (node/require "https")
        ^js server   (.createServer module (clj->js {:key pk :cert pc}) http-handler)]
    (.listen server port host on-success)
    server))

(defn ipc-server
  ":ipc-path - UNIX/Windows domain socket the server will listen on
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening"
  [{:keys [handler ipc-path on-success websockets?] :as opts}]
  (let [http-handler (http/handler handler (assoc opts :scheme :http))
        ^js module   (node/require "http")
        ^js server   (.createServer module http-handler)]
    (.listen server ipc-path on-success)
    server))

(defn start
  ":host - hostname to bind (default 0.0.0.0)
  :port - HTTP port the server will listen on
  :protocol - :http or :https  (default :http)
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening
  :private-key - path to the private key (only used when protocol is :https)
  :websockets? - boolean for enabling websockets
  :certificate - path to the certificate for the key (only used when protocol is :https)"
  [{:keys [handler host port protocol]
    :or   {host     "0.0.0.0"
           protocol :http}
    :as   opts}]
  (case protocol
    :http (http-server opts)
    :https (https-server opts)
    :ipc (ipc-server opts)
    (throw (js/Error. (str "Unrecognized protocol: " protocol " must be :http, :https, or :ipc")))))

(defn start-ws
  "starts a WebSocket server given a handler and a Node server instance"
  [server handler & [opts]]
  (let [^js wss (ws.Server. #js{:server server})]
    (.on wss "connection" (http/ws-handler handler opts))))
