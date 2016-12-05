(ns macchiato.middleware.multipart-params
  (:require [macchiato.util.request :as req]))

;; https://www.npmjs.com/package/multiparty
(def multiparty (js/require "multiparty"))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (= (req/content-type request) "multipart/form-data"))

(defn- parse-opts [{:keys [encoding max-fields-size max-fields max-files-size auto-fields upload-dir]}]
  (clj->js
    (merge
      (when encoding {:encoding encoding})
      (when max-fields-size {:maxFieldsSize max-fields-size})
      (when max-fields {:maxFields max-fields})
      (when max-files-size {:maxFilesSize max-files-size})
      (when auto-fields {:autoFields auto-fields})
      (when upload-dir {:uploadDir upload-dir}))))

(defn- multipart-request [handler request respond raise opts]
  (if (multipart-form? request)
    (let [form (if opts
                 (.form multiparty (parse-opts opts))
                 (.form multiparty))]


      (.parse form (:node/request request)
              (fn [err fields files]
                (if err
                  (raise err)
                  (handler
                    (assoc request
                      :fields fields
                      :files files)
                    respond
                    raise)))))
    request))

(defn wrap-multipart
  ":encoding - sets encoding for the incoming form fields. Defaults to utf8.
  :max-fields-size - Limits the amount of memory all fields (not files) can allocate in bytes. If this value is exceeded, an error event is emitted. The default size is 2MB.
  :max-fields - Limits the number of fields that will be parsed before emitting an error event. A file counts as a field in this case. Defaults to 1000.
  :max-files-size - Only relevant when autoFiles is true. Limits the total bytes accepted for all files combined. If this value is exceeded, an error event is emitted. The default is Infinity.
  :auto-fields - Enables field events and disables part events for fields. This is automatically set to true if you add a field listener.
  :auto-files - Enables file events and disables part events for files. This is automatically set to true if you add a file listener.
  :upload-dir - Only relevant when autoFiles is true. The directory for placing file uploads in. You can move them later using fs.rename(). Defaults to os.tmpDir()."
  [handler & [opts]]
  (fn [request respond raise]
    (handler (multipart-request handler request respond raise opts) respond raise)))
