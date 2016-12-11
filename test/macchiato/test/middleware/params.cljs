(ns macchiato.test.middleware.params
  (:require
    [macchiato.middleware.params :refer [wrap-params]]
    [macchiato.test.mock.request :refer [header request]]
    [macchiato.test.mock.util :refer [mock-handler raw-response ok-response]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(def wrapped-echo (wrap-params identity))

(deftest wrap-params-query-params-only
  (let [req  {:query-string "foo=bar&biz=bat%25"}
        resp (mock-handler wrapped-echo req)]
    (is (= {"foo" "bar" "biz" "bat%"} (:query-params resp)))
    (is (empty? (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))

