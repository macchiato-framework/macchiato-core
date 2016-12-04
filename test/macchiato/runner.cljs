(ns macchiato.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [macchiato.test.core-test]
    [macchiato.test.content-type]))

(doo-tests 'macchiato.test.core-test
           'macchiato.test.content-type)
