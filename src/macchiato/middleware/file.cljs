(ns macchiato.middleware.file
  (:require
    [cljs-time.core :refer [before?]]
    [macchiato.fs :as fs]
    [macchiato.util.response :as res]
    [macchiato.util.mime-type :refer [ext-mime-type]]))

(def Stream (js/require "stream"))
(def etag (js/require "etag"))

(defn- guess-mime-type
  "Returns a String corresponding to the guessed mime type for the given file,
  or application/octet-stream if a type cannot be guessed."
  [file mime-types]
  (or (ext-mime-type (.-path file) mime-types)
      "application/octet-stream"))

(defn- file-stats [stream mime-types]
  (let [stats (->> stream .-path (fs/stat))]
    {:etag      (etag stats)
     :lmodified (-> stats :mtime .getTime (js/Date.) .toUTCString)
     :size      (:size stats)
     :type      (guess-mime-type stream mime-types)}))

(defn file-info-response
  "Adds headers to response as described in wrap-file-info."
  ([response request]
   (file-info-response response request {}))
  ([response request mime-types]
   (let [body (:body response)]
     (if (instance? Stream body)
       (let [{:keys [etag lmodified size type]} (file-stats body mime-types)
             response (-> response
                          (res/content-type type)
                          (res/header "ETag" etag)
                          (res/header "Last-Modified" lmodified))]
         (if (= (get-in request [:headers :if-none-match]) etag)
           (-> response
               (res/status 304)
               (res/header "Content-Length" 0)
               (assoc :body ""))
           (-> response (res/header "Content-Length" size))))
       response))))

(defn wrap-file
  "Wrap a handler such that responses with a file for a body will have
  corresponding Content-Type, Content-Length, and Last Modified headers added if
  they can be determined from the file.
  If the request specifies a If-Modified-Since header that matches the last
  modification date of the file, a 304 Not Modified response is returned.
  If two arguments are given, the second is taken to be a map of file extensions
  to content types that will supplement the default, built-in map."
  ([handler]
   (wrap-file handler {}))
  ([handler mime-types]
   (fn [request respond raise]
     (handler request
              (fn [response]
                (respond (file-info-response response request mime-types)))
              raise))))
