package org.jetbrains.kotlinx.dataframe.person

import io.kotest.matchers.shouldBe
import org.jetbrains.kotlinx.dataframe.ColumnsContainer
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.DataRow
import org.jetbrains.kotlinx.dataframe.Many
import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.associate
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.columns
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.drop
import org.jetbrains.kotlinx.dataframe.api.dropNulls
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.gather
import org.jetbrains.kotlinx.dataframe.api.getColumnGroup
import org.jetbrains.kotlinx.dataframe.api.getColumnsWithPaths
import org.jetbrains.kotlinx.dataframe.api.group
import org.jetbrains.kotlinx.dataframe.api.groupBy
import org.jetbrains.kotlinx.dataframe.api.groupByOther
import org.jetbrains.kotlinx.dataframe.api.into
import org.jetbrains.kotlinx.dataframe.api.isMany
import org.jetbrains.kotlinx.dataframe.api.join
import org.jetbrains.kotlinx.dataframe.api.last
import org.jetbrains.kotlinx.dataframe.api.map
import org.jetbrains.kotlinx.dataframe.api.mapKeys
import org.jetbrains.kotlinx.dataframe.api.mapValues
import org.jetbrains.kotlinx.dataframe.api.matches
import org.jetbrains.kotlinx.dataframe.api.mergeRows
import org.jetbrains.kotlinx.dataframe.api.named
import org.jetbrains.kotlinx.dataframe.api.notNull
import org.jetbrains.kotlinx.dataframe.api.pivot
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.remove
import org.jetbrains.kotlinx.dataframe.api.replace
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.sumOf
import org.jetbrains.kotlinx.dataframe.api.toDataRow
import org.jetbrains.kotlinx.dataframe.api.toInt
import org.jetbrains.kotlinx.dataframe.api.toMany
import org.jetbrains.kotlinx.dataframe.api.ungroup
import org.jetbrains.kotlinx.dataframe.api.update
import org.jetbrains.kotlinx.dataframe.api.values
import org.jetbrains.kotlinx.dataframe.api.where
import org.jetbrains.kotlinx.dataframe.api.with
import org.jetbrains.kotlinx.dataframe.column
import org.jetbrains.kotlinx.dataframe.columnOf
import org.jetbrains.kotlinx.dataframe.columns.ColumnKind
import org.jetbrains.kotlinx.dataframe.columns.ndistinct
import org.jetbrains.kotlinx.dataframe.dataFrameOf
import org.jetbrains.kotlinx.dataframe.get
import org.jetbrains.kotlinx.dataframe.impl.columns.asColumnGroup
import org.jetbrains.kotlinx.dataframe.impl.getType
import org.jetbrains.kotlinx.dataframe.manyOf
import org.jetbrains.kotlinx.dataframe.newColumn
import org.jetbrains.kotlinx.dataframe.typeClass
import org.jetbrains.kotlinx.dataframe.values
import org.junit.Test
import java.io.Serializable
import java.util.AbstractCollection
import java.util.AbstractList
import java.util.AbstractSet
import kotlin.reflect.KClass

class PivotTests {

    val df = dataFrameOf("name", "key", "value")(
        "Alice", "age", 15,
        "Alice", "city", "London",
        "Alice", "weight", 54,
        "Bob", "age", 45,
        "Bob", "weight", 87,
        "Mark", "age", 20,
        "Mark", "city", "Moscow",
        "Mark", "weight", null,
        "Alice", "age", 55,
    )

    val defaultExpected = dataFrameOf("name", "age", "city", "weight")(
        "Alice", manyOf(15, 55), "London", 54,
        "Bob", manyOf(45), "-", 87,
        "Mark", manyOf(20), "Moscow", "-"
    )

// Generated Code

    @DataSchema
    interface Person {
        val name: String
        val key: String
        val value: Any?
    }

    val DataRow<Person>.name get() = this["name"] as String
    val DataRow<Person>.key get() = this["key"] as String
    val DataRow<Person>.value get() = this["value"] as Any?
    val ColumnsContainer<Person>.name get() = this["name"].cast<String>()
    val ColumnsContainer<Person>.key get() = this["key"].cast<String>()
    val ColumnsContainer<Person>.value get() = this["value"].cast<Any?>()

    val typed: DataFrame<Person> = df.cast()

    val name by column<String>()
    val key by column<String>()
    val value by column<Any?>()

// Tests

    val keyConverter: (String) -> String = { "__$it" }
    val valueConverter: (Any?) -> Any? = { (it as? Int)?.toDouble() ?: it }

    val expectedFiltered = typed.dropNulls { value }.sortBy { name and key }

    @Test
    fun `pivot matches`() {
        val filtered = typed.drop(1)
        val res = filtered.pivot { key }.groupBy { name }.matches()
        res.ncol() shouldBe 1 + filtered.key.ndistinct()
        res.nrow() shouldBe filtered.name.ndistinct()

        val expected = filtered.map { (name to key) }.toSet()
        val actual = res.columns().subList(1, res.ncol()).flatMap {
            val columnName = it.name()
            res.map {
                val value = it[columnName] as Boolean
                if (value) {
                    (it.name to columnName)
                } else null
            }.filterNotNull()
        }.toSet()

        actual shouldBe expected
        res["age"].type() shouldBe getType<Boolean>()
        res["city"].type() shouldBe getType<Boolean>()
        res["weight"].type() shouldBe getType<Boolean>()
    }

    @Test
    fun `simple pivot`() {
        val res = typed.pivot { key }.groupBy { name }.values { value default "-" }

        res.ncol() shouldBe 1 + typed.key.ndistinct()
        res.nrow() shouldBe typed.name.ndistinct()

        res["age"].type() shouldBe getType<Many<Int>>()
        res["city"].type() shouldBe getType<String>()
        res["weight"].type() shouldBe getType<Serializable>()

        res shouldBe defaultExpected

        typed.pivot { key }.groupBy { name }.default("-").values { value } shouldBe res
        typed.pivot { key }.groupBy { name }.default("-").with { value } shouldBe res
        df.pivot { key }.groupBy { name }.default("-").values { value } shouldBe res
        df.pivot(key).groupBy(name).default("-").values(value) shouldBe res
        df.pivot(key).groupBy(name).default("-").with { value } shouldBe res
        typed.groupBy { name }.pivot { key }.default("-").values { value } shouldBe res

        typed.pivot { key }.groupBy { name }.default("-").with { value.toString() }
    }

    @Test
    fun `pivot with transform`() {
        val pivoted = typed.pivot { key.map { "_$it" } }.groupBy { name }.with { value }
        pivoted.columnNames().drop(1).toSet() shouldBe typed.key.distinct().map { "_$it" }.toSet()
    }

    @Test
    fun `pivot with index transform`() {
        val pivoted = typed.pivot { key }.groupBy { name.map { "_$it" } }.with { value }
        pivoted.name shouldBe typed.name.distinct().map { "_$it" }
    }

    @Test
    fun `pivot with value map`() {
        val pivoted = typed.pivot { key }.groupBy { name }.values { value.map { "_$it" } }

        pivoted shouldBe dataFrameOf("name", "age", "city", "weight")(
            "Alice", manyOf("_15", "_55"), "_London", "_54",
            "Bob", manyOf("_45"), null, "_87",
            "Mark", manyOf("_20"), "_Moscow", "_null"
        )
    }

    @Test
    fun `pivot two values`() {
        val pivoted = typed.pivot { key }.groupBy { name }.values { value and "str" { value?.toString() } default "-" }
        pivoted.print()

        val expected = defaultExpected.replace("age", "city", "weight").with {
            columnOf(
                it named "value",
                it.map {
                    if (it is Many<*>) it.map { it?.toString() }.toMany()
                    else it?.toString()
                } named "str"
            ) named it.name()
        }
        expected.print()

        pivoted shouldBe expected
    }

    @Test
    fun `pivot two values group by value`() {
        val type by column<KClass<*>?>()
        val pivoted = typed.add(type) { value?.javaClass?.kotlin }
            .pivot { key }.groupBy { name }.values(separate = true) { value and type }
        pivoted.print()
        pivoted.ncol() shouldBe 3
    }

    @Test
    fun `pivot two columns with then`() {
        val pivoted = typed.add("index") { 1 }.pivot { name then key }.groupBy("index").with { value }

        pivoted.columnNames() shouldBe listOf("index") + typed.name.distinct().values()
        pivoted.nrow() shouldBe 1

        val keys = typed.key.distinct().values()
        pivoted.columns().drop(1).forEach {
            val group = it.asColumnGroup()
            group.columnNames() shouldBe if (it.name() == "Bob") keys - "city" else keys
        }

        val leafColumns = pivoted.getColumnsWithPaths { all().drop(1).dfs() }
        leafColumns.size shouldBe typed.name.ndistinct() * typed.key.ndistinct() - 1
        leafColumns.forEach { it.path.size shouldBe 2 }

        val data = leafColumns.associate { it.path[0] to it.path[1] to it.data[0] }
        val expected = typed.associate { name to key to value }.toMutableMap()
        expected["Alice" to "age"] = manyOf(15, 55)
        data shouldBe expected

        val pivotedNoIndex = typed.pivot { name then key }.with { value }
        pivotedNoIndex shouldBe pivoted.remove("index")[0]
    }

    @Test
    fun `pivot two columns with and`() {
        val withIndex = typed.add("index") { 1 }
        val pivoted = withIndex.pivot { name and key }.groupBy("index").with { value }
        pivoted shouldBe
            withIndex.pivot(inward = true) { name }.groupBy("index").with { value }.join(withIndex.pivot(inward = true) { key }.groupBy("index").with { value })

        val pivotedNoIndex = typed.pivot { name and key }.with { value }
        pivotedNoIndex shouldBe pivoted.remove("index")[0]
    }

    @Test
    fun `pivot with two index columns`() {
        val pivoted = typed.dropNulls { value }.pivot { value.map { it!!.javaClass.kotlin.simpleName } }
            .groupBy { name and key }.with { value }

        val expected = typed.dropNulls { value }.add {
            "Int" { value as? Int }
            "String" { value as? String }
        }.remove("value").mergeRows("Int", dropNulls = true)

        pivoted shouldBe expected
    }

    @Test
    fun `pivot two values without index`() {
        val pivoted = typed.pivot { name then key }.values { value and (value.map { it?.javaClass?.kotlin } named "type") }

        pivoted.ncol() shouldBe typed.name.ndistinct()

        val cols = pivoted.df().columns { all().dfs() }
        cols.size shouldBe 2 * typed.name.ndistinct() * typed.key.ndistinct() - 2
        cols.forEach {
            when {
                it.isMany() -> it.path().dropLast(1) shouldBe listOf("Alice", "age")
                it.hasNulls() -> {
                    it.path().dropLast(1) shouldBe listOf("Mark", "weight")
                    it.typeClass shouldBe Any::class
                }
                it.name() == "type" -> it.typeClass shouldBe KClass::class
                else -> it.name() shouldBe "value"
            }
        }
        pivoted["Bob"]["weight"]["value"] shouldBe 87
    }

    @Test
    fun `pivot two values without index group by value`() {
        val pivoted = typed.pivot { name }.values(separate = true) { key and value }
        pivoted.df().columnNames() shouldBe listOf("key", "value")
        (pivoted["key"]["Alice"] as Many<String>).size shouldBe 4
        pivoted.df()["value"]["Bob"].type() shouldBe getType<Many<Int>>()
        pivoted["value"]["Bob"] shouldBe manyOf(45, 87)
    }

    @Test
    fun `pivot in group aggregator`() {
        val pivoted = typed.groupBy { name }.aggregate {
            pivot { key }.with { value } into "key"
        }
        pivoted.ncol() shouldBe 2
        pivoted.print()
        pivoted.ungroup("key") shouldBe typed.pivot { key }.groupBy { name }.with { value }
    }

    @Test
    fun `equal pivots`() {
        val expected = typed.pivot { key }.groupBy { name }.with { value }
        typed.groupBy { name }.pivot { key }.values { value } shouldBe expected
        val pivoted = typed.groupBy { name }.aggregate {
            pivot { key }.with { value }
        }
        pivoted.print()
        pivoted shouldBe expected
    }

    @Test
    fun gather() {
        val res = typed.pivot { key }.groupBy { name }.with { value }
        val gathered = res.gather { cols().drop(1) }.into("key", "value")
        gathered shouldBe typed.dropNulls { value }.sortBy { name and "key" }
    }

    @Test
    fun `gather with filter`() {
        val pivoted = typed.pivot { key }.groupBy { name }.with { value }
        val gathered = pivoted.gather { cols().drop(1) }.where { it is Int }.into("key", "value")
        gathered shouldBe typed.filter { value is Int }.sortBy("name", "key").convert("value").toInt() // TODO: replace convert with cast
    }

    @Test
    fun `grouped pivot with key and value conversions`() {
        val grouped = typed.groupBy { name }

        val pivoted = grouped.pivot { key.map(keyConverter) }.with { valueConverter(value) }

        val pivoted2 = grouped.aggregate {
            pivot { key.map(keyConverter) }.with { valueConverter(value) }
        }

        val pivoted3 = typed.pivot { key.map(keyConverter) }.groupBy { name }.values { value.map(valueConverter) }

        pivoted2 shouldBe pivoted
        pivoted3 shouldBe pivoted

        val gathered = pivoted.gather { cols().drop(1) }.into("key", "value")
        val expected =
            expectedFiltered.update { key }.with { keyConverter(it) }
                .convert { value }.with { valueConverter(it) as? Serializable }
        gathered shouldBe expected
    }

    @Test
    fun `gather with value conversion`() {
        val pivoted = typed.pivot { key }.groupBy { name }.with { valueConverter(value) }
        val gathered = pivoted.gather { cols().drop(1) }.mapValues { (it as? Double)?.toInt() ?: it }.into("key", "value")
        gathered shouldBe expectedFiltered
    }

    @Test
    fun `gather doubles with value conversion`() {
        val pivoted = typed.pivot { key }.groupBy { name }.with { valueConverter(value) }
        val gathered = pivoted.remove("city").gather { doubleCols() }.notNull().mapValues { it.toInt() }.into("key", "value")
        val expected = typed.filter { key != "city" && value != null }.convert { value }.to<Int>().sortBy { name and key }
        gathered shouldBe expected
    }

    @Test
    fun `gather with name conversion`() {
        val pivoted = typed.pivot { key.map(keyConverter) }.groupBy { name }.with { value }
        val gathered = pivoted.gather { cols().drop(1) }.mapKeys { it.substring(2) }.into("key", "value")
        gathered shouldBe expectedFiltered
    }

    @Test
    fun `type arguments inference in pivot with index`() {
        val id by columnOf(1, 1, 2, 2)
        val name by columnOf("set", "list", "set", "list")
        val data by columnOf(setOf(1), listOf(1), setOf(2), listOf(2))
        val df = dataFrameOf(id, name, data)
        df[data].type() shouldBe getType<AbstractCollection<Int>>()
        val pivoted = df.pivot { name }.groupBy { id }.values { data }
        pivoted.nrow() shouldBe 2
        pivoted.ncol() shouldBe 3
        pivoted["set"].type() shouldBe getType<AbstractSet<Int>>()
        pivoted["list"].type() shouldBe getType<AbstractList<Int>>()
    }

    @Test
    fun `type arguments inference in pivot`() {
        val name by columnOf("set", "list")
        val data by columnOf(setOf(1), listOf(1))
        val df = dataFrameOf(name, data)
        df[data].type() shouldBe getType<AbstractCollection<Int>>()
        val pivoted = df.pivot { name }.values { data }
        pivoted.ncol() shouldBe 2
        pivoted.df()["set"].type() shouldBe getType<AbstractSet<Int>>()
        pivoted.df()["list"].type() shouldBe getType<AbstractList<Int>>()
    }

    @Test
    fun `pivot with grouping`() {
        val pivoted = typed.pivot(inward = true) { key }.groupBy { name }.with { value }
        pivoted.columnNames() shouldBe listOf("name", "key")
        pivoted["key"].asColumnGroup().columnNames() shouldBe typed.key.distinct().values()
    }

    @Test
    fun `pivot matches yes no`() {
        val pivoted = typed.drop(1).pivot { key }.groupBy { name }.matches("yes", "no")
        pivoted.sumOf { values.count { it == "yes" } } shouldBe typed.nrow() - 1
        pivoted.sumOf { values.count { it == "no" } } shouldBe 1
    }

    @Test
    fun `pivot aggregate into`() {
        val pivoted = typed.pivot { key }.groupBy { name }.aggregate {
            value.first() into "value"
        }
        pivoted.columns().drop(1).forEach {
            it.kind() shouldBe ColumnKind.Group
            it.asColumnGroup().columnNames() shouldBe listOf("value")
        }
    }

    @Test
    fun `pivot aggregate several into`() {
        val pivoted = typed.pivot { key }.groupBy { name }.aggregate {
            value.first() into "first value"
            value.last() into "last value"
            "unused"
        }
        pivoted.columns().drop(1).forEach {
            it.kind() shouldBe ColumnKind.Group
            it.asColumnGroup().columnNames() shouldBe listOf("first value", "last value")
        }
    }

    @Test
    fun `pivot two value columns into one name`() {
        val type by typed.newColumn { value?.javaClass?.kotlin ?: Unit::class }
        val pivoted = (typed + type).pivot { key }.groupBy { name }.values { value and (type default Any::class) into "data" }
        pivoted.print()
        pivoted.columns().drop(1).forEach {
            val group = it.asColumnGroup()
            group.columnNames() shouldBe listOf("data")
            group["data"].asColumnGroup().columnNames() shouldBe listOf("value", "type")
            group["data"]["type"].hasNulls() shouldBe false
        }
        pivoted.print()
    }

    @Test
    fun `pivot one value without index`() {
        val pivoted = typed.pivot { name then key }.with { value }
        pivoted.columnNames() shouldBe typed.name.distinct().values()
        pivoted.df()["Alice"].asColumnGroup().columnNames() shouldBe typed.key.distinct().values()
        pivoted.df()["Bob"].asColumnGroup().columnNames() shouldBe listOf("age", "weight")
        pivoted.df()["Mark"].asColumnGroup().columnNames() shouldBe typed.key.distinct().values()
        pivoted.df()["Alice"]["age"].type() shouldBe getType<Many<Int>>()
        pivoted.df()["Mark"]["weight"].type() shouldBe getType<Any?>()
    }

    @Test
    fun `pivot plain`() {
        val pivoted = typed.pivot { name }.toDataRow()
        pivoted.columnNames() shouldBe typed.name.distinct().toList()
        pivoted["Bob"] shouldBe typed.filter { name == "Bob" }
    }

    @Test
    fun `pivot columns inward`() {
        typed.pivot(inward = true) { name }.count().ncol() shouldBe 1
        typed.pivot { name }.count() shouldBe typed.pivot(inward = false) { name }.count()

        typed.pivot { name and key }.count().columnNames() shouldBe listOf("name", "key")
        typed.pivot(inward = false) { name and key }.count().ncol() shouldBe typed.name.ndistinct() + typed.key.ndistinct()
        typed.pivot(inward = true) { name and key }.count() shouldBe typed.pivot { name and key }.count()
    }

    @Test
    fun `pivot from group`() {
        val pivoted = typed.group { key and value }.into("info")
            .pivot(inward = true) { "info"["value"] }.groupByOther().count()
        pivoted.getColumnGroup("info").getColumnGroup("value").ncol() shouldBe typed.value.ndistinct
    }
}
