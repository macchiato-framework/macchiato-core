(ns macchiato.test.middleware.node-middleware
  (:require
   [macchiato.middleware.node-middleware :refer [wrap-node-middleware]]
   [cljs.test :refer-macros [is deftest testing]]))

(defn fake-node-middleware
  "Sets properties in the req and res js objects."
  [node-req node-res next]
  (aset node-req "user" "john")
  (aset node-res "param" "value")
  (next))

(defn error-node-middleware
  "Calls next with an error."
  [node-req node-res next]
  (next (js/Error "something went wrong!")))

(defn echo [req res raise] (res req))

(deftest node-middleware-modify-request-response
  (let [node-req (js-obj)
        node-res (js-obj)
        req {:node/request node-req
             :node/response node-res}
        res identity
        error-handler (fn [err] (aset node-res "error" (.-message err)))
        wrapped-echo (-> echo (wrap-node-middleware fake-node-middleware))
        result (wrapped-echo req res error-handler)]
    (is (= nil (aget (:node/response req) "error")))
    (is (= "john" (aget (:node/request req) "user")))
    (is (= "value" (aget (:node/response req) "param")))
    (is (= req result))))

(deftest node-middleware-map-request-properties
  (let [node-req (js-obj)
        node-res (js-obj)
        req {:node/request node-req
             :node/response node-res}
        res identity
        error-handler (fn [err] (aset node-res "error" (.-message err)))
        wrapped-echo (-> echo (wrap-node-middleware fake-node-middleware :req-map {:user "user" :other "user"}))
        result (wrapped-echo req res error-handler)]
    (is (= nil (aget (:node/response req) "error")))
    (is (= "john" (:user result)))
    (is (= "john" (:other result)))))

(deftest node-middleware-raise-error
  (let [node-req (js-obj)
        node-res (js-obj)
        req {:node/request node-req
             :node/response node-res}
        res identity
        error-handler (fn [err] (aset node-res "error" (.-message err)))
        wrapped-echo (-> echo (wrap-node-middleware error-node-middleware))
        result (wrapped-echo req res error-handler)]
    (is (= "something went wrong!" (aget (:node/response req) "error")))))
