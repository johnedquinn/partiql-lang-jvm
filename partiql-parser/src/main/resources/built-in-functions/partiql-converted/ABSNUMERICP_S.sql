CREATE FUNCTION "ABS"(
    N NUMERIC )
RETURNS NUMERIC
SPECIFIC ABSNUMERICP_S
RETURN ABS ( N ) ;