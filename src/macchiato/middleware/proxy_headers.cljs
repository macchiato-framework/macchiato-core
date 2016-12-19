(ns macchiato.middleware.proxy-headers
  (:require [cuerdas.core :as str]))

(defn forwarded-remote-addr-request
  "Change the :remote-addr key of the request map to the last value present in
  the X-Forwarded-For header. See: wrap-forwarded-remote-addr."
  [request]
  (if-let [forwarded-for (get-in request [:headers "x-forwarded-for"])]
    (let [remote-addr (str/trim (re-find #"[^,]*$" forwarded-for))]
      (assoc request :remote-addr remote-addr))
    request))

(defn
  ^{:macchiato/middleware
    {:id :wrap-forwarded-remote-addr}}
  wrap-forwarded-remote-addr
  "Middleware that changes the :remote-addr of the request map to the
  last value present in the X-Forwarded-For header."
  [handler]
  (fn [request respond raise]
    (handler (forwarded-remote-addr-request request) respond raise)))
