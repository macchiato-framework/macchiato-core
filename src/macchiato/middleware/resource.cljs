(ns macchiato.middleware.resource
  "Middleware for serving static resources."
  (:require
    [cuerdas.core :as s]
    [macchiato.fs :as fs]
    [macchiato.fs.path :as path]
    [macchiato.util.response :as resp]))

(defn file-exists? [path]
  (try
    (and
      (fs/exists? path)
      (fs/file? path))
    (catch js/Error _)))

(defn uri->path [root-path uri]
  (s/replace (str root-path (js/decodeURI uri)) #"/" path/separator))

(defn
  ^{:macchiato/middleware
    {:id :wrap-resource}}
  wrap-resource
  "Middleware that first checks to see whether the request map matches a static resource."
  [handler root-path & [opts]]
  (fn [{:keys [uri] :as request} respond raise]
    (let [path (uri->path root-path uri)]
      (if (and (#{:head :get} (:request-method request)) (file-exists? path))
        (respond (resp/file path))
        (handler request respond raise)))))
