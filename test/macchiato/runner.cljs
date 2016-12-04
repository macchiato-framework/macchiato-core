(ns macchiato.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [macchiato.core-test]))

(doo-tests 'macchiato.core-test)
