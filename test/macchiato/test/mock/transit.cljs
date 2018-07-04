(ns macchiato.test.mock.transit)

(defrecord MockPoint [x y])

(deftype MockPointWriteHandler []
  Object
  (tag [this v] "point")
  (rep [this v] #js [(.-x v) (.-y v)])
  (stringRep [this v] nil))

(def mock-write-handlers
  {:handlers
   {MockPoint (MockPointWriteHandler.)}})

(def mock-read-handlers
  {:handlers
   {"point" (fn [[x y]] (MockPoint. x y))}})
