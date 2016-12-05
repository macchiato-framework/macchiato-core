(ns macchiato.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [macchiato.test.core-test]
    [macchiato.test.content-type]
    [macchiato.test.anti-forgery]
    [macchiato.test.ssl]))

(doo-tests 'macchiato.test.core-test
           'macchiato.test.content-type
           'macchiato.test.anti-forgery
           'macchiato.test.ssl)
