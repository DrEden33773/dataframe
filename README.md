# Kotlin Dataframe: typesafe in-memory structured data processing for JVM
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Kotlin component alpha stability](https://img.shields.io/badge/project-alpha-kotlin.svg?colorA=555555&colorB=DB3683&label=&logo=kotlin&logoColor=ffffff&logoWidth=10)](https://kotlinlang.org/docs/components-stability.html)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Dynamic XML Badge](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fjetbrains%2Fkotlinx%2Fdataframe%2Fmaven-metadata.xml&query=%2F%2Fversion%5Bnot(contains(text()%2C%22dev%22&#41;&#41;%5D%5Blast()%5D&label=Release%20version)](https://search.maven.org/artifact/org.jetbrains.kotlinx/dataframe)
[![Dynamic XML Badge](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Forg%2Fjetbrains%2Fkotlinx%2Fdataframe%2Fmaven-metadata.xml&query=%2F%2Fversion%5Bcontains(text()%2C%22dev%22&#41;%5D%5Blast()%5D&label=Dev%20version&color=yellow)](https://search.maven.org/artifact/org.jetbrains.kotlinx/dataframe)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Binder](https://mybinder.org/badge_logo.svg)](https://mybinder.org/v2/gh/Kotlin/dataframe/HEAD)

Kotlin Dataframe aims to reconcile Kotlin's static typing with the dynamic nature of data by utilizing both the full power of the Kotlin language and the opportunities provided by intermittent code execution in Jupyter notebooks and REPL.   

* **Hierarchical** — represents hierarchical data structures, such as JSON or a tree of JVM objects.
* **Functional** — data processing pipeline is organized in a chain of `DataFrame` transformation operations. Every operation returns a new instance of `DataFrame` reusing underlying storage wherever it's possible.
* **Readable** — data transformation operations are defined in DSL close to natural language.
* **Practical** — provides simple solutions for common problems and the ability to perform complex tasks.
* **Minimalistic** — simple, yet powerful data model of three column kinds.
* **Interoperable** — convertable with Kotlin data classes and collections.
* **Generic** — can store objects of any type, not only numbers or strings.
* **Typesafe** — on-the-fly generation of extension properties for type safe data access with Kotlin-style care for null safety.
* **Polymorphic** — type compatibility derives from column schema compatibility. You can define a function that requires a special subset of columns in dataframe but doesn't care about other columns.

Integrates with [Kotlin kernel for Jupyter](https://github.com/Kotlin/kotlin-jupyter). Inspired by [krangl](https://github.com/holgerbrandl/krangl), Kotlin Collections and [pandas](https://pandas.pydata.org/)

## Documentation

Explore [**documentation**](https://kotlin.github.io/dataframe/overview.html) for details.

You could find the following articles there:

* [Get started with Kotlin DataFrame](https://kotlin.github.io/dataframe/gettingstarted.html)
* [Working with Data Schemas](https://kotlin.github.io/dataframe/schemas.html)
* [Full list of all supported operations](https://kotlin.github.io/dataframe/operations.html)
    * [Reading from SQL databases](https://kotlin.github.io/dataframe/readsqldatabases.html)
    * [Reading/writing from/to different file formats like JSON, CSV, Apache Arrow](https://kotlin.github.io/dataframe/read.html)
    * [Joining a few dataframes](https://kotlin.github.io/dataframe/join.html)
    * [GroupBy operation](https://kotlin.github.io/dataframe/groupby.html)
* [Rendering to HTML](https://kotlin.github.io/dataframe/tohtml.html#jupyter-notebooks)

## Setup

```kotlin
implementation("org.jetbrains.kotlinx:dataframe:0.13.1")
```

Optional Gradle plugin for enhanced type safety and schema generation
https://kotlin.github.io/dataframe/schemasgradle.html
```kotlin
id("org.jetbrains.kotlinx.dataframe") version "0.13.1"
```

Check out the [custom setup page](https://kotlin.github.io/dataframe/gettingstartedgradleadvanced.html) if you don't need some of the formats as dependencies,
for Groovy, and for configurations specific to Android projects.

## Getting started

```kotlin
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
```

```kotlin
val df = DataFrame.read("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
df["full_name"][0] // Indexing https://kotlin.github.io/dataframe/access.html

df.filter { "stargazers_count"<Int>() > 50 }.print() 
```

## Getting started with data schema

Requires Gradle plugin to work
```kotlin
id("org.jetbrains.kotlinx.dataframe") version "0.13.1"
```

Plugin generates extension properties API for provided sample of data. Column names and their types become discoverable in completion.

```kotlin
// Make sure to place the file annotation above the package directive
@file:ImportDataSchema(
    "Repository",
    "https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv",
)

package example

import org.jetbrains.kotlinx.dataframe.annotations.ImportDataSchema
import org.jetbrains.kotlinx.dataframe.api.*

fun main() {
    // execute `assemble` to generate extension properties API
    val df = Repository.readCSV()
    df.fullName[0]
    
    df.filter { stargazersCount > 50 }
}
```

## Getting started in Jupyter Notebook / Kotlin Notebook

Install [Kotlin kernel](https://github.com/Kotlin/kotlin-jupyter) for [Jupyter](https://jupyter.org/)

Import stable `dataframe` version into notebook: 
```
%use dataframe
```
or specific version:
```
%use dataframe(<version>)
```

```kotlin
val df = DataFrame.read("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
df // the last expression in the cell is displayed
```

When a cell with a variable declaration is executed, in the next cell `DataFrame` provides extension properties based on its data 
```kotlin
df.filter { stargazers_count > 50 }
```

## Data model
* `DataFrame` is a list of columns with equal sizes and distinct names.
* `DataColumn` is a named list of values. Can be one of three kinds:
  * `ValueColumn` — contains data
  * `ColumnGroup` — contains columns
  * `FrameColumn` — contains dataframes

## Syntax example

Let us show you how data cleaning and aggregation pipelines could look like with DataFrame.

**Create:**
```kotlin
// create columns
val fromTo by columnOf("LoNDon_paris", "MAdrid_miLAN", "londON_StockhOlm", "Budapest_PaRis", "Brussels_londOn")
val flightNumber by columnOf(10045.0, Double.NaN, 10065.0, Double.NaN, 10085.0)
val recentDelays by columnOf("23,47", null, "24, 43, 87", "13", "67, 32")
val airline by columnOf("KLM(!)", "{Air France} (12)", "(British Airways. )", "12. Air France", "'Swiss Air'")

// create dataframe
val df = dataFrameOf(fromTo, flightNumber, recentDelays, airline)

// print dataframe
df.print()
```

**Clean:**
```kotlin
// typed accessors for columns
// that will appear during
// dataframe transformation
val origin by column<String>()
val destination by column<String>()

val clean = df
    // fill missing flight numbers
    .fillNA { flightNumber }.with { prev()!!.flightNumber + 10 }

    // convert flight numbers to int
    .convert { flightNumber }.toInt()

    // clean 'airline' column
    .update { airline }.with { "([a-zA-Z\\s]+)".toRegex().find(it)?.value ?: "" }

    // split 'fromTo' column into 'origin' and 'destination'
    .split { fromTo }.by("_").into(origin, destination)

    // clean 'origin' and 'destination' columns
    .update { origin and destination }.with { it.lowercase().replaceFirstChar(Char::uppercase) }

    // split lists of delays in 'recentDelays' into separate columns
    // 'delay1', 'delay2'... and nest them inside original column `recentDelays`
    .split { recentDelays }.inward { "delay$it" }

    // convert string values in `delay1`, `delay2` into ints
    .parse { recentDelays }
```

**Aggregate:**
```kotlin
clean
    // group by the flight origin renamed into "from"
    .groupBy { origin named "from" }.aggregate {
        // we are in the context of a single data group

        // total number of flights from origin
        count() into "count"

        // list of flight numbers
        flightNumber into "flight numbers"

        // counts of flights per airline
        airline.valueCounts() into "airlines"

        // max delay across all delays in `delay1` and `delay2`
        recentDelays.maxOrNull { delay1 and delay2 } into "major delay"

        // separate lists of recent delays for `delay1`, `delay2` and `delay3`
        recentDelays.implode(dropNA = true) into "recent delays"

        // total delay per destination
        pivot { destination }.sum { recentDelays.colsOf<Int?>() } into "total delays to"
    }
```

Check it out on [**Datalore**](https://datalore.jetbrains.com/view/notebook/vq5j45KWkYiSQnACA2Ymij) to get a better visual impression of what happens and what the hierarchical DataFrame structure looks like. 

Explore [**more examples here**](examples).

## Kotlin, Kotlin Jupyter, OpenAPI, Arrow and JDK versions

This table shows the mapping between main library component versions and minimum supported Java versions.

| Kotlin DataFrame Version | Minimum Java Version | Kotlin Version | Kotlin Jupyter Version | OpenAPI version | Apache Arrow version |
|--------------------------|----------------------|----------------|------------------------|-----------------|----------------------|
| 0.10.0                   | 8                    | 1.8.20         | 0.11.0-358             | 3.0.0           | 11.0.0               |
| 0.10.1                   | 8                    | 1.8.20         | 0.11.0-358             | 3.0.0           | 11.0.0               |
| 0.11.0                   | 8                    | 1.8.20         | 0.11.0-358             | 3.0.0           | 11.0.0               |
| 0.11.1                   | 8                    | 1.8.20         | 0.11.0-358             | 3.0.0           | 11.0.0               |
| 0.12.0                   | 8                    | 1.9.0          | 0.11.0-358             | 3.0.0           | 11.0.0               |
| 0.12.1                   | 8                    | 1.9.0          | 0.11.0-358             | 3.0.0           | 11.0.0               |
| 0.13.1                   | 8                    | 1.9.22         | 0.12.0-139             | 3.0.0           | 15.0.0               |

## Code of Conduct

This project and the corresponding community are governed by the [JetBrains Open Source and Community Code of Conduct](https://confluence.jetbrains.com/display/ALL/JetBrains+Open+Source+and+Community+Code+of+Conduct). Please make sure you read it.

## License

Kotlin Dataframe is licensed under the [Apache 2.0 License](LICENSE).
