### Macchiato HTTP Core

Macchiato Core implements core Ring 1.6 async handlers and middleware on top of Node.js. The API is kept same as the original Ring API whenever possible.

Current functionality includes:

* Ring HTTP request/response spec
* cookies

Available middleware:

* anti-forgery
* content-type
* file
* flash
* head
* nested-params
* params
* session
* ssl

#### Attribution

Most middleware is ported directly from [Ring core middleware](https://github.com/ring-clojure/ring/tree/master/ring-core/src/ring/middleware).

