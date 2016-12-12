(ns macchiato.test.middleware.params
  (:require
    [macchiato.middleware.params :refer [wrap-params]]
    [macchiato.test.mock.request :refer [header request]]
    [macchiato.test.mock.util :refer [mock-handler raw-response ok-response]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(defn wrapped-echo [req]
  ((mock-handler wrap-params (fn [req res rais] (res req)))
    req))

(deftest wrap-params-query-params-only
  (let [req  {:query-string "foo=bar&biz=bat%25"}
        resp (wrapped-echo req)]
    (is (= {"foo" "bar" "biz" "bat%"} (:query-params resp)))
    (is (empty? (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))

(deftest wrap-params-query-and-form-params
  (let [req  {:query-string "foo=bar"
              :headers      {"content-type" "application/x-www-form-urlencoded"}
              :body         "biz=bat%25"}
        resp (wrapped-echo req)]
    (is (= {"foo" "bar"}  (:query-params resp)))
    (is (= {"biz" "bat%"} (:form-params resp)))
    (is (= {"foo" "bar" "biz" "bat%"} (:params resp)))))


(deftest wrap-params-not-form-encoded
  (let [req  {:headers {"content-type" "application/json"}
              :body    "{foo: \"bar\"}"}
        resp (wrapped-echo req)]
    (is (empty? (:form-params resp)))
    (is (empty? (:params resp)))))


(deftest wrap-params-always-assocs-maps
  (let [req  {:query-string ""
              :headers      {"content-type" "application/x-www-form-urlencoded"}
              :body         ""}
        resp (wrapped-echo req)]
    (is (= {} (:query-params resp)))
    (is (= {} (:form-params resp)))
    (is (= {} (:params resp)))))
