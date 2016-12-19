(ns macchiato.middleware.content-type
  "Middleware for automatically adding a content type to response maps."
  (:require [macchiato.util.mime-type :refer [ext-mime-type]]
            [macchiato.util.response :refer [content-type get-header]]))

(defn content-type-response
  "Adds a content-type header to response. See: wrap-content-type."
  ([response request]
   (content-type-response response request {}))
  ([response request options]
   (if response
     (if (get-header response "Content-Type")
       response
       (let [mime-type (ext-mime-type (:uri request) (:mime-types options))]
         (content-type response (or mime-type "text/plain")))))))

(defn
  ^{:macchiato/middleware
    {:id :wrap-content-type}}
  wrap-content-type
  "Middleware that adds a content-type header to the response if one is not
  set by the handler. Uses the macchiato.util.mime-type/ext-mime-type function to
  guess the content-type from the file extension in the URI. If no
  content-type can be found, it defaults to 'application/octet-stream'.

  Accepts the following options:

  :mime-types - a map of filename extensions to mime-types that will be
                used in addition to the ones defined in
                macchiato.util.mime-types/default-mime-types"
  ([handler]
   (wrap-content-type handler {}))
  ([handler options]
   (fn [request respond raise]
     (handler request
              (fn [response] (respond (content-type-response response request options)))
              raise))))
