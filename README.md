#JHPL - Java High-Performance Library for Lattices

##Background##

In mathematics, a *lattice* is a partially ordered set L in which every two elements have a unique supremum (also called 
a least upper bound) and a unique infimum (also called a greatest lower bound). A bounded lattice is 
a lattice that additionally has a greatest element ```1``` and a least element ```0```, which satisfy ```0 ≤ x ≤ 1``` for 
every ```x in L```. [(Source)](http://en.wikipedia.org/wiki/Lattice_%28order%29)

Lattices can be modeled with *Hasse diagrams*. In order theory, a Hasse diagram is a type of mathematical 
diagram used to represent a finite partially ordered set, in the form of a drawing of its transitive reduction.
[(Source)](http://en.wikipedia.org/wiki/Hasse_diagram)

This library has been designed to model a specific type of bounded lattices, in which *each element is a combination of elements 
from different dimensions*. The elements from each dimension are required to have a *total order*. Natural numbers with a 
given number of digits are a good example. For example, all natural binary numbers with 2 digits form a lattice in which
each dimension can have a value of either ```0``` or ```1``` ```(0 < 1)```. It can be visualized with the following diagram:

```Java
Level-2    (1,1)
           /   \
Level-1 (0,1) (1,0)
           \   /
Level-0    (0,0)
```

The aim of this library is to efficiently represent very large such lattices, while allowing information to be stored 
about individual elements and groups of elements as well as supporting the enumeration of elements with certain properties.  

##Motivation##

The type of lattices modeled by this library can become very large. If a lattice consists of elements with ```n``` dimensions,
where each dimension i (```0 ≤ i < n```) has ```m_i``` different components, the total number of elements is 
```m_0 * m_1 * ... * m_(n-1)```.

The aim of this library is to efficiently (in terms of space and time complexity) represent lattices by storing information
about elements only implicitly. You may, for example, use this library if you have a search problem for which the solution space 
can be expressed as a lattice. JHPL supports lattices with up to ```2³¹-1``` (```~9.223372 * 10¹⁸```) elements. Of course,
having such large lattices only makes sense if you do not need to store information about all elements. In particular, JHPL
supports the concept of *predictive properties*.

A predictive property is a property that is automatically inherited to all (direct and indirect) successors or 
(direct and indirect) predecessors of an element to which it has been assigned. With predictive properties, very large spaces 
can be classified without traversing each element and without explicitly storing information about all elements. 
JHPL supports properties that are inherited to all successors, to all predecessors, or to both successors and predecessors. 
Moreover, JHPL also acts a map which allows to associated objects with individual elements, if required.

##Overview##

JHPL supports lattices in which each dimension consists of a set of objects (optionally of a given type). For example, the following
lattice represents binary numbers with two digits, where each digit is represented by a string:

```Java
// Elements per dimension
String[][] elements = new String[][]{ {"0", "1"}, 
                                      {"0", "1"}};
        
// Create lattice with String-keys and Integer-values 
// (the type of objects that may be assigned to individual elements)
Lattice<String, Integer> lattice = new Lattice<String, Integer>(elements); 
```

###Spaces###

Elements from lattices may be represented in three different ```spaces```:

- The *source space*: This space is meant to provide natural representations of elements, e.g., ```(A, B, C)```
- The *id space*:     In this space, each element is represented by an identifier, which is a positive long value. You may use
                      this representation to store larger sets of elements or to use them as keys in maps.
- The *index space*:  This space represents elements by indices that correspond to the position of each element in the
                      source space. For example, the index representation of ```("0", "1")``` is ```(0, 1)```. 
                      *All interactions with this library are performed in the index space*.

Methods for converting between the different spaces are encapsulated in a class that is accessible via the method
```lattice.space()```. Some examples:

```Java
// Example element
String[] element = new String[]{"0", "1"};

// Convert source to id 
long id = lattice.space().toId(element);

// Convert source to index
int[] index = lattice.space().toIndex(element);

// Convert index to source
element = lattice.space().toSource(index);

// Etc.
```

Additionally, the class provides methods for converting iterators:
- ```Iterator<Long> indexIteratorToIdIterator(Iterator<int[]>)```
- ```Iterator<T[]> indexIteratorToSourceIterator(Iterator<int[]>)```
- ```Iterator<int[]> idIteratorToIndexIterator(Iterator<Long>)```
- ```Iterator<T[]> idIteratorToSourceIterator(Iterator<Long>)```
- ```Iterator<int[]> sourceIteratorToIndexIterator(Iterator<T[]>)```
- ```Iterator<Long> sourceIteratorToIdIterator(Iterator<T[]>)```

Methods for working with nodes are encapsulated in a class that is accessible via the method
```lattice.nodes()```. Some examples:

```Java
// Top and bottom elements
int[] top = lattice.nodes().getTop();
int[] bottom = lattice.nodes().getBottom();

// Iterate over neighbors
Iterator<int[]> successors = lattice.nodes().listSuccessors(bottom);

// Check relationships
boolean direct = lattices.nodes().isDirectParentChild(bottom, top);
```

*Note:* Never assume that the integer arrays returned by this library are newly allocated! Never manipulate them and don't store
any references to them. If you need to keep track of a set of elements: use the id space. You may use efficient implementations
of collections of primitive values, as, e.g., provided by the [HPPC project](https://github.com/carrotsearch/hppc). Example:

```Java
// List all successors of the bottom element
Iterator<int[]> iter = lattice.nodes().listSuccessors(lattices.nodes.getBottom());

// Store references
LongArrayList list = new LongArrayList();
while (iter.hasNext()) {
	list.add(iter.next());
}

// Work with the elements from the set
for (long e : list) {
	int[] index = lattice.space().toIndex(e);
	boolean stored = lattice.contains(index);
}
```

###Storing data###

You may use this library to assign data or predictive properties to elements. Predictive properties are implemented in an
according class and may have an optional label. 

```Java
// Associated to an element and all direct and indirect successors
PredictiveProperty property1 = new PredictiveProperty(Direction.UP);

// Associated to an element and all direct and indirect predecessors
PredictiveProperty property2 = new PredictiveProperty(Direction.DOWN);

// Associated to an element and all direct and indirect successors and predecessors
PredictiveProperty property3 = new PredictiveProperty(Direction.BOTH);
```

The methods provided by the class ```Lattice``` are optimized for read access and have the following run-time complexities:

- ```getData(node)```: Retrieves the associated data. Guaranteed *O(1)*.
- ```putData(node)```: Associates data with a node. Guaranteed *O(1)*.
- ```contains(node)```: Returns whether any data is stored about a node. Guaranteed *O(1)*.
- ```hasProperty(node)```: Returns whether any property is associated with a node. Guaranteed *O(1)*.
- ```hasProperty(node, property)```: Determines whether a node is associated with a (predictive) property. Guaranteed *O(1)*.
- ```putProperty(node, property)```: Associates a node and predecessors or successors with a (predictive) property. 
     The worst-case run-time complexity of this operation is *O(#nodes for which put has already been called with this property)*.
     Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
     a complexity of *amortized O(1)*.
- ```removeProperty(node, property)```: Removes the association between a node and predecessors or successors with a (predictive) property. 
     The worst-case run-time complexity of this operation is *O(#nodes for which put has been called with this property)*.
     Depending on your access pattern (e.g. sequential in terms of a path from bottom to top), it may be reduced up to 
     a complexity of *amortized O(1)*.

###Enumerating elements###

JHPL provides two different ways of access to elements. Firstly, it allows accessing elements about which information
has been *explicitly* stored (i.e. for which ```putData()``` or ```putProperty()``` has been called). These 
methods are safe to call at any time:

- ```listNodes()```: Enumerates all nodes stored in the lattice
- ```listNodes(level)```: Enumerates all nodes stored on the given level

Secondly, JHPL also provides methods for accessing elements about which only *implicit* information is available. These methods
are encapsulated in an object that is accessible via the method ```lattice.unsafe()```. These methods may not be safe to call
because their worst-case complexity is bound by the total number of nodes in the lattice. You should therefore only call them
for "small" lattices (e.g., with up to a few million elements):

- ```listAllNodes()```: List all nodes in the lattice.
- ```listNodesNotStored()```: List all nodes for which no data is stored in the lattice.
- ```listNodesWithProperty()```: Lists all nodes which are associated with a property (also predictively).
- ```listNodesWithProperty(property)```: Lists all nodes which are associated with the given property (also predictively).
- ```listNodesWithPropertyOrStored()```: Lists all nodes which are associated with a property or for which data is stored.
- ```listNodesWithoutProperty()```: Lists all nodes which are not associated with any property.
- ```listNodesWithoutProperty(property)```: Lists all nodes which are not associated with the given property.
- ```listNodesWithoutPropertyAndNotStored()```: Lists all nodes which are not associated with any property and for which not data is stored.

*Note:* All of these methods support an optional parameter with which the *level* of the nodes that are to be returned
may be specified.

*Note:* Similar methods are also provided for listing successors and predecessors with certain conditions
(e.g. ```lattice.nodes().listSuccessorsWithoutProperty(node)```). These are safe to call at any time.

*Note:* Methods from this library do not support concurrent modifications. An according exception will be raised.

*Note:* Methods from this library are not thread-safe.

##Details and Evaluation##

###How it works###

JHPL uses [tries](http://en.wikipedia.org/wiki/Trie) to store implicit information about the contained elements. It manages 
one trie per property (and direction) as well as a "master" trie for all elements. The tries use the index representation of the 
components of an element and they are serialized into integer arrays. When checking whether a given element has a certain property, 
the according trie is traversed while comparing elements with greater-than-or-equals for properties that are inherited to 
predecessors or with less-than-or-equals for properties that are inherited to successors. This scheme works, as long as the 
following two conditions are met:

1. It is made sure that no properties are stored for elements that already have the given property. Counterexample:
 - Assume property A is predictive in an upwards direction.
 - We first add property A for (1, 2, 1)
 - We then add property A for (1, 3, 25). 
 - We query for (1, 3, 20) with <= and the result will be ```false``` (which is wrong).
2. It is made sure that all obsolete properties are removed. Counterexample:
 - Assume property A is predictive in an upwards direction.
 - We first add property A for (1, 3, 25)
 - We then add property A for (1, 2, 1). 
 - We query for (1, 3, 20) with <= and the result will be ```false``` (which is wrong).

The following output shows the in-memory representation of a lattice over the dimensions
({"A", "B", "C", "D"}, {"A", "B"}, {"A", "B", "C"}) for which put has been called with an upwards-predictive property
for nodes ("B", "B", "C") and ("A", "A", "A"):

```Java
Lattice
├── Upwards-predictive properties
|   └── Property1
|       └── Trie
|           ├── Memory statistics
|           |   ├── Allocated: 76 [bytes]
|           |   ├── Used: 36 [bytes]
|           |   └── Relative: 47.36842 [%]
|           ├── Buffer
|           |   └── [9, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, -1, 0, 0, 0, 0, 0, 0, 0]
|           └── Tree
|               ├── [0]
|               │   └── [0]
|               │       └── [0]
|               └── [EOT]
├── Master
|   └── Trie
|       ├── Memory statistics
|       |   ├── Allocated: 76 [bytes]
|       |   ├── Used: 56 [bytes]
|       |   └── Relative: 73.68421 [%]
|       ├── Buffer
|       |   └── [9, 4, 0, 0, 0, 6, 0, 0, -1, 11, 0, -1, 0, 0, 0, 0, 0, 0, 0]
|       └── Tree
|           ├── [0]
|           │   ├── [0]
|           │   │   └── [0]
|           ├── [1]
|           │   └── [1]
|           │       └── [2]
|           └── [EOT]
└── Memory: 424 [bytes]
```

The difference in byte sizes (152 bytes for both tries vs. 424 bytes for the overall structure) is due to an additional
hash table that may be used for associating data to elements.

###Some numbers###

Measured with a Lenovo Thinkpad T440s on Ubuntu 14.04 with an Oracle JVM 1.7.0 (rev. 72)

####Space complexity####

The following table shows a comparison of the in-memory size of lattices with 10¹ (ten) to 10⁷ (one billion) elements. Each
lattice has 1 to 7 dimensions with 10 elements per dimension. The lattices have been materialized with a call to
```lattice.unsafe().materialize()``` which is a shortcut for calling ```putData()``` on all elements in the lattice.

| #Elements        | Size           | Time   | Naive size |
| ---------------: | --------------:| ------:| ----------:|
| 10¹=10           | 324 B          | 0 ms   | 1.1 kB     |
| 10²=100          | 836 B          | 0 ms   | 13.3 kB    |
| 10³=1000         | 6.7 kB         | 1 ms   | 151.2 kB   |
| 10⁴=10000        | 48.8 kB        | 3 ms   | 1.7 MB     |
| 10⁵=100000       | 533.3 kB       | 5 ms   | 18.8 MB    |
| 10⁶=1000000      | 6.3 MB         | 35 ms  | 206.4 MB   |
| 10⁷=10000000     | 47.8 MB        | 300 ms | 2.2 GB     |

Per element, the naive implementation maintains one integer-array representing its components as well as pointers to its 
successors and predecessors.

####Run-time complexity####

####Storing predictive properties####

The following numbers show the time needed to assign a predictive property to all nodes of a lattice with 1 million elements
(1 million calls to ```putProperty()```). The best-case performance simply needs to check whether the property already exists 
and the worst-case performance needs to check, clear the lattice and set the property.
- Best-case: 188 milliseconds (188 nanoseconds per put-operation)
- Worst-case: 564 milliseconds (564 nanoseconds per put-operation) 

####Enumerating elements####

The following numbers show the time needed to enumerate all elements from a materialized lattice with 1 million elements. 

#####1. Enumerating the elements level by level (in a natural order)#####

```Java
for (int level=0; level<lattice.numLevels(); level++) {
	processAll(lattice.listNodes(level));
}
```
This requires ~200ms with a maximum of 8 ms per level (55 generalization levels in total).

#####2. Enumerating all elements (in a natural order)#####

```Java
processAll(lattice.listNodes());
```
This requires ~25ms.

####Putting properties and enumerating elements####

This is a more complex experiment. First, we create a lattice with 1 million elements. When then create five predictive properties,
two of which are inherited to successors, two of which are inherited to predecessors and one of which is inherited to successors and
predecessors. 

#####1. Setting the properties#####

We associate each property to 10.000 random elements (50.000 put operations). This takes ~240 ms. 
The resulting lattice consumes about 3.5 MB of space.

#####2. Listing all nodes with any property level-by-level#####

For each level, we enumerate over all elements that are associated with *any* property. Additionally, we perform a space
mapping by calling ```toId(element)``` for all elements returned by the iterators. This requires ~300 ms and returns 980.133 
elements, meaning that roughly 98% of the whole lattice have been characterized by at least one property during step 1:

```Java
for (int level = 0; level < lattice.numLevels(); level++) {
	Iterator<int[]> iter = lattice.unsafe().listNodesWithProperty();
	processAll(lattice.space().indexIteratorToIdIterator(iter));
}
```

Calling ```listNodesWithoutProperty()``` exhibits comparable performance.

#####3. Listing nodes with a specific property level-by-level#####

For each level and each property, we enumerate over all elements that are associated with the property. 
Additionally, we perform a space mapping by calling toId(element) for all elements returned by the iterators. 
This requires ~1200 ms and returns 2.674.978 elements, meaning that on average, each element is associated
with 2.7 properties:

```Java
for (PredictiveProperty property : properties) { 
	for (int level = 0; level < lattice.numLevels(); level++) {
	Iterator<int[]> iter = lattice.unsafe().listNodesWithProperty(property);
		processAll(lattice.space().indexIteratorToIdIterator(iter));
	}
}
```

#####4. Listing all nodes without any property#####

Again, we also perform space mapping:

```Java
Iterator<int[]> iter = lattice.unsafe().listNodesWithoutProperty();
processAll(lattice.space().indexIteratorToIdIterator(iter));
```

This takes ~150 ms, compared to the 300 ms required for enumerating the elements level-by-level.

##Download##
A binary version (JAR file) is available for download [here](https://rawgithub.com/prasser/jhpl/master/jars/jhpl-0.0.1.jar).

The according Javadoc is available for download [here](https://rawgithub.com/prasser/jhpl/master/jars/jhpl-0.0.1-doc.jar). 

##Documentation##
Online documentation can be found [here](https://rawgithub.com/prasser/jhpl/master/doc/index.html).

##License##
Apache 2.0
