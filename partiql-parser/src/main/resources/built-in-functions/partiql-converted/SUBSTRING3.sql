CREATE FUNCTION "SUBSTRING"(
    S CHARACTER,
START NUMERIC )
RETURNS CHARACTER VARYING
SPECIFIC SUBSTRING3
RETURN SUBSTRING ( S FROM START ) ;