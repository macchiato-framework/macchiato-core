(ns macchiato.test.middleware.proxy-headers
  (:require
    [macchiato.middleware.proxy-headers :refer [wrap-forwarded-remote-addr]]
    [macchiato.test.mock.util :refer [mock-handler ok-response raw-response]]
    [macchiato.util.response :refer [ok content-type get-header]]
    [macchiato.test.mock.request :refer [header request]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest test-wrap-forwarded-remote-addr
         (let [handler (mock-handler wrap-forwarded-remote-addr (comp ok :remote-addr))]
           (testing "without x-forwarded-for"
                    (let [req  (assoc (request :get "/") :remote-addr "1.2.3.4")
                          resp (handler req)]
                      (is (= (:body resp) "1.2.3.4"))))

           (testing "with x-forwarded-for"
                    (let [req  (-> (request :get "/")
                                   (assoc :remote-addr "127.0.0.1")
                                   (header "x-forwarded-for" "1.2.3.4"))
                          resp (handler req)]
                      (is (= (:body resp) "1.2.3.4"))))

           (testing "with multiple proxies"
                    (let [req  (-> (request :get "/")
                                   (assoc :remote-addr "127.0.0.1")
                                   (header "x-forwarded-for" "10.0.1.9, 192.168.4.98, 1.2.3.4"))
                          resp (handler req)]
                      (is (= (:body resp) "1.2.3.4"))))))

