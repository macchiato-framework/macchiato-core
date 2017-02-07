### Macchiato HTTP Core

[![CircleCI](https://circleci.com/gh/macchiato-framework/macchiato-core.svg?style=svg)](https://circleci.com/gh/macchiato-framework/macchiato-core)

[![Clojars Project](https://img.shields.io/clojars/v/macchiato/core.svg)](https://clojars.org/macchiato/core)


Macchiato Core implements core Ring 1.6 async handlers and middleware on top of Node.js. The API is kept same as the original Ring API whenever possible.

### Getting Started

Getting up and running is easy, simply create a new project using [Leiningen](http://leiningen.org/) and follow the instructions:

    lein new macchiato myapp

See [here](https://github.com/macchiato-framework/examples) for some example projects to get started.

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

