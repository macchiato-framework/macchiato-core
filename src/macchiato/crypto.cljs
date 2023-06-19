(ns macchiato.crypto
  (:require
    ["crypto" :as crypto]
    ["simple-encryptor" :as encryptor]))

(defn encrypt [key data]
  (.encrypt (encryptor. key) data))

(defn decrypt [key data]
  (.decrypt (encryptor. key) data))

(defn hmac [key data]
  (.hmac (encryptor. key) data))

(defn eq?
  "Test whether two sequences of characters or bytes are equal in a way that
  protects against timing attacks. Note that this does not prevent an attacker
  from discovering the *length* of the data being compared."
  [a b]
  (let [a (map #(.charCodeAt %) a), b (map #(.charCodeAt %) b)]
    (if (and a b (= (count a) (count b)))
      (zero? (reduce bit-or (map bit-xor a b)))
      false)))

(defn random-base64
  "generates a random base64 string of length n"
  [n]
  (-> (.randomBytes crypto n) (.toString "base64")))
