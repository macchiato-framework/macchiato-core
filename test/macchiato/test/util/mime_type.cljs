(ns macchiato.test.util.mime-type
  (:require [macchiato.util.mime-type :as mt]
            [cljs.test :refer-macros [is are deftest testing use-fixtures]]))

(deftest ext-mime-type-test
  (testing "default mime types"
    (are [f m] (= (mt/ext-mime-type f) m)
               "foo.txt" "text/plain"
               "foo.html" "text/html"
               "foo.png" "image/png"))
  (testing "custom mime types"
    (is (= (mt/ext-mime-type "foo.bar" {"bar" "application/bar"})
           "application/bar"))
    (is (= (mt/ext-mime-type "foo.txt" {"txt" "application/text"})
           "application/text")))
  (testing "case insensitivity"
    (is (= (mt/ext-mime-type "FOO.TXT") "text/plain"))))


