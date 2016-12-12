(ns macchiato.server
  (:require
    [macchiato.http :as http]))

(defn- http-server
  ":host - hostname to bind
  :port - HTTP port the server will listen on
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening"
  [{:keys [handler host port on-success] :as opts}]
  (let [server (js/require "http")]
    (doto server
        (.createServer (http/handler handler (assoc opts :scheme :http)))
        (.listen port host on-success))))

(defn https-server
  ":host - hostname to bind
  :port - HTTP port the server will listen on
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening
  :private-key - path to the private key
  :certificate - path to the certificate for the key"
  [{:keys [handler host port on-success private-key certificate] :as opts}]
  (let [server (js/require "https")
        fs     (js/require "fs")
        pk     (.readFileSync fs private-key)
        pc     (.readFileSync fs certificate)]
    (doto server
      (.createServer (clj->js {:key pk :cert pc}) (http/handler handler (assoc opts :scheme :https)))
      (.listen port host on-success))))

(defn start
  ":host - hostname to bind (default 0.0.0.0)
  :port - HTTP port the server will listen on
  :protocol - :http or :https  (default :http)
  :handler - Macchiato handler function for handling request/response
  :on-success - success callback that's called when server starts listening
  :private-key - path to the private key (only used when protocol is :https)
  :certificate - path to the certificate for the key (only used when protocol is :https)"
  [{:keys [handler host port protocol]
    :or   {host     "0.0.0.0"
           protocol :http}
    :as   opts}]
  (case protocol
    :http (http-server opts)
    :https (https-server opts)
    (throw (js/Error. (str "Unrecognized protocol: " protocol " must be either :http or :https")))))
