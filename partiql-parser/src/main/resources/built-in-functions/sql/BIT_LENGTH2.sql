CREATE FUNCTION "BIT_LENGTH"(
    S1 CHARACTER VARYING ( CML ) )
RETURNS NUMERIC ( P2, 0 )
SPECIFIC BIT_LENGTH2
RETURN BIT_LENGTH ( S1 ) ;