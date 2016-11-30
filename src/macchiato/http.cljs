(ns macchiato.http
  (:require
    [macchiato.cookies :as cookies]
    [clojure.string :as s]))

(def Stream (js/require "stream"))

(def url-parser (js/require "url"))

(defn req->map [req res opts]
  (let [conn    (.-connection req)
        url     (.parse url-parser (.-url req))
        headers (js->clj (.-headers req) :keywordize-keys true)
        address (js->clj (.address conn) :keywordize-keys true)]

    {:server-port     (:port address)
     :server-name     (:address address)
     :remote-addr     (.-remoteAddress conn)
     :headers         headers
     :cookies         (cookies/request-cookies req res (:cookies opts))
     :content-type    (:content-type headers)
     :content-length  (:content-length headers)
     :method          (keyword (.-method req))
     :url             (.-url req)
     :uri             (.-pathname url)
     :query           (when-let [query (.-search url)] (.substring query 1))
     :body            (.-body req)
     :fresh?          (.-fresh req)
     :hostname        (-> req .-headers .-host (s/split #":") first)
     :params          (js->clj (.-param req) :keywordize-keys true)
     :protocol        (.-protocol req)
     :secure?         (.-secure req)
     :signed-cookies  (js->clj (.-signedCookies req))
     :ssl-client-cert (when-let [peer-cert-fn (.-getPeerCertificate conn)] (peer-cert-fn))
     :stale?          (.-state req)
     :subdomains      (js->clj (.-subdomains req))
     :xhr?            (.-xhr req)
     :node/request    req
     :node/response   res}))

(defprotocol IHTTPResponseWriter
  (-write-response [data res] "Write data to a http.ServerResponse"))

(defn error-response [res status err]
  ;;TODO
  )

(extend-protocol IHTTPResponseWriter

  nil
  (-write-response [_ _] true)

  string
  (-write-response [data res]
    (.write res data)
    true)

  PersistentHashMap
  (-write-response [data res]
    (.write res (-> data clj->js js/JSON.stringify))
    true)

  PersistentArrayMap
  (-write-response [data res]
    (.write res (-> data clj->js js/JSON.stringify))
    true)

  PersistentVector
  (-write-response [data res]
    (doseq [i data] (-write-response i res))
    true)

  List
  (-write-response [data res]
    (doseq [i data] (-write-response i res))
    true)

  LazySeq
  (-write-response [data res]
    (doseq [i data] (-write-response i res))
    true)

  js/Buffer
  (-write-response [data res]
    (.write res data)
    true)

  Stream
  (-write-response [data res]
    (.on data "error" #(error-response res 500 %))
    (.pipe data res)
    false))

(defn response [req res opts]
  (fn [{:keys [cookies headers body status]}]
    (cookies/set-cookies cookies req res (:cookies opts))
    (.writeHead res status (clj->js headers))
    (when (-write-response body res)
      (.end res))))

(defn handler [handler-fn & [opts]]
  (let [opts (or opts {})]
    (fn [req res]
      (handler-fn (req->map req res opts) (response req res opts)))))
