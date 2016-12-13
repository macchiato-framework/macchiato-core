(ns macchiato.middleware.resource
  "Middleware for serving static resources."
  (:require
    [cuerdas.core :as s]
    [macchiato.util.response :as resp]))

(def fs (js/require "fs"))

(defn file-exists? [path]
  (try
    (and
      (.existsSync fs path)
      (.isFile (.lstatSync fs path)))
    (catch js/Error _)))

(def path-separator (.-sep (js/require "path")))

(defn uri->path [root-path uri]
  (s/replace (str root-path (js/decodeURI uri)) #"/" path-separator))

(defn wrap-resource
  "Middleware that first checks to see whether the request map matches a static resource."
  [handler root-path & [opts]]
  (fn [{:keys [uri] :as request} respond raise]
    (let [path (uri->path root-path uri)]
      (if (and (#{:head :get} (:request-method request)) (file-exists? path))
        (respond (resp/file path))
        (handler request respond raise)))))
