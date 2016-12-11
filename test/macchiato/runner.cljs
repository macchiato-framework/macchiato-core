(ns macchiato.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [macchiato.test.core-test]
    [macchiato.test.middleware.anti-forgery]
    [macchiato.test.middleware.content-type]
    [macchiato.test.middleware.flash]
    [macchiato.test.middleware.session]
    [macchiato.test.middleware.ssl]
    [macchiato.test.middleware.x-headers]
    [macchiato.test.util.mime-type]
    [macchiato.test.util.request]
    [macchiato.test.util.response]))

(doo-tests 'macchiato.test.core-test
           'macchiato.test.middleware.anti-forgery
           'macchiato.test.middleware.content-type
           'macchiato.test.middleware.flash
           'macchiato.test.middleware.session
           'macchiato.test.middleware.ssl
           'macchiato.test.util.mime-type
           'macchiato.test.middleware.x-headers
           'macchiato.test.util.request
           'macchiato.test.util.response)
