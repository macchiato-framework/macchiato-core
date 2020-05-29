(ns macchiato.middleware.session.cookie
  (:require
    [cljs.reader :as edn]
    [macchiato.crypto :as c]
    [macchiato.middleware.session.store :refer [SessionStore]]))

(defn- seal
  "Seal a Clojure data structure into an encrypted and HMACed string."
  [key data]
  (let [data (c/encrypt key (pr-str data))]
    (str (.toString (.from js/Buffer data) "base64") "--" (c/hmac key data))))

(defn- unseal
  "Retrieve a sealed Clojure data structure from a string"
  [key string]
  (let [[data mac] (.split string "--")
        data (.toString (.from js/Buffer data "base64") "utf8")]
    (if (c/eq? mac (c/hmac key data))
      (edn/read-string (c/decrypt key data)))))

(deftype CookieStore [secret-key]
  SessionStore
  (read-session [_ data]
    (if data (unseal secret-key data)))
  (write-session [_ _ data]
    (seal secret-key data))
  (delete-session [_ _]
    (seal secret-key {})))

(defn- valid-secret-key? [key]
  (= (count key) 16))

(defn- get-secret-key
  "Get a valid secret key from a map of options, or create a random one from
  scratch."
  [options]
  (or (:key options)
      (apply str (repeatedly 16 #(rand-nth "abcdefghikjlmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")))))

(defn cookie-store
  "Creates an encrypted cookie storage engine. Accepts the following options:
  :key - The secret key to encrypt the session cookie. Must be exactly 16 bytes
         If no key is provided then a random key will be generated. Note that in
         that case a server restart will invalidate all existing session
         cookies."
  ([] (cookie-store {}))
  ([options]
   (let [key (get-secret-key options)]
     (assert (valid-secret-key? key) "the secret key must be exactly 16 bytes")
     (CookieStore. (get-secret-key options)))))
