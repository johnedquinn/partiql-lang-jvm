CREATE FUNCTION "MOD"(
    N1 SMALLINT,
    N2 DECIMAL ( P, 0 ) )
RETURNS DECIMAL ( P, 0 )
SPECIFIC MODSMALLINT_DECIMALP
RETURN MOD ( N1, N2 ) ;