(ns macchiato.middleware
  (:require
    [clojure.set :refer [difference]]))

(defn- update-middleware-meta [handler handler-middleware middleware-meta]
  (with-meta
    handler
    {:macchiato/middleware
     (conj handler-middleware middleware-meta)}))

(defn validate
  "middleware metadata can contain the following keys
  :id - id of the middleware function
  :required - a collection of the ids of middleware functions it requires to be present"
  [handler-middleware
   {:keys [id required] :as middleware-meta}]
  (when (not-empty (difference (set required) (set (map :id handler-middleware))))
    (throw (js/Error. (str id " is missing required middleware: " required))))
  middleware-meta)

(defn- middleware-from-handler [handler]
  (->> handler meta :macchiato/middleware (remove nil?) vec))

(defn validate-handler [handler]
  (let [middleware (middleware-from-handler handler)]
    (loop [[middleware-meta & handler-middleware] middleware]
      (when middleware-meta
        (validate handler-middleware middleware-meta)
        (recur handler-middleware)))
    handler))

(defn- loaded? [middleware {:keys [id]}]
  (some #{id} (map :id middleware)))

(defn wrap
  ([handler middleware-fn]
   (wrap handler middleware-fn nil))
  ([handler middleware-fn opts]
   (let [handler-middleware (middleware-from-handler handler)
         middleware-meta    (meta middleware-fn)
         middleware-info    (or (:macchiato/middleware middleware-meta)
                                {:id (keyword (:name middleware-meta))})]
     (if (loaded? handler-middleware middleware-info)
       handler
       (update-middleware-meta
         (if opts
           (middleware-fn handler opts)
           (middleware-fn handler))
         handler-middleware
         middleware-info)))))

(defn- middleware-id [middleware]
  (-> (if (coll? middleware) (first middleware) middleware) meta :macchiato/middleware))

(defn- check-conflicting-middleware [handler-middleware middleware]
  (when-let [conflicting-middleware (not-empty
                                      (filter
                                        #(loaded?
                                           handler-middleware
                                           (middleware-id %))
                                        middleware))]
    (throw (js/Error. (str "following middleware has already been wrapped:"
                           (map #(-> % middleware-id :id) conflicting-middleware))))))

(defn wrap-middleware [handler & middleware]
  (check-conflicting-middleware (middleware-from-handler handler) middleware)
  (validate-handler
    (reduce
      (fn [handler middleware-fn]
        (let [[middleware-fn opts] (if (coll? middleware-fn) middleware-fn [middleware-fn])]
          (wrap handler middleware-fn opts)))
      handler
      middleware)))

