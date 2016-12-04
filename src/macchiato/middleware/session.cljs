(ns macchiato.middleware.session
  (:require [macchiato.session.memory :as mem]
            [macchiato.session.store :as store]))

(defn- session-options
  [options]
  {:store        (options :store (mem/memory-store))
   :cookie-name  (options :cookie-name "macchiato-session")
   :cookie-attrs (merge {:path      "/"
                         :http-only true}
                        (options :cookie-attrs)
                        (if-let [root (options :root)]
                          {:path root}))})

(defn- bare-session-request
  [request {:keys [store cookie-name]}]
  (let [req-key     (get-in request [:cookies cookie-name :value])
        session     (store/read-session store req-key)
        session-key (if session req-key)]
    (merge request {:session     (or session {})
                    :session/key session-key})))

(defn session-request
  ([request]
   (session-request request {}))
  ([request options]
   (-> request (bare-session-request options))))

(defn- bare-session-response
  [{:keys [session-cookie-attrs] :as response} {session-key :session/key} {:keys [store cookie-name cookie-attrs]}]
  (let [new-session-key (if (contains? response :session)
                          (if-let [session (response :session)]
                            (if (:recreate (meta session))
                              (do
                                (store/delete-session store session-key)
                                (->> (vary-meta session dissoc :recreate)
                                     (store/write-session store nil)))
                              (store/write-session store session-key session))
                            (if session-key
                              (store/delete-session store session-key))))
        cookie          {cookie-name
                         (merge cookie-attrs
                                session-cookie-attrs
                                {:value (or new-session-key session-key)})}
        response        (dissoc response :session :session-cookie-attrs)]
    (if (or (and new-session-key (not= session-key new-session-key))
            (and session-cookie-attrs (or new-session-key session-key)))
      (update response :cookies merge cookie)
      response)))

(defn session-response
  ([request response options]
   (fn [response-map]
     (some-> response-map (bare-session-response request options) response))))

(defn wrap-session
  ([handler]
   (wrap-session handler {}))
  ([handler options]
   (let [options (session-options options)]
     (fn
       ([request respond raise]
        (let [request (session-request request options)]
          (handler request (session-response request respond options) raise)))))))
