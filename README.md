### Macchiato HTTP Core

[![CircleCI](https://circleci.com/gh/macchiato-framework/macchiato-core.svg?style=svg)](https://circleci.com/gh/macchiato-framework/macchiato-core)

Macchiato Core implements core Ring 1.6 async handlers and middleware on top of Node.js. The API is kept same as the original Ring API whenever possible.

### [API Documentation](https://macchiato-framework.github.io/api/core/index.html)

Available middleware:

* anti-forgery
* content-type
* default-charset
* file
* flash
* head
* keyword-params
* multipart-params
* nested-params
* not-modified
* params
* proxy-headers
* session
* ssl
* x-headers

#### Attribution

Most middleware is ported directly from [Ring core middleware](https://github.com/ring-clojure/ring/tree/master/ring-core/src/ring/middleware).

