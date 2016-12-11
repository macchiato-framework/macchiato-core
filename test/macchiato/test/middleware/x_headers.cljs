(ns macchiato.test.middleware.x-headers
  (:require
    [macchiato.middleware.x-headers :as x-headers
     :refer [content-type-options-response wrap-frame-options frame-options-response wrap-xss-protection wrap-content-type-options xss-protection-response]]
    [macchiato.test.mock.util :refer [mock-handler ok-response raw-response]]
    [macchiato.util.response :refer [ok content-type get-header]]
    [macchiato.test.mock.request :refer [header request]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest test-wrap-frame-options
         (let [handle-hello (ok-response "hello")]
           (testing "deny"
                    (let [handler (mock-handler wrap-frame-options handle-hello :deny)
                          resp    (handler (request :get "/"))]
                      (is (= (:headers resp) {"X-Frame-Options" "DENY"}))))

           (testing "sameorigin"
                    (let [handler (mock-handler wrap-frame-options handle-hello :sameorigin)
                          resp    (handler (request :get "/"))]
                      (is (= (:headers resp) {"X-Frame-Options" "SAMEORIGIN"}))))

           (testing "allow-from"
                    (let [handler (mock-handler wrap-frame-options handle-hello {:allow-from "http://example.com/"})
                          resp    (handler (request :get "/"))]
                      (is (= (:headers resp) {"X-Frame-Options" "ALLOW-FROM http://example.com/"}))))

           #_(testing "bad arguments"
                    (is (thrown? AssertionError (wrap-frame-options handle-hello :foobar)))
                    (is (thrown? AssertionError (wrap-frame-options handle-hello {:allowfrom "foo"})))
                    (is (thrown? AssertionError (wrap-frame-options handle-hello {:allow-from nil}))))

           (testing "response fields"
                    (let [handler (raw-response
                                    (-> (ok "hello")
                                        (content-type "text/plain")))
                          resp    ((mock-handler  wrap-frame-options handler :deny)
                                    (request :get "/"))]
                      (is (= resp {:status  200
                                   :headers {"X-Frame-Options" "DENY"
                                             "Content-Type" "text/plain"}
                                   :body    "hello"}))))

           (testing "nil response"
                    (let [handler (wrap-frame-options (constantly nil) :deny)]
                      (is (nil? (handler (request :get "/"))))))))

(deftest test-frame-options-response
         (testing "deny"
                  (is (= (frame-options-response (ok "hello") :deny)
                         {:status 200, :headers {"X-Frame-Options" "DENY"}, :body "hello"})))

         (testing "nil response"
                  (is (nil? (frame-options-response nil :deny)))))


(deftest test-wrap-content-type-options
         (let [handle-hello (-> (ok "hello") (content-type "text/plain") (raw-response))]
           (testing "nosniff"
                    (let [handler (mock-handler wrap-content-type-options handle-hello :nosniff)
                          resp    (handler (request :get "/"))]
                      (is (= resp {:status  200
                                   :headers {"X-Content-Type-Options" "nosniff"
                                             "Content-Type" "text/plain"}
                                   :body    "hello"}))))

           #_(testing "bad arguments"
                    (is (thrown? AssertionError (wrap-content-type-options handle-hello :foo))))

           (testing "nil response"
                    (let [handler (mock-handler wrap-content-type-options (raw-response nil) :nosniff)]
                      (is (nil? (handler (request :get "/") identity nil)))))))

(deftest test-content-type-options-response
         (testing "nosniff"
                  (is (= (content-type-options-response
                           (-> (ok "hello") (content-type "text/plain"))
                           :nosniff)
                         {:status  200
                          :headers {"X-Content-Type-Options" "nosniff"
                                    "Content-Type" "text/plain"}
                          :body    "hello"})))

         (testing "nil response"
                  (is (nil? (content-type-options-response nil :nosniff)))))

(deftest test-wrap-xss-protection
         (let [handle-hello (ok-response "hello")]
           (testing "enable"
                    (let [handler (mock-handler wrap-xss-protection handle-hello true)
                          resp    (handler (request :get "/"))]
                      (is (= (:headers resp) {"X-XSS-Protection" "1"}))))

           #_(testing "disable"
                    (let [handler (mock-handler wrap-xss-protection handle-hello false)
                          resp    (handler (request :get "/"))]
                      (println "RESP" resp)
                      (is (= (:headers resp) {"X-XSS-Protection" "0"}))))


           ;;TODO mode=block not being set
           #_(testing "enable with block"
                    (let [handler (raw-response
                                    (-> (ok "hello")
                                        (content-type "text/plain")))
                          resp    ((mock-handler wrap-xss-protection handler true {:mode :block})
                                    (request :get "/"))]
                      (is (= resp {:status  200
                                   :headers {"X-XSS-Protection" "1; mode=block"
                                             "Content-Type" "text/plain"}
                                   :body    "hello"}))))

           #_(testing "bad arguments"
                    (is (thrown? AssertionError
                                 (wrap-xss-protection handle-hello true {:mode :blob}))))

           (testing "nil response"
                    (let [handler (mock-handler wrap-xss-protection (raw-response nil) true)]
                      (is (nil? (handler (request :get "/"))))))))

(deftest test-xss-protection-response
         (testing "enable"
                  (is (= (xss-protection-response (ok "hello") :deny)
                         {:status 200, :headers {"X-XSS-Protection" "1"}, :body "hello"})))

         (testing "nil response"
                  (is (nil? (frame-options-response nil :deny)))))
