(ns macchiato.middleware.resource
  "Middleware for serving static resources."
  (:require
    [cuerdas.core :as s]
    [macchiato.fs :as fs]
    [macchiato.fs.path :as path]
    [macchiato.util.response :as resp]))

(defn- file-exists? [path]
  (when path
    (try
      (and
        (fs/exists? path)
        (fs/file? path))
      (catch js/Error _))))

(defn- remove-leading-slash [url]
  (if (s/starts-with? url "/")
    (subs url 1) url))

(defn- uri->path [root-path uri]
  (let [root (path/resolve root-path)
        path (path/resolve root
                           (-> uri
                               (remove-leading-slash)
                               (js/decodeURI)
                               (s/replace #"/" path/separator)))]
    (when (s/starts-with? path root)
      path)))

(defn
  ^{:macchiato/middleware
    {:id :wrap-resource}}
  wrap-resource
  "Middleware that first checks to see whether the request map matches a static resource."
  [handler root-path]
  (fn [{:keys [uri] :as request} respond raise]
    (let [path (uri->path root-path uri)]
      (if (and (#{:head :get} (:request-method request)) (file-exists? path))
        (respond (resp/file path))
        (handler request respond raise)))))
