# Architecture Decision Record for Ring Spec 2.0

This is a record of the decisions that have gone into the Ring 2.0
specification. As of writing, the specification is still a draft, and
should not be considered finalized.

## Keyword Namespaces

The most obvious change in the 2.0 draft specification is that the
keys for request and response maps are now namespaced.

This change updates the request and response maps to be more in line
with the future direction of Clojure. Core libraries such as Spec, and
tools from Cognitect such as Datomic make extensive use of namespaced
keywords. In addition the language itself has moved toward more
support for namespaced keywords, such as in the recent changes
destructuring, and the syntax sugar for maps that have keys with a
common namespace.

Namespaced keywords also make request and response maps unambiguous.
If a map contains `:ring.request/method`, then it must be a request
map; if a map contains `:ring.response/status` then it must be a
response.

It's also possible that we can namespace keys added via middleware,
therefore distinguishing between keys from the adapter, and keys
derived via calculation such as cookies or URL-encoded parameters.

## Backward Compatibility

An key benefit to being able to identify requests and responses is
that we can retain backward compatibility across middleware and
utility functions. We can efficiently distinguish between requests
that adhere to the 1.x specification, and those that adhere to the 2.x
specification, and invoke different behavior depending on the type of
input.

As an example:

```clojure
(defn wrap-example [handler]
  (let [handler-1 (wrap-example-1 handler)
        handler-2 (wrap-example-2 handler)]
    (fn [request]
      (if (:ring.request/method request)
        (handler-2 request)
        (handler-1 request)))))
```

In informal benchmarks, looking up a key in a small request map was
around 6ns. This is relatively little overhead, even across a chain of
middleware functions. Additionally this should lend itself to hardware
branch prediction, as it's expected that an application should use one
version of the Ring spec.

## Required vs. Optional Keys

In the Ring 1 spec there were a number of required keys for both
request and response maps. In the 2.0 spec, this is reduced to one
required key for requests, and one required key for responses.

Example:

```clojure
#:ring.request{:method :get}

#:ring.response{:status 200}
```

Having at least one mandatory key is necessary for efficient
identification of requests and reponses. However, too many
requirements make it difficult to build requests that may have partial
information.

### Required Request Maps Keys

In Ring 1, the `:server-port`, `:server-name` and `:remove-addr`
keys are required. These provide useful information about the
underlying TCP connection, but a HTTP request could be delivered
through a Unix socket, or stored in a file. These aren't necessarily
part of what makes a HTTP request, so they shouldn't be required.

The next required key in Ring 1 is `:uri`. This represents the path
component of the request URI (see the **Key Changes** section). A HTTP
request does not necessarily include a path to a resource. The request
URI, later referred to as the request target, may be "*" or an
authority. For example:

```
OPTIONS * HTTP/1.1
```

And:

```
CONNECT www.example.com:443 HTTP/1.1
```

Again, this means the path should not be required.

The `:scheme` key is required in Ring 1, but again this isn't always
known, as a HTTP request may be recorded in a file or passed through a
proxy. Therefore this key is made optional.


The `:request-method` key is required in Ring 1, and will be required
for Ring 2 also. It's both a madantory part of all HTTP requests,
whatever the version of the protocol, and it's a necessary element to
check. Methods like `HEAD` and `OPTIONS` will always behave
differently to other methods.

The `:protocol` key is part of the request line in HTTP 1.1, and also
part of the connection preface of HTTP/2. It's a known quantity, but
in general not a useful one. We rarely need to know the protocol used
in order to process a HTTP request, and in general there protocol of
the request doesn't affect the content of the response. For this
reason, the protocol is optional in Ring 2.

Finally, the `:headers` key is required in Ring 1, but in Ring 2 it
will be optional, as an empty map of headers can be considered
equivalent to a lack of any header key in the request.

### Required Response Map Keys

The response map in Ring 1 has two required keys: `:status` and
`:headers`.

The status is a mandatory part of a HTTP response, so it
remains a required key in Ring 2.

The headers key will be optional, for the same reasons request headers
have been made optional.

## Namespace Aliasing

Both request and response maps in Ring 2 have protocols that describe
the request and response body. These protocols could be placed in
other namespaces, but as they're a key part of their respective maps,
it's more logical and convenient to place them in `ring.request` and
`ring.response` namespaces

As a side benefit, this allows developers to take advantage of
namespace aliasing to build more concise response maps.

```clojure
(require '[ring.response :as resp])
         
(defn handler [request]
  #::resp{:status  200
          :headers {"content-type" ["text/plain"]}
          :body    "Hello World"})
```

## Key Changes

### :ring.request/body

In Ring 1, the request body is an `java.io.InputStream` object. In
Ring 2, the request body must at least satisfy the
`ring.request/StreamingRequestBody` protocol.

A request body may choose to satify other protocols. In future, a
protocol for asynchronous reads of the request body will be
introduced. However, the fallback of a streaming request protocol will
always be available, at least for any JVM implementations of Ring.

This limits compatibility with platforms that use asynchronous I/O
exclusively, such as ClojureScript. However, we have reader
conditionals for just such cases, and these would need to be used
anyway to selectively remove Ring's synchronous handlers in middleware
and the like.

Having a streaming protocol to fall back on on the JVM is useful for
both being able to quickly read in the request body without requiring
a callback, and because Java servlets also use an `InputStream` to
represent the request body, even for the asynchronous I/O introduced
in the Servlet 3.1 specification.

In practice, this change will make reading the request body slightly
more verbose. Contrast a handler written for Ring 1:

```clojure
(defn handler [request]
  (let [name (-> request :body slurp)] 
    {:status 200, :body (str "Hello " name)}))
```

With a handler written for Ring 2:

  
```clojure
(require '[ring.request :as req]
         '[ring.response :as resp])

(defn handler [request]
  (let [name (-> request :body req/get-body-stream slurp)]
    #::resp{:status 200, :body (str "Hello " name)})
```

While this is somewhat harder to write, it does break Ring's reliance
on blocking I/O. It also allows us to write requests for tests more
easily, as strings can be made to satisfy the protocol. So In Ring 1:

```clojure
(require '[ring.util.io :as rio])

{:request-method :post
 :uri "/"
 :headers {}
 :body (rio/string-input-stream "Hello World")}
```

While in Ring 2:

```clojure
(require '[ring.request :as req])


#::req{:method :post
       :path "/"
       :body "Hello World"}
```

### :ring.request/headers

In Ring 1, request headers are `String` to `String` maps that relate
lowercased header names with their corresponding values.

In HTTP, there can be multiple headers with the same name. For
example:

```
Accept: text/html
Accept: application/json
```

This is equivalent to separating header values with a comma:

```
Accept: text/html, application/json
```

And as header names are case insensitive, this is equivalent to:

```
accept: text/html, application/json
```

In Ring 1, all headers are normalized to this format:

```clojure
{"accept" "text/html, application/json"}
```

The lowercased header names allow for map lookup, and by concatenating
header values we can ensure that all values are strings, making it
easier for developers to deal with the most common case, where a
header will have a single value.

However, this approach prioritizes 'easy' over 'simple'. If there are
multiple headers they have to be joined and then later split apart
once again. And from a semantic point of view, a string is a poor
representation of an ordered collection of values.

In Ring 2, we map a lowercase `String` to a vector of `String`s:

```clojure
{"accept" ["text/html" "application/json"]}
```

This applies even in the single case:

```clojure
{"content-type" ["text/html"]}
```

This does make accessing request headers with single values slightly
more laborious:

```clojure
(first (get-in request [:headers "content-type"]))
```

However, this disadvantage can be offset by utility functions:

```clojure
(get-header request :content-type)
```

It also ensures that you don't accidentally start processing a
concatenated list of header values.

Still to be determined: whether or not an adapter should automatically
split apart concantenated header values.

### :ring.request/method

In Ring 1 this is named `:request-method`. In Ring 2 we can drop the
`request-` part as that is included in the namespace.

### :ring.request/path

In Ring 1 this was named `:uri`. This corresponds to the
`getRequestURI` method of the `HttpServletRequest` class.

However, despite the name, the `getRequestURI` method does not
directly correspond to the `Request-URI` detailed in [RFC 2616][], as
one might expect. Instead, this represents the *path* component of the
request URI, as defined in [RFC 3986][]

In [RFC 7230][] the `Request-URI` is renamed to the `request-target`,
and in HTTP/2 the request target is split into `:path` and
`:authority` pseudo-headers (see [RFC 7540][]).

Given all this, it makes most sense to refer to change the `:uri` key
to `:ring.request/path` in Ring 2, as that's more consistent with
terminology in both the URI RFC and the HTTP/2 RFC.

It should be noted that the `:path` HTTP/2 pseudo-header includes the
query string, while the `path` of a URI excludes it. As the query
string is split out by default by most server implementations, and is
most useful to be considered separately, Ring 2 will keep the path and
query separate.

[RFC 2616]: https://tools.ietf.org/html/rfc2616#section-5.1.2
[RFC 7230]: https://tools.ietf.org/html/rfc7230#section-3.1.1
[RFC 7540]: https://tools.ietf.org/html/rfc7540#section-8.1.2.3

### :ring.request/query

In Ring 1 this is named `:query-string`. In [RFC 3986][] this is
referred to as the `query`. For consistency, and because the `-string`
part is somewhat redundant, in Ring 2 this key has been renamed to
`:ring.request/query`.

[RFC 3986]: https://tools.ietf.org/html/rfc3986#section-3

### :ring.response/headers

The Ring 1 specification is very permissable when it comes to response
headers. The header name need not be lowercase, and the header value
may be a string or a vector of strings.

For example, this is a permissible reponse header map:

```clojure
{"x-foo" "1"
 "X-Foo" "2"
 "X-foo" ["3" "4"]}
```

This makes it hard to build middleware that modify the response
headers. By contrast, Ring 2 mandates that response header names be
lowercase, and the values be vectors:

```clojure
{"x-foo" ["1" "2" "3" "4"]}
```

This also ensures that response and request headers have a single,
consistent format.

In HTTP/1 all header names are case insensitive, and in HTTP/2 header
names have to be lowercased.

## Websockets

TBD

## Push Notifications

TBD