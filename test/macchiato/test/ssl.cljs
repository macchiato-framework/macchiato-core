(ns macchiato.test.ssl
  (:require
    [macchiato.middleware.ssl :as ssl]
    [macchiato.test.mock.util :refer [mock-handler ok-response]]
    [macchiato.util.response :refer [ok get-header]]
    [macchiato.test.mock.request :refer [header request]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest test-wrap-forwarded-scheme
  (let [handler (fn [req res raise]
                  (res (ok (name (:scheme req)))))]
    (let [handler (mock-handler ssl/wrap-forwarded-scheme handler)]
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
          (is (= (:body response) "http")))))
    (testing "custom header"
      (let [handler  (mock-handler ssl/wrap-forwarded-scheme handler "X-Foo")
            response (handler (-> (request :get "/")
                                  (header "x-foo" "https")))]
        (is (= (:body response) "https"))))))

(deftest test-wrap-ssl-redirect
  (let [handler (mock-handler
                  ssl/wrap-ssl-redirect
                  (ok-response ""))]
    (testing "HTTP GET request"
      (let [response (handler (request :get "/"))]
        (is (= (:status response) 301))
        (is (= (get-header response "location") "https://localhost/"))))

    (testing "HTTP POST request"
      (let [response (handler (request :post "/"))]
        (is (= (:status response) 307))
        (is (= (get-header response "location") "https://localhost/"))))

    (testing "HTTPS request"
      (let [response (handler (request :get "https://localhost/"))]

        (is (= (:status response) 200))
        (is (nil? (get-header response "location"))))))

  (let [handler (mock-handler
                  ssl/wrap-ssl-redirect
                  (ok-response "")
                  {:ssl-port 8443})]
    (testing "HTTP GET request with custom SSL port"
      (let [response (handler (request :get "/"))]
        (is (= (:status response) 301))
        (is (= (get-header response "location") "https://localhost:8443/"))))

    (testing "HTTP POST request with custom SSL port"
      (let [response (handler (request :post "/"))]
        (is (= (:status response) 307))
        (is (= (get-header response "location") "https://localhost:8443/"))))))

(deftest test-wrap-hsts
  (testing "no matching handler"
    (let [handler  (mock-handler ssl/wrap-hsts (fn [req res raise] nil))
          response (handler (request :get "/not-found"))]
      (is (nil? response))))

  (testing "defaults"
    (let [handler  (mock-handler ssl/wrap-hsts (ok-response ""))
          response (handler (request :get "/"))]
      (is (= (get-header response "strict-transport-security")
             "max-age=31536000; includeSubDomains"))))

  (testing "custom max-age"
    (let [handler  (mock-handler ssl/wrap-hsts (ok-response "") {:max-age 0})
          response (handler (request :get "/"))]
      (is (= (get-header response "strict-transport-security")
             "max-age=0; includeSubDomains"))))

  (testing "don't include subdomains"
    (let [handler  (mock-handler ssl/wrap-hsts (ok-response "") {:include-subdomains? false})
          response (handler (request :get "/"))]
      (is (= (get-header response "strict-transport-security")
             "max-age=31536000")))))
