(ns macchiato.session.cookie
  (:require
    [cljs.reader :as edn]
    [macchiato.session.store :refer [SessionStore]]))

(def crypto (js/require "simple-encryptor"))

(defn encrypt [key data]
  (.encrypt (crypto. key) data))

(defn decrypt [key data]
  (.decrypt (crypto. key) data))

(defn eq?
  "Test whether two sequences of characters or bytes are equal in a way that
  protects against timing attacks. Note that this does not prevent an attacker
  from discovering the *length* of the data being compared."
  [a b]
  (let [a (map int a), b (map int b)]
    (if (and a b (= (count a) (count b)))
      (zero? (reduce bit-or (map bit-xor a b)))
      false)))

(defn hmac [key data]
  (.hmac (crypto. key) data))

(defn- seal
  "Seal a Clojure data structure into an encrypted and HMACed string."
  [key data]
  (let [data (encrypt key (pr-str data))]
    (str (.toString (js/Buffer. data) "base64") "--" (hmac key data))))

(defn- unseal
  "Retrieve a sealed Clojure data structure from a string"
  [key string]
  (let [[data mac] (.split string "--")
        data (.toString (js/Buffer. data "base64") "utf8")]
    (if (eq? mac (hmac key data))
      (edn/read-string (decrypt key data)))))

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
