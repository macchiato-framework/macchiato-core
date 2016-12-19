(ns macchiato.middleware.default-charset
  "Middleware for automatically adding a charset to the content-type header in
  response maps."
  (:require [macchiato.util.response :as response]))

(defn- text-based-content-type? [content-type]
  (or (re-find #"text/" content-type)
      (re-find #"application/xml" content-type)))

(defn- contains-charset? [content-type]
  (re-find #";\s*charset=[^;]*" content-type))

(defn default-charset-response
  "Add a default charset to a response if the response has no charset and
  requires one. See: wrap-default-charset."
  [response charset]
  (if response
    (if-let [content-type (response/get-header response "Content-Type")]
      (if (and (text-based-content-type? content-type)
               (not (contains-charset? content-type)))
        (response/charset response charset)
        response)
      response)))

(defn
  ^{:macchiato/middleware
    {:id :wrap-default-charset}}
  wrap-default-charset
  "Middleware that adds a charset to the content-type header of the response if
  one was not set by the handler."
  [handler charset]
  (fn [request respond raise]
    (handler request #(respond (default-charset-response % charset)) raise)))
