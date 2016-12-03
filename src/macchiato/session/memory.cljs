(ns macchiato.session.memory
  "A session storage engine that stores session data in memory."
  (:require [macchiato.session.store :refer [SessionStore]]))

(deftype MemoryStore [session-map]
  SessionStore
  (read-session [_ key]
    (@session-map key))
  (write-session [_ key data]
    (let [key (or key (str (gensym)))]
      (swap! session-map assoc key data)
      key))
  (delete-session [_ key]
    (swap! session-map dissoc key)
    nil))

(defn memory-store
  "Creates an in-memory session storage engine. Accepts an atom as an optional
  argument; if supplied, the atom is used to hold the session data."
  ([] (memory-store (atom {})))
  ([session-atom] (MemoryStore. session-atom)))
