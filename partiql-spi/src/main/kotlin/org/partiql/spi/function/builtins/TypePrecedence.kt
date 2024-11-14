package org.partiql.spi.function.builtins

import org.partiql.types.PType.Kind

/**
 * @return the precedence of the types for the PartiQL comparator.
 * @see .TYPE_PRECEDENCE
 */
@Suppress("deprecation")
internal val TYPE_PRECEDENCE: Map<Kind, Int> = listOf(
    Kind.UNKNOWN,
    Kind.BOOL,
    Kind.TINYINT,
    Kind.SMALLINT,
    Kind.INTEGER,
    Kind.BIGINT,
    Kind.NUMERIC,
    Kind.DECIMAL,
    Kind.REAL,
    Kind.DOUBLE,
    Kind.DECIMAL_ARBITRARY, // Arbitrary precision decimal has a higher precedence than FLOAT
    Kind.CHAR,
    Kind.VARCHAR,
    Kind.SYMBOL,
    Kind.STRING,
    Kind.CLOB,
    Kind.BLOB,
    Kind.DATE,
    Kind.TIME,
    Kind.TIMEZ,
    Kind.TIMESTAMP,
    Kind.TIMESTAMPZ,
    Kind.ARRAY,
    Kind.SEXP,
    Kind.BAG,
    Kind.ROW,
    Kind.STRUCT,
    Kind.DYNAMIC
).mapIndexed { precedence, type -> type to precedence }.toMap()
