-- [trim] ---------

CREATE FUNCTION "TRIM"(
    VALUE STRING)
    RETURNS STRING
    SPECIFIC -
  RETURN TRIM ( VALUE );

CREATE FUNCTION "TRIM"(
    VALUE NULLABLE_STRING)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN TRIM ( VALUE );

CREATE FUNCTION "TRIM"(
    VALUE SYMBOL)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN TRIM ( VALUE );

CREATE FUNCTION "TRIM"(
    VALUE NULLABLE_SYMBOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN TRIM ( VALUE );


-- [trim_leading] ---------

CREATE FUNCTION "TRIM_LEADING"(
    VALUE STRING)
    RETURNS STRING
    SPECIFIC -
  RETURN TRIM_LEADING ( VALUE );

CREATE FUNCTION "TRIM_LEADING"(
    VALUE NULLABLE_STRING)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN TRIM_LEADING ( VALUE );

CREATE FUNCTION "TRIM_LEADING"(
    VALUE SYMBOL)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN TRIM_LEADING ( VALUE );

CREATE FUNCTION "TRIM_LEADING"(
    VALUE NULLABLE_SYMBOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN TRIM_LEADING ( VALUE );


-- [trim_trailing] ---------

CREATE FUNCTION "TRIM_TRAILING"(
    VALUE STRING)
    RETURNS STRING
    SPECIFIC -
  RETURN TRIM_TRAILING ( VALUE );

CREATE FUNCTION "TRIM_TRAILING"(
    VALUE NULLABLE_STRING)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN TRIM_TRAILING ( VALUE );

CREATE FUNCTION "TRIM_TRAILING"(
    VALUE SYMBOL)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN TRIM_TRAILING ( VALUE );

CREATE FUNCTION "TRIM_TRAILING"(
    VALUE NULLABLE_SYMBOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN TRIM_TRAILING ( VALUE );


-- [null_if] ---------

CREATE FUNCTION "NULL_IF"(
    VALUE NULL
        NULLIFIER BOOL)
    RETURNS NULL
  SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE MISSING
        NULLIFIER BOOL)
    RETURNS MISSING
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_BOOL
        NULLIFIER BOOL)
    RETURNS NULLABLE_BOOL
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_INT8
        NULLIFIER BOOL)
    RETURNS NULLABLE_INT8
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_INT16
        NULLIFIER BOOL)
    RETURNS NULLABLE_INT16
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_INT32
        NULLIFIER BOOL)
    RETURNS NULLABLE_INT32
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_INT64
        NULLIFIER BOOL)
    RETURNS NULLABLE_INT64
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_INT
        NULLIFIER BOOL)
    RETURNS NULLABLE_INT
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_DECIMAL
        NULLIFIER BOOL)
    RETURNS NULLABLE_DECIMAL
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_FLOAT32
        NULLIFIER BOOL)
    RETURNS NULLABLE_FLOAT32
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_FLOAT64
        NULLIFIER BOOL)
    RETURNS NULLABLE_FLOAT64
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_CHAR
        NULLIFIER BOOL)
    RETURNS NULLABLE_CHAR
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_STRING
        NULLIFIER BOOL)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_SYMBOL
        NULLIFIER BOOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_CLOB
        NULLIFIER BOOL)
    RETURNS NULLABLE_CLOB
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_BINARY
        NULLIFIER BOOL)
    RETURNS NULLABLE_BINARY
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_BYTE
        NULLIFIER BOOL)
    RETURNS NULLABLE_BYTE
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_BLOB
        NULLIFIER BOOL)
    RETURNS NULLABLE_BLOB
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_DATE
        NULLIFIER BOOL)
    RETURNS NULLABLE_DATE
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_TIME
        NULLIFIER BOOL)
    RETURNS NULLABLE_TIME
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_TIMESTAMP
        NULLIFIER BOOL)
    RETURNS NULLABLE_TIMESTAMP
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_INTERVAL
        NULLIFIER BOOL)
    RETURNS NULLABLE_INTERVAL
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_LIST
        NULLIFIER BOOL)
    RETURNS NULLABLE_LIST
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_BAG
        NULLIFIER BOOL)
    RETURNS NULLABLE_BAG
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_SEXP
        NULLIFIER BOOL)
    RETURNS NULLABLE_SEXP
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );

CREATE FUNCTION "NULL_IF"(
    VALUE NULLABLE_STRUCT
        NULLIFIER BOOL)
    RETURNS NULLABLE_STRUCT
    SPECIFIC -
  RETURN NULL_IF ( VALUE, NULLIFIER );


-- [in_collection] ---------

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE MISSING
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE MISSING
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE MISSING
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BOOL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BOOL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BOOL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BOOL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BOOL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BOOL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT8
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT8
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT8
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT8
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT8
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT8
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT16
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT16
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT16
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT16
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT16
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT16
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT32
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT32
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT32
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT32
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT32
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT32
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT64
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT64
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT64
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT64
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT64
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT64
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INT
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INT
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE DECIMAL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE DECIMAL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE DECIMAL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_DECIMAL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_DECIMAL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_DECIMAL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE FLOAT32
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE FLOAT32
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE FLOAT32
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_FLOAT32
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_FLOAT32
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_FLOAT32
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE FLOAT64
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE FLOAT64
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE FLOAT64
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_FLOAT64
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_FLOAT64
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_FLOAT64
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE CHAR
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE CHAR
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE CHAR
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_CHAR
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_CHAR
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_CHAR
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE STRING
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE STRING
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE STRING
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_STRING
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_STRING
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_STRING
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE SYMBOL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE SYMBOL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE SYMBOL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_SYMBOL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_SYMBOL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_SYMBOL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE CLOB
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE CLOB
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE CLOB
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_CLOB
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_CLOB
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_CLOB
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BINARY
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BINARY
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BINARY
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BINARY
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BINARY
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BINARY
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BYTE
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BYTE
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BYTE
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BYTE
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BYTE
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BYTE
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BLOB
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BLOB
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BLOB
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BLOB
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BLOB
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BLOB
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE DATE
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE DATE
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE DATE
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_DATE
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_DATE
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_DATE
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE TIME
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE TIME
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE TIME
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_TIME
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_TIME
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_TIME
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE TIMESTAMP
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE TIMESTAMP
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE TIMESTAMP
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_TIMESTAMP
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_TIMESTAMP
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_TIMESTAMP
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INTERVAL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INTERVAL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE INTERVAL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INTERVAL
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INTERVAL
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_INTERVAL
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE LIST
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE LIST
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE LIST
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_LIST
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_LIST
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_LIST
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BAG
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BAG
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE BAG
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BAG
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BAG
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_BAG
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE SEXP
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE SEXP
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE SEXP
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_SEXP
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_SEXP
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_SEXP
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE STRUCT
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE STRUCT
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE STRUCT
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_STRUCT
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_STRUCT
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE NULLABLE_STRUCT
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE GRAPH
        COLLECTION LIST)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE GRAPH
        COLLECTION BAG)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );

CREATE FUNCTION "IN_COLLECTION"(
    VALUE GRAPH
        COLLECTION SEXP)
    RETURNS BOOL
    SPECIFIC -
  RETURN IN_COLLECTION ( VALUE, COLLECTION );


-- [substring] ---------

CREATE FUNCTION "SUBSTRING"(
    VALUE STRING
        START INT64)
    RETURNS STRING
    SPECIFIC -
  RETURN SUBSTRING ( VALUE, START );

CREATE FUNCTION "SUBSTRING"(
    VALUE NULLABLE_STRING
        START INT64)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN SUBSTRING ( VALUE, START );

CREATE FUNCTION "SUBSTRING"(
    VALUE SYMBOL
        START INT64)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN SUBSTRING ( VALUE, START );

CREATE FUNCTION "SUBSTRING"(
    VALUE NULLABLE_SYMBOL
        START INT64)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN SUBSTRING ( VALUE, START );


-- [like] ---------

CREATE FUNCTION "LIKE"(
    VALUE STRING
        PATTERN STRING)
    RETURNS BOOL
    SPECIFIC -
  RETURN LIKE ( VALUE, PATTERN );


-- [position] ---------

CREATE FUNCTION "POSITION"(
    PROBE STRING
        VALUE STRING)
    RETURNS INT64
    SPECIFIC -
  RETURN POSITION ( PROBE, VALUE );

CREATE FUNCTION "POSITION"(
    PROBE NULLABLE_STRING
        VALUE NULLABLE_STRING)
    RETURNS INT64
    SPECIFIC -
  RETURN POSITION ( PROBE, VALUE );

CREATE FUNCTION "POSITION"(
    PROBE SYMBOL
        VALUE SYMBOL)
    RETURNS INT64
    SPECIFIC -
  RETURN POSITION ( PROBE, VALUE );

CREATE FUNCTION "POSITION"(
    PROBE NULLABLE_SYMBOL
        VALUE NULLABLE_SYMBOL)
    RETURNS INT64
    SPECIFIC -
  RETURN POSITION ( PROBE, VALUE );


-- [trim_chars] ---------

CREATE FUNCTION "TRIM_CHARS"(
    VALUE STRING
        CHARS STRING)
    RETURNS STRING
    SPECIFIC -
  RETURN TRIM_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_CHARS"(
    VALUE NULLABLE_STRING
        CHARS NULLABLE_STRING)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN TRIM_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_CHARS"(
    VALUE SYMBOL
        CHARS SYMBOL)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN TRIM_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_CHARS"(
    VALUE NULLABLE_SYMBOL
        CHARS NULLABLE_SYMBOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN TRIM_CHARS ( VALUE, CHARS );


-- [trim_leading_chars] ---------

CREATE FUNCTION "TRIM_LEADING_CHARS"(
    VALUE STRING
        CHARS STRING)
    RETURNS STRING
    SPECIFIC -
  RETURN TRIM_LEADING_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_LEADING_CHARS"(
    VALUE NULLABLE_STRING
        CHARS NULLABLE_STRING)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN TRIM_LEADING_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_LEADING_CHARS"(
    VALUE SYMBOL
        CHARS SYMBOL)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN TRIM_LEADING_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_LEADING_CHARS"(
    VALUE NULLABLE_SYMBOL
        CHARS NULLABLE_SYMBOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN TRIM_LEADING_CHARS ( VALUE, CHARS );


-- [trim_trailing_chars] ---------

CREATE FUNCTION "TRIM_TRAILING_CHARS"(
    VALUE STRING
        CHARS STRING)
    RETURNS STRING
    SPECIFIC -
  RETURN TRIM_TRAILING_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_TRAILING_CHARS"(
    VALUE NULLABLE_STRING
        CHARS NULLABLE_STRING)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN TRIM_TRAILING_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_TRAILING_CHARS"(
    VALUE SYMBOL
        CHARS SYMBOL)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN TRIM_TRAILING_CHARS ( VALUE, CHARS );

CREATE FUNCTION "TRIM_TRAILING_CHARS"(
    VALUE NULLABLE_SYMBOL
        CHARS NULLABLE_SYMBOL)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN TRIM_TRAILING_CHARS ( VALUE, CHARS );


-- [between] ---------

CREATE FUNCTION "BETWEEN"(
    VALUE INT8
        LOWER INT8
        UPPER INT8)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_INT8
        LOWER NULLABLE_INT8
        UPPER NULLABLE_INT8)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE INT16
        LOWER INT16
        UPPER INT16)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_INT16
        LOWER NULLABLE_INT16
        UPPER NULLABLE_INT16)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE INT32
        LOWER INT32
        UPPER INT32)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_INT32
        LOWER NULLABLE_INT32
        UPPER NULLABLE_INT32)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE INT64
        LOWER INT64
        UPPER INT64)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_INT64
        LOWER NULLABLE_INT64
        UPPER NULLABLE_INT64)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE INT
        LOWER INT
        UPPER INT)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_INT
        LOWER NULLABLE_INT
        UPPER NULLABLE_INT)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE DECIMAL
        LOWER DECIMAL
        UPPER DECIMAL)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_DECIMAL
        LOWER NULLABLE_DECIMAL
        UPPER NULLABLE_DECIMAL)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE FLOAT32
        LOWER FLOAT32
        UPPER FLOAT32)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_FLOAT32
        LOWER NULLABLE_FLOAT32
        UPPER NULLABLE_FLOAT32)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE FLOAT64
        LOWER FLOAT64
        UPPER FLOAT64)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );

CREATE FUNCTION "BETWEEN"(
    VALUE NULLABLE_FLOAT64
        LOWER NULLABLE_FLOAT64
        UPPER NULLABLE_FLOAT64)
    RETURNS BOOL
    SPECIFIC -
  RETURN BETWEEN ( VALUE, LOWER, UPPER );


-- [substring_length] ---------

CREATE FUNCTION "SUBSTRING_LENGTH"(
    VALUE STRING
        START INT64
        END INT64)
    RETURNS STRING
    SPECIFIC -
  RETURN SUBSTRING_LENGTH ( VALUE, START, END );

CREATE FUNCTION "SUBSTRING_LENGTH"(
    VALUE NULLABLE_STRING
        START INT64
        END INT64)
    RETURNS NULLABLE_STRING
    SPECIFIC -
  RETURN SUBSTRING_LENGTH ( VALUE, START, END );

CREATE FUNCTION "SUBSTRING_LENGTH"(
    VALUE SYMBOL
        START INT64
        END INT64)
    RETURNS SYMBOL
    SPECIFIC -
  RETURN SUBSTRING_LENGTH ( VALUE, START, END );

CREATE FUNCTION "SUBSTRING_LENGTH"(
    VALUE NULLABLE_SYMBOL
        START INT64
        END INT64)
    RETURNS NULLABLE_SYMBOL
    SPECIFIC -
  RETURN SUBSTRING_LENGTH ( VALUE, START, END );


-- [like_escape] ---------

CREATE FUNCTION "LIKE_ESCAPE"(
    VALUE STRING
        PATTERN STRING
        ESCAPE STRING)
    RETURNS BOOL
    SPECIFIC -
  RETURN LIKE_ESCAPE ( VALUE, PATTERN, ESCAPE );
