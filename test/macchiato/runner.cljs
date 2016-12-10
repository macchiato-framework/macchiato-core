(ns macchiato.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [macchiato.test.anti-forgery]
    [macchiato.test.content-type]
    [macchiato.test.core-test]
    [macchiato.test.flash]
    [macchiato.test.ssl]
    [macchiato.test.util.mime-type]
    [macchiato.test.util.request]))

(doo-tests 'macchiato.test.anti-forgery
           'macchiato.test.content-type
           'macchiato.test.core-test
           'macchiato.test.flash
           'macchiato.test.ssl
           'macchiato.test.util.mime-type
           'macchiato.test.util.request)
