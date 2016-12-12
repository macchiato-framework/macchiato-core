(ns macchiato.http
  (:require
    [macchiato.cookies :as cookies]
    [macchiato.middleware.session :as session]
    [clojure.string :as s]))

(def Stream (js/require "stream"))

(def url-parser (js/require "url"))

(defn req->map [req res opts]
  (let [conn         (.-connection req)
        url          (.parse url-parser (.-url req) true)
        scheme       (if (boolean (.-encrypted conn)) :https :http)
        http-version (.-httpVersion req)
        headers      (js->clj (.-headers req))
        address      (js->clj (.address conn) :keywordize-keys true)]

    {:server-port     (:port address)
     :server-name     (:address address)
     :remote-addr     (.-remoteAddress conn)
     :headers         headers
     :cookies         (cookies/request-cookies req res (:cookies opts))
     :content-type    (get headers "content-type")
     :content-length  (get headers "content-length")
     :request-method  (keyword (s/lower-case (.-method req)))
     :url             (.-url req)
     :uri             (.-pathname url)
     :query-string    (when-let [query (.-search url)] (.substring query 1))
     :body            (.-body req)
     :fresh?          (.-fresh req)
     :hostname        (-> req .-headers .-host (s/split #":") first)
     :params          (js->clj (.-param req))
     :protocol        (str (if (= :http scheme) "HTTP/" "HTTPS/") http-version)
     :secure?         (.-secure req)
     :signed-cookies  (js->clj (.-signedCookies req))
     :ssl-client-cert (when-let [peer-cert-fn (.-getPeerCertificate conn)] (peer-cert-fn))
     :stale?          (.-state req)
     :subdomains      (js->clj (.-subdomains req))
     :xhr?            (.-xhr req)
     :scheme          scheme
     :node/request    req
     :node/response   res}))

(defprotocol IHTTPResponseWriter
  (-write-response [data res raise] "Write data to a http.ServerResponse"))

(extend-protocol IHTTPResponseWriter

  nil
  (-write-response [_ _ _])

  string
  (-write-response [data node-server-response _]
    (.write node-server-response data)
    (.end node-server-response))

  PersistentHashMap
  (-write-response [data node-server-response _]
    (.write node-server-response (-> data clj->js js/JSON.stringify))
    (.end node-server-response))

  PersistentArrayMap
  (-write-response [data node-server-response _]
    (.write node-server-response (-> data clj->js js/JSON.stringify))
    (.end node-server-response))

  PersistentVector
  (-write-response [data node-server-response raise]
    (doseq [i data] (-write-response i node-server-response raise))
    (.end node-server-response))

  List
  (-write-response [data node-server-response raise]
    (doseq [i data] (-write-response i node-server-response raise))
    (.end node-server-response))

  LazySeq
  (-write-response [data node-server-response raise]
    (doseq [i data] (-write-response i node-server-response raise))
    (.end node-server-response))

  js/Buffer
  (-write-response [data node-server-response _]
    (.write node-server-response data)
    (.end node-server-response))

  Stream
  (-write-response [data node-server-response raise]
    (.on data "error" raise)
    (.pipe data node-server-response)))

(defn response [request-map node-server-response raise opts]
  (fn [{:keys [cookies headers body status]}]
    (try
      (cookies/set-cookies cookies request-map node-server-response (:cookies opts))
      (.writeHead node-server-response status (clj->js headers))
      (-write-response body node-server-response raise)
      (catch js/Error e
        (raise e)))))

(defn error-handler [node-server-response]
  (fn [error]
    (doto node-server-response
      (.writeHead 500 #js {"content-type" "text/html"})
      (.write (.-message error))
      (.end))))

(defn handler [handler-fn & [opts]]
  (let [opts         (merge {} opts)
        http-handler (if-let [session-opts (:session opts)]
                       (session/wrap-session handler-fn session-opts)
                       handler-fn)]
    (fn [node-client-request node-server-response]
      (http-handler (req->map node-client-request node-server-response opts)
                    (response node-client-request node-server-response error-handler opts)
                    (error-handler node-server-response)))))
