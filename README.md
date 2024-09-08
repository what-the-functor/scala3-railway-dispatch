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

## Analysis and Implementation Detail

# State Model

The railway state is implemented by nested immutable arrays.  The outer array represents the railway, and each array that it contains represents a tram line.

## Line
Each line is represented internally by an immutable array, `ArraySeq`.  The immutable array data structure is chosen for it's constant-time random access, in order to retrieve the next, and previous terminal.  Furthormore, a line can be thought of as a very simple graph, a _path_ in graph theory terms.  In the example of a /heap/,  binary-tree can be represented by an array.  Therefore, the /heap/ example shows that an array is a suitable structure to represent a /path/ (a simpler structure than a binary-tree).

In-keeping with functional programming style, an immutable array is used.  Among many benifits, this allays any concurrency concerns, such as race conditions.

A /path/ could also be implemented as a /doubly-linked list/, though Scala does not have a doubly-linked list in the standard API.  When an element is added to a doubly-linked list, the entire list has to be rebuilt.  Even though, in this case, the size of the list would not change, instantiation would leave behind more objects on the heap, than instantiation of the immutable array.

## Railway
The railway is implemented as an immutable array of `Line`s.  There must be at minimum one line, and the maximum size is not explicitly limited.

## Train, Terminal, and Direction
A train is reprented by a product type of `Terminal` and `Direction`.
`Terminal` is a simple data type to represent a terminal by it's name, a positive `Int`.
`Direction` is a sum type: `Forward`, and `Backward`.




