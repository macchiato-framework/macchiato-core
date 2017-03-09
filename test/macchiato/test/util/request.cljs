(ns macchiato.test.util.request
  (:require
    [macchiato.util.request
     :refer [accept
             request-url
             content-type
             content-length
             character-encoding
             urlencoded-form?
             body-string
             in-context?
             set-context]]
    [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest test-request-url
  (is (= (request-url {:scheme :http
                       :uri "/foo/bar"
                       :headers {"host" "example.com"}
                       :query-string "x=y"})
         "http://example.com/foo/bar?x=y"))
  (is (= (request-url {:scheme :http
                       :uri "/"
                       :headers {"host" "localhost:8080"}})
         "http://localhost:8080/"))
  (is (= (request-url {:scheme :https
                       :uri "/index.html"
                       :headers {"host" "www.example.com"}})
         "https://www.example.com/index.html")))

(deftest accept-header
  (is
    (= [{:type "application", :sub-type "json", :level "1", :q 0.4}]
       (accept "application/json;level=1;q=0.4")))
  (is
    (= [{:type "application", :sub-type "json", :q 1}]
       (accept "application/json")))
  (is
    (= [{:type "application", :sub-type "transit+json", :q 1}]
       (accept "application/transit+json")))
  (is
    (= [{:type "text", :sub-type "x-c", :q 1}
        {:type "text", :sub-type "x-dvi", :q 0.8, :mxb "100000", :mxt "5.0"}]
       (accept "text/x-dvi; q=.8; mxb=100000; mxt=5.0, text/x-c"))))

(deftest test-content-type
  (testing "no content-type"
    (is (= (content-type {:headers {}}) nil)))
  (testing "content type with no charset"
    (is (= (content-type {:headers {"content-type" "text/plain"}}) "text/plain")))
  (testing "content type with charset"
    (is (= (content-type {:headers {"content-type" "text/plain; charset=UTF-8"}})
           "text/plain"))))

(deftest test-content-length
  (testing "no content-length header"
    (is (= (content-length {:headers {}}) nil)))
  (testing "a content-length header"
    (is (= (content-length {:headers {"content-length" "1337"}}) 1337))))

(deftest test-character-encoding
  (testing "no content-type"
    (is (= (character-encoding {:headers {}}) nil)))
  (testing "content-type with no charset"
    (is (= (character-encoding {:headers {"content-type" "text/plain"}}) nil)))
  (testing "content-type with charset"
    (is (= (character-encoding {:headers {"content-type" "text/plain; charset=UTF-8"}})
           "UTF-8"))
    (is (= (character-encoding {:headers {"content-type" "text/plain;charset=UTF-8"}})
           "UTF-8"))))

(deftest test-urlencoded-form?
  (testing "urlencoded form"
    (is (urlencoded-form? {:headers {"content-type" "application/x-www-form-urlencoded"}}))
    (is (urlencoded-form?
          {:headers {"content-type" "application/x-www-form-urlencoded; charset=UTF-8"}})))
  (testing "other content type"
    (is (not (urlencoded-form? {:headers {"content-type" "application/json"}}))))
  (testing "no content type"
    (is (not (urlencoded-form? {:headers {}})))))

(deftest test-body-string
  (testing "nil body"
    (is (= (body-string {:body nil}) nil)))
  (testing "string body"
    (is (= (body-string {:body "foo"}) "foo"))))

(deftest test-in-context?
  (is (in-context? {:uri "/foo/bar"} "/foo"))
  (is (not (in-context? {:uri "/foo/bar"} "/bar"))))

(deftest test-set-context
  (is (= (set-context {:uri "/foo/bar"} "/foo")
         {:uri "/foo/bar"
          :context "/foo"
          :path-info "/bar"}))
  (try (set-context {:uri "/foo/bar"} "/bar")
       (is ("shouldn't be here" :not-ok))
       (catch js/Error _)))
