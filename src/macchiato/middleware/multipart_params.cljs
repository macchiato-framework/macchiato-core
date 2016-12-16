(ns macchiato.middleware.multipart-params
  (:require
    [macchiato.middleware.nested-params :as np]
    [macchiato.util.request :as req]))

;; https://www.npmjs.com/package/multiparty
(def multiparty (js/require "multiparty"))

(defn- multipart-form?
  "Does a request have a multipart form?"
  [request]
  (= (req/content-type request) "multipart/form-data"))

(defn- parse-opts [{:keys [encoding max-fields-size max-fields max-files-size upload-dir]}]
  (clj->js
    (merge
      {:autoFields true
       :autoFiles  true}
      (when encoding {:encoding encoding})
      (when max-fields-size {:maxFieldsSize max-fields-size})
      (when max-fields {:maxFields max-fields})
      (when max-files-size {:maxFilesSize max-files-size})
      (when upload-dir {:uploadDir upload-dir}))))

(defn- parse-params [fields]
  (np/nest-params
    (reduce
      (fn [params k]
        (assoc params [k] (js->clj (aget fields k))))
      {} (js/Object.keys fields))
    identity))

(defn- parse-file-param [file-param]
  (let [{:keys [originalFilename path size headers]} (js->clj file-param :keywordize-keys true)]
    {:filename     originalFilename
     :tempfile     path
     :size         size
     :content-type (:content-type headers)}))

(defn- parse-file-params [files]
  (reduce
    (fn [params k]
      (assoc params k (map parse-file-param (aget files k))))
    {} (js/Object.keys files)))

(defn- multipart-request [handler request respond raise {:keys [progress-fn] :as opts}]
  (let [form (multiparty.Form. (parse-opts opts))]
    (when progress-fn
      (.on form "progress" progress-fn))
    (.parse form (:body request)
            (fn [err fields files]
              (let [params (merge
                             (parse-file-params files)
                             (parse-params fields))]
                (if err
                  (raise err)
                  (handler
                    (assoc request
                      :multipart-params params
                      :params params)
                    respond
                    raise)))))))

(defn wrap-multipart
  ":encoding - sets encoding for the incoming form fields. Defaults to utf8.
  :max-fields-size - Limits the amount of memory all fields (not files) can allocate in bytes. If this value is exceeded, an error event is emitted. The default size is 2MB.
  :max-fields - Limits the number of fields that will be parsed before emitting an error event. A file counts as a field in this case. Defaults to 1000.
  :max-files-size - Only relevant when autoFiles is true. Limits the total bytes accepted for all files combined. If this value is exceeded, an error event is emitted. The default is Infinity.
  :upload-dir - Only relevant when autoFiles is true. The directory for placing file uploads in. Defaults to (.tmpDir os).
  :progress-fn - function that will be called when bytes are received, should expect two fields: bytes-eeceived, bytes-expected"
  [handler & [opts]]
  (fn [request respond raise]
    (if (multipart-form? request)
      (let [opts (assoc opts :encoding (or (:encoding opts) (req/character-encoding request) "utf8"))]
        (multipart-request handler request respond raise opts))
      (handler request respond raise))))
