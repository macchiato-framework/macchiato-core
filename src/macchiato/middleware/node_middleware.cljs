(ns macchiato.middleware.node-middleware)

(defn- populate-req-map
  "Translate (js->clj) each property from the node request into the req map."
  [req mappings]
  (let [node-req (:node/request req)]
    (reduce (fn [req [key prop]]
                (let [value (js->clj (aget node-req prop))]
                  (assoc req key value)))
            req mappings)))

(defn wrap-node-middleware
  "Executes the given Node.js middleware function with the original
  :node/request and :node/response objects before calling the handler.
  The :req-map maps properties from the resulting :node/request object to keys
  in the request map passed to the handler (js->clj is applied to the values)."
  [handler node-mw & {:keys [req-map]}]
  (fn [req res raise]
    (let [next (fn [err]
                 (if err
                   (raise err)
                   (handler (populate-req-map req req-map) res raise)))]
      (node-mw (:node/request req) (:node/response req) next))))
