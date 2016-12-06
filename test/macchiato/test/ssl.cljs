(ns macchiato.test.ssl
  (:require
    [macchiato.middleware.ssl :as ssl]
    [macchiato.util.response :refer [ok get-header]]
    [macchiato.test.mock.request :refer [header request]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest test-wrap-forwarded-scheme
  (let [handler (fn [req res raise]
                  (res (ok (name (:scheme req)))))]
    (let [handler #((ssl/wrap-forwarded-scheme handler) % identity nil)]
      (testing "no header"
        (let [response (handler (request :get "/"))]
          (is (= (:body response) "http")))
        (let [response (handler
                         (request :get "https://localhost/"))]
          (is (= (:body response) "https"))))

      (testing "default header"
        (let [response (handler
                         (-> (request :get "/")
                             (header "x-forwarded-proto" "https")))]
          (is (= (:body response) "https")))
        (let [response (handler
                         (-> (request :get "https://localhost/")
                             (header "x-forwarded-proto" "http")))]
          (is (= (:body response) "http"))))

      (testing "default header"
        (let [response (handler
                         (-> (request :get "/")
                             (header "x-forwarded-proto" "https")))]
          (is (= (:body response) "https")))
        (let [response (handler (-> (request :get "https://localhost/")
                                    (header "x-forwarded-proto" "http")))]
          (is (= (:body response) "http"))))))
  )
