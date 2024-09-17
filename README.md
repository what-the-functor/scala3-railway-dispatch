# Coding Challenge
Railway dispatching system for the WunderReise GmbH.

## Problem Specification
You’re a railway scheduling expert and you design and implement such systems for a living.
A new customer, WunderReise GmbH ordered a scheduling system for their airport terminal shuttle railway.
All their terminals are connected with parallel railway lines, with one dedicated train per line.
The trains can move back and forth, and they should pick up passengers according to their pickup requests.

Here is an example:

```
   line A |--------------------|------[train]-------|
   line B |-------[train]------|--------------------|
       Terminal A           Terminal B           Terminal C
```

They have the following requirements:
* The scheduling system should be able to handle multiple lines with a maximum of 32.
* The scheduling system should be able to handle multiple terminals with a maximum of 32.
* The scheduling system needs an interface to query the state of all trains. That should include at least the following information: current terminal and direction.
* And of course, your scheduling system needs to be able to process **pick-up** requests.
* Finally, this system should work in discrete steps (time or state steps) and an endpoint is required to move the state forward.
* Your solution should be built as an executable other systems can interact with. How that interaction is designed is up to you. It can be as simple as a CLI.
* You can ignore the train capacity in your solution if you want to.
* You can ignore intermediate positions (between terminals) of a train and just make it go directly from terminal to terminal.


**A pick-up** is an indicator of where a user is currently located (terminal) and to which terminal they want to go.

How are you going to design a system that tries to service pick-up requests as fast as possible? What data structures, interfaces, and algorithms will you need?
And of course you don’t have to overdo it. In the end, we know that you have to work on it next to your daily life. A simple solution is perfectly fine. Should you hit a road block don’t hesitate to ask us.

## Assessment Criteria
We expect that your code is well-factored, without needless duplication, follow good practices and be automatically verified.
What we will look at:
- How clean is your design and implementation, how easy it is to understand and maintain your code.
- How you verified your software, if by automated tests or some other way.

# Instructions

## Getting Started
Requires a java runtime, and a recent SBT.  I used OpenJDK 17, and SBT 1.9.9.
Run the app with `sbt run`, or compile and run the jar.  This starts a server on port 8080.
The available endpoints are:
  - `GET /status` returns the queue count, and status of all lines in the railway,
  - `POST /pickup` enqueues a pickup request, and
  - `POST /step` progresses the state of the railway by one discrete step

## Example workflow

### Initial State
All trains are waiting at terminal 1

```shell
curl localhost:8080/status | jq
```

Response (truncated)
```shell
[
  0,
  {
    "first": {
      "trainLocation": {
        "name": 1
      },
      "trainDirection": "forward",
      "trainPickups": []
    },
    "rest": [
      {
        "trainLocation": {
          "name": 1
        },
        "trainDirection": "forward",
        "trainPickups": []
      },
      {
        "trainLocation": {
          "name": 1
        },
        "trainDirection": "forward",
        "trainPickups": []
      },
      ...
   ]
  }
]
```

### Queue Pickup Requests
```shell
curl -v --header "Content-Type: application/json" --request POST --data '{"a":1, "b": 3}' localhost:8080/pickup
```

The endpoint responds with an empty 202 (accepted), or otherwise responds with a 400 (bad request) containing a helpful error message.
```shell
...
* upload completely sent off: 15 bytes
< HTTP/1.1 202 Accepted
< Date: Tue, 17 Sep 2024 15:06:31 GMT
< Connection: keep-alive
< Content-Length: 0
...
```

```shell
curl -v --header "Content-Type: application/json" --request POST --data '{"a":1, "b": 4}' localhost:8080/pickup
```

```shell
curl -v --header "Content-Type: application/json" --request POST --data '{"a":3, "b": 5}' localhost:8080/pickup
```

### The status report shows 3 enqueued pickup requests

```shell
curl localhost:8080/status | jq
```

Response (truncated)
```shell
[
  3,
  {
  ...
  }
[
```

### Process one step
```shell
curl -v -X POST localhost:8080/step
```

The endpoint responds with an empty 202, or otherwise 500 in case of any internal error.
```shell
* Request completely sent off
< HTTP/1.1 202 Accepted
< Date: Tue, 17 Sep 2024 15:16:11 GMT
< Connection: keep-alive
< Content-Type: text/plain; charset=UTF-8
< Content-Length: 7
<
* Connection #0 to host localhost left intact
Changed
```

### Check the status

The pickups from terminal 1 are embarked on the first train, the pickup from 3 -> 5 is waiting in the queue.
```shell
[
  1,
  {
    "first": {
      "trainLocation": {
        "name": 2
      },
      "trainDirection": "forward",
      "trainPickups": [
        {
          "a": {
            "name": 1
          },
          "b": {
            "name": 3
          }
        },
        {
          "a": {
            "name": 1
          },
          "b": {
            "name": 4
          }
        }
      ]
    },
    ...
  }
]
```

# Process two steps and check the status
```shell
for x in 1 2; do curl -v -X POST localhost:8080/step; done
```

```shell
curl localhost:8080/status | jq
```

The first train is now at terminal 4.  The pickup from 1 -> 3 has disembarked, and the pickup from 3 -> 5 has embarked.
```shell
[
  0,
  {
    "first": {
      "trainLocation": {
        "name": 4
      },
      "trainDirection": "forward",
      "trainPickups": [
        {
          "a": {
            "name": 1
          },
          "b": {
            "name": 4
          }
        },
        {
          "a": {
            "name": 3
          },
          "b": {
            "name": 5
          }
        }
      ]
    },
    ...
  }
]
```


# Analysis and Implementation Detail

## State Model

The railway state is implemented by nested immutable arrays.  The outer array represents the railway, and each array that it contains represents a tram line.

## Pick-up queue
The queue of pick-up requests is implemented in-memory as a pure double-ended-queue (from Cats Effect 3), in-order to avoid global mutable state.  A production implementation would use a persistent queue for robustness, such as Kafka.

## Line
Each line is represented internally by an immutable array, `ArraySeq`.  The immutable array data structure is chosen for it's constant-time random access, in order to retrieve the next, and previous terminal.  Furthormore, a line can be thought of as a very simple graph, a _path_ in graph theory terms.  In the example of a /heap/,  binary-tree can be represented by an array.  Therefore, the /heap/ example shows that an array is a suitable structure to represent a /path/ (a simpler structure than a binary-tree).

In-keeping with functional programming style, an immutable array is used.  Among many benifits, this allays any concurrency concerns, such as race conditions.

A /path/ could also be implemented as a /doubly-linked list/, though Scala does not have a doubly-linked list in the standard API.  When an element is added to a doubly-linked list, the entire list has to be rebuilt.  Even though, in this case, the size of the list would not change, instantiation would leave behind more objects on the heap, than instantiation of an immutable array.

## Railway
The railway is implemented as an immutable array of `Line`s.  There must be at minimum one line, and the maximum size is not explicitly limited.

## Train, Terminal, and Direction
A train is reprented by a product type of `Terminal` and `Direction`.
`Terminal` is a simple data type to represent a terminal by it's name, a positive `Int`.
`Direction` is a sum type: `Forward`, and `Backward`.

## Testing and Assured Correctness
The vector for illegal state is significantly reduced by using type refinement.  For example, `IntPositive` is guaranteed to represent a positive integer, and `Terminal` can only be applied to `IntPositive`.
The /refined types/ are implemented with a pattern of /case classes/ with private constructors, combined with /smart constructors/ which return a disjunction of the refined type, or an error if the input does not conform (i.e. a negative number in the case of `IntPositive`).
For a production implementation it would be advantageous to use a refined type library such as /Iron/, or /Refined/.

The HTTP routing and business logic are loosely coupled, and validated separately.

A production implementation would have more test coverage.

## Complexities
Due to the fact that the line is modeled as an array, the terminals can be accessed randomly by their index in constant time.
A `SortedMap` is used for (near constant time) access to trains by their distance from each pickup request's source terminal.  Quadratic sorting of linear sequences (such as List) is avoided.
Unfortunately in the current implementation, the train distances are computed for each pickup in the queue, on every step.  For example, given a queue size of 10, the distances are comuted 10 times.
It is possible to pre-compute a constant value of all distances from every terminal, however I left that out of this implementation for the sake of time.
