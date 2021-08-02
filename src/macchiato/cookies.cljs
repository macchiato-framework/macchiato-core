(ns macchiato.cookies
  (:require
    [cljs.nodejs :as node]))

(def ^:no-doc Cookies (node/require "cookies"))

(def ^:no-doc random-bytes (node/require "random-bytes"))

(def ^:no-doc secret (str (random-bytes. 32)))

(def ^{:doc "HTTP token: 1*<any CHAR except CTLs or tspecials>. See RFC2068"} re-token #"[!#$%&'*\-+.0-9A-Z\^_`a-z\|~]+")

(def ^{:private true, :doc "RFC6265 cookie-octet"} re-cookie-octet #"[!#$%&'()*+\-./0-9:<=>?@A-Z\[\]\^_`a-z\{\|\}~]")

(def ^{:private true, :doc "RFC6265 cookie-value"} re-cookie-value (re-pattern (str "\"" (.-source re-cookie-octet) "*\"|" (.-source re-cookie-octet) "*")))

(def ^{:private true, :doc "RFC6265 set-cookie-string"} re-cookie (re-pattern (str "\\s*(" (.-source re-token) ")=(" (.-source re-cookie-value) ")\\s*[;,]?")))

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

(defn- translate-cookie-opts [{:keys [same-site secure signed max-age expires http-only path domain overwrite?]}]
  (clj->js
    (merge
      (when secure {:secure true})
      (when signed {:signed true})
      (when same-site {:sameSite same-site})
      (when max-age {:maxAge max-age})
      (when path {:path path})
      (when domain {:domain domain})
      (when expires {:expires expires})
      (when (some? http-only) {:httpOnly http-only})
      (when overwrite? {:overwrite overwrite?}))))

(defn- gen-keys [cookie-opts]
  (clj->js {:keys (or (:keys cookie-opts) [secret])}))

(defn- signed [opts]
  (clj->js {:signed (boolean (:signed? opts))}))

(defn ^:no-doc set-cookies [cookies req res cookie-opts]
  (let [cookie-manager (Cookies. req res (clj->js (gen-keys cookie-opts)))]
    (doseq [[k {:keys [value] :as opts}] cookies]
      (.set cookie-manager (name k) (-serialize-cookie value) (translate-cookie-opts opts)))))

(defn ^:no-doc request-cookies [req res opts]
  (when-let [cookies (-> (.-headers req) (aget "cookie"))]
    (let [cookie-manager (Cookies. req res (gen-keys opts))]
      (reduce
        (fn [cookies k]
          (assoc cookies k {:value (.get cookie-manager (name k) (signed opts))}))
        {} (map second (re-seq re-cookie cookies))))))
