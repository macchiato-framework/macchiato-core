(ns macchiato.test.util.response
  (:require
    [macchiato.util.response :as r]
    [cognitect.transit :as transit]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest test-content-type
  (is (= {:status 200 :headers {"Content-Type" "text/html" "Content-Length" "10"}}
         (r/content-type {:status 200 :headers {"Content-Length" "10"}}
                       "text/html"))))

(deftest test-charset
  (testing "add charset"
    (is (= (r/charset {:status 200 :headers {"Content-Type" "text/html"}} "UTF-8")
           {:status 200 :headers {"Content-Type" "text/html; charset=UTF-8"}})))
  (testing "replace existing charset"
    (is (= (r/charset {:status 200 :headers {"Content-Type" "text/html; charset=UTF-16"}}
                    "UTF-8")
           {:status 200 :headers {"Content-Type" "text/html; charset=UTF-8"}})))
  (testing "default content-type"
    (is (= (r/charset {:status 200 :headers {}} "UTF-8")
           {:status 200 :headers {"Content-Type" "text/plain; charset=UTF-8"}})))
  (testing "case insensitive"
    (is (= (r/charset {:status 200 :headers {"content-type" "text/html"}} "UTF-8")
           {:status 200 :headers {"content-type" "text/html; charset=UTF-8"}}))))

(deftest test-find-header
         (is (= (r/find-header {:headers {"Content-Type" "text/plain"}} "Content-Type")
                ["Content-Type" "text/plain"]))
         (is (= (r/find-header {:headers {"content-type" "text/plain"}} "Content-Type")
                ["content-type" "text/plain"]))
         (is (= (r/find-header {:headers {"Content-typE" "text/plain"}} "content-type")
                ["Content-typE" "text/plain"]))
         (is (nil? (r/find-header {:headers {"Content-Type" "text/plain"}} "content-length"))))

(deftest test-get-header
         (is (= (r/get-header {:headers {"Content-Type" "text/plain"}} "Content-Type")
                "text/plain"))
         (is (= (r/get-header {:headers {"content-type" "text/plain"}} "Content-Type")
                "text/plain"))
         (is (= (r/get-header {:headers {"Content-typE" "text/plain"}} "content-type")
                "text/plain"))
         (is (nil? (r/get-header {:headers {"Content-Type" "text/plain"}} "content-length"))))

(deftest test-update-header
  (is (= (r/update-header {:headers {"Content-Type" "text/plain"}}
                        "content-type"
                        str "; charset=UTF-8")
         {:headers {"Content-Type" "text/plain; charset=UTF-8"}}))
  (is (= (r/update-header {:headers {}}
                        "content-type"
                        str "; charset=UTF-8")
         {:headers {"content-type" "; charset=UTF-8"}})))

(deftest test-transit-response
  (let [some-edn {:test :value :message "Hello!"}
        resp (r/transit some-edn)
        decoded (transit/read (transit/reader :json) (:body resp))]
    (is (= some-edn decoded))))
