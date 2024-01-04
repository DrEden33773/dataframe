package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.AnyColumnReference
import org.jetbrains.kotlinx.dataframe.ColumnsSelector
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.DistinctColumnsSelectionDsl.Usage
import org.jetbrains.kotlinx.dataframe.api.DistinctColumnsSelectionDsl.Usage.ColumnSetName
import org.jetbrains.kotlinx.dataframe.columns.ColumnSet
import org.jetbrains.kotlinx.dataframe.columns.SingleColumn
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet
import org.jetbrains.kotlinx.dataframe.documentation.Indent
import org.jetbrains.kotlinx.dataframe.documentation.UsageTemplateColumnsSelectionDsl.UsageTemplate
import org.jetbrains.kotlinx.dataframe.exceptions.DuplicateColumnNamesException
import org.jetbrains.kotlinx.dataframe.impl.columns.DistinctColumnSet
import org.jetbrains.kotlinx.dataframe.indices
import kotlin.reflect.KProperty

// region DataFrame

public fun <T> DataFrame<T>.distinct(): DataFrame<T> = distinctBy { all() }

public fun <T, C> DataFrame<T>.distinct(columns: ColumnsSelector<T, C>): DataFrame<T> = select(columns).distinct()

public fun <T> DataFrame<T>.distinct(vararg columns: KProperty<*>): DataFrame<T> = distinct {
    val set = columns.toColumnSet()
    set
}

public fun <T> DataFrame<T>.distinct(vararg columns: String): DataFrame<T> = distinct { columns.toColumnSet() }

public fun <T> DataFrame<T>.distinct(vararg columns: AnyColumnReference): DataFrame<T> =
    distinct { columns.toColumnSet() }

public fun <T> DataFrame<T>.distinctBy(vararg columns: KProperty<*>): DataFrame<T> =
    distinctBy { columns.toColumnSet() }

public fun <T> DataFrame<T>.distinctBy(vararg columns: String): DataFrame<T> = distinctBy { columns.toColumnSet() }

public fun <T> DataFrame<T>.distinctBy(vararg columns: AnyColumnReference): DataFrame<T> =
    distinctBy { columns.toColumnSet() }

public fun <T, C> DataFrame<T>.distinctBy(columns: ColumnsSelector<T, C>): DataFrame<T> {
    val cols = get(columns)
    val distinctIndices = indices.distinctBy { i -> cols.map { it[i] } }
    return this[distinctIndices]
}

// endregion

// region ColumnsSelectionDsl

/**
 * See [Usage]
 */
public interface DistinctColumnsSelectionDsl {

    /**
     * ## Distinct Usage
     *
     * @include [UsageTemplate]
     * {@setArg [UsageTemplate.DefinitionsArg]
     *  {@include [UsageTemplate.ColumnSetDef]}
     * }
     *
     * {@setArg [UsageTemplate.ColumnSetFunctionsArg]
     *  {@include [Indent]}{@include [ColumnSetName]}**`()`**
     * }
     *
     * {@setArg [UsageTemplate.PlainDslPart]}
     * {@setArg [UsageTemplate.ColumnGroupPart]}
     */
    public interface Usage {

        /** .[**distinct**][ColumnsSelectionDsl.distinct] */
        public interface ColumnSetName
    }

    /**
     * ## Distinct
     * Returns a new [ColumnSet] from [this] containing only distinct columns (by path).
     * This is useful when you've selected the same column multiple times.
     *
     * NOTE: This doesn't solve [DuplicateColumnNamesException] if you've selected two columns with the same name.
     * For this, you'll need to [rename][ColumnsSelectionDsl.named] one of the columns.
     *
     * ### Check out: [Usage]
     *
     * #### For Example:
     * `df.`[select][DataFrame.select]` { (`[colsOf][SingleColumn.colsOf]`<`[Int][Int]`>() `[and][ColumnsSelectionDsl.and]` age).`[distinct][ColumnSet.distinct]`() }`
     *
     * `df.`[select][DataFrame.select]` { `[colsAtAnyDepth][ColumnsSelectionDsl.colsAtAnyDepth]`().`[nameStartsWith][ColumnsSelectionDsl.nameStartsWith]`("order").`[distinct][ColumnSet.distinct]`() }`
     *
     * @return A new [ColumnSet] containing only distinct columns (by path).
     */
    public fun <C> ColumnSet<C>.distinct(): ColumnSet<C> = DistinctColumnSet(this)
}

// endregion
