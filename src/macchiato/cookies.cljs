(ns macchiato.cookies)

(def Cookies (js/require "cookies"))

(def random-bytes (js/require "random-bytes"))

(def secret (str (random-bytes. 32)))

(def ^{:doc "HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068"} re-token #"[!#$%&'*\-+.0-9A-Z\^_`a-z\|~]+")

(def ^{:private true, :doc "RFC6265 cookie-octet"} re-cookie-octet #"[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]")

(def ^{:private true, :doc "RFC6265 cookie-value"} re-cookie-value (re-pattern (str "\"" (.-source re-cookie-octet) "*\"|" (.-source re-cookie-octet) "*")))

(def ^{:private true, :doc "RFC6265 set-cookie-string"}  re-cookie (re-pattern (str "\\s*(" (.-source re-token) ")=(" (.-source re-cookie-value) ")\\s*[;,]?")))

(defprotocol ICookie
  (-serialize-cookie [cookie] "serialize cookie value"))

(extend-protocol ICookie
  nil
  (-serialize-cookie [_] nil)

  string
  (-serialize-cookie [cookie]
    cookie)

  PersistentHashMap
  (-serialize-cookie [cookie]
    (-> cookie clj->js js/JSON.stringify))

  PersistentArrayMap
  (-serialize-cookie [cookie]
    (-> cookie clj->js js/JSON.stringify)))

(defn translate-cookie-opts [{:keys [secure max-age expires http-only path domain overwrite?]}]
  (clj->js
    (merge
      (when secure {:signed true})
      (when max-age {:maxAge max-age})
      (when path {:path path})
      (when domain {:domain domain})
      (when expires {:expires expires})
      (when http-only {:http-only http-only})
      (when overwrite? {:overwrite overwrite?}))))

(defn gen-keys [cookie-opts]
  (clj->js {:keys (or (:keys cookie-opts) [secret])}))

(defn signed [opts]
  (clj->js {:signed (boolean (:signed? opts))}))

(defn set-cookies [cookies req res cookie-opts]
  (let [cookie-manager (Cookies. req res (clj->js (gen-keys cookie-opts)))]
    (doseq [[k {:keys [value] :as opts}] cookies]
      (.set cookie-manager (name k) (-serialize-cookie value) (translate-cookie-opts opts)))))

(defn- parse-cookie-header
  "Turn a HTTP Cookie header into a list of name/value pairs."
  [header]
  (for [[_ name value] (re-seq re-cookie header)]
    [name value]))

(defn request-cookies [req res opts]
  (let [cookie-manager (Cookies. req res (gen-keys opts))]
    (reduce
      (fn [cookies [k]]
        (assoc cookies k (.get cookie-manager (name k) (signed opts))))
      {} (-> (.-headers req) (aget "cookie") parse-cookie-header))))